package com.gym.billing;

import com.gym.billing.MembershipReminder.Kind;
import com.gym.mail.MailService;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/**
 * Manda por correo un aviso unos días antes de que venza la membresía y otro cuando ya venció.
 * Cada aviso se envía una sola vez por vencimiento; si el servidor estuvo apagado, se pone al día al volver.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipReminderJob {
    /** Después de este tiempo vencida ya no se avisa (se entiende que el socio se dio de baja). */
    private static final int MAX_DAYS_EXPIRED = 7;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-MX"));

    private final PaymentRepository payments;
    private final UserRepository users;
    private final MembershipReminderRepository sent;
    private final MailService mail;

    @Value("${app.reminders.enabled:true}")
    private boolean enabled;
    @Value("${app.reminders.days-before:3}")
    private int daysBefore;

    @Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}")
    public void scheduled() {
        if (enabled) {
            run();
        }
    }

    /** Revisa a todos los socios y manda los avisos pendientes. Devuelve cuántos correos salieron. */
    @Transactional
    public int run() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        int count = 0;
        for (Object[] row : payments.paidUntilAll()) {
            Long userId = (Long) row[0];
            LocalDate paidUntil = (LocalDate) row[1];
            long left = ChronoUnit.DAYS.between(today, paidUntil);
            Kind kind = left >= 0 && left <= daysBefore ? Kind.POR_VENCER
                    : left < 0 && left >= -MAX_DAYS_EXPIRED ? Kind.VENCIDA : null;
            if (kind == null || sent.existsById(new MembershipReminder.Key(userId, paidUntil, kind))) {
                continue;
            }
            User u = users.findById(userId).orElse(null);
            if (u == null || !u.isActive() || !u.hasRole(Role.SOCIO)) {
                continue;
            }
            if (send(u, paidUntil, left, kind)) {
                sent.save(new MembershipReminder(userId, paidUntil, kind));
                count++;
            }
        }
        if (count > 0) {
            log.info("Recordatorios de membresía enviados: {}", count);
        }
        return count;
    }

    private boolean send(User u, LocalDate paidUntil, long left, Kind kind) {
        String name = MailService.firstName(u.getFullName());
        String until = paidUntil.format(DAY);
        String title;
        String message;
        if (kind == Kind.POR_VENCER) {
            title = left == 0 ? "Tu membresía vence hoy" : "Tu membresía vence en " + left + (left == 1 ? " día" : " días");
            message = "te recordamos que tu membresía está pagada hasta el " + until + ".";
        } else {
            title = "Tu membresía venció";
            message = "tu membresía venció el " + until + ". Te esperamos de regreso.";
        }
        String text = "Hola %s, %s%nRenueva en recepción para seguir entrenando.".formatted(name, message);
        return mail.send(u.getEmail(), title, "recordatorio",
                Map.of("nombre", name, "titulo", title, "mensaje", message, "pagadoHasta", until), text);
    }
}
