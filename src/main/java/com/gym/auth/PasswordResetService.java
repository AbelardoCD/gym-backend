package com.gym.auth;

import com.gym.common.ApiException;
import com.gym.mail.MailConfig;
import com.gym.mail.MailService;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Recuperación de contraseña: se manda un enlace de un solo uso que vence en {@link #VALID_FOR}.
 * La respuesta es la misma exista o no el correo, para no revelar qué cuentas existen.
 */
@Slf4j
@Service
@Transactional
public class PasswordResetService {
    static final Duration VALID_FOR = Duration.ofMinutes(60);
    /** Evita que se pidan enlaces en ráfaga para la misma cuenta. */
    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;
    private final TaskExecutor mailExecutor;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.web-url}")
    private String webUrl;

    public PasswordResetService(PasswordResetTokenRepository tokens, UserRepository users, PasswordEncoder passwordEncoder,
                                MailService mail, @Qualifier(MailConfig.EXECUTOR) TaskExecutor mailExecutor) {
        this.tokens = tokens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.mailExecutor = mailExecutor;
    }

    public void request(String email) {
        User u = users.findByEmailIgnoreCase(email.trim()).filter(User::isActive).orElse(null);
        Instant now = Instant.now();
        if (u == null || tokens.existsByUserIdAndCreatedAtAfter(u.getId(), now.minus(COOLDOWN))) {
            return;
        }
        tokens.invalidateAll(u.getId(), now);

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setTokenHash(hash(token));
        t.setExpiresAt(now.plus(VALID_FOR));
        tokens.save(t);

        String link = webUrl + "/restablecer?token=" + token;
        String name = MailService.firstName(u.getFullName());
        String minutes = String.valueOf(VALID_FOR.toMinutes());
        String text = """
                Hola %s, recibimos una solicitud para cambiar tu contraseña.
                Abre este enlace (vale por %s minutos): %s

                Si no fuiste tú, ignora este correo.
                """.formatted(name, minutes, link);
        String to = u.getEmail();
        mailExecutor.execute(() -> mail.send(to, "Restablece tu contraseña", "restablecer",
                Map.of("nombre", name, "enlace", link, "minutos", minutes), text));
    }

    public void reset(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(hash(token))
                .filter(x -> x.getUsedAt() == null && x.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("El enlace no es válido o ya venció. Pide uno nuevo."));
        User u = t.getUser();
        if (!u.isActive()) {
            throw ApiException.badRequest("La cuenta está inactiva");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        t.setUsedAt(Instant.now());
        log.info("Contraseña restablecida para {}", u.getEmail());
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
