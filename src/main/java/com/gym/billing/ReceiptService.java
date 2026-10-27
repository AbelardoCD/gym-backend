package com.gym.billing;

import com.gym.common.ApiException;
import com.gym.mail.MailConfig;
import com.gym.mail.MailService;
import com.gym.security.AuthUser;
import com.gym.user.Permission;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Recibos de pago: el PDF para descargar y el correo al socio con el PDF adjunto. */
@Service
@Transactional(readOnly = true)
public class ReceiptService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"));

    private final PaymentRepository payments;
    private final MailService mail;
    private final TaskExecutor mailExecutor;
    private final ReceiptPdf.GymInfo gym;

    public ReceiptService(PaymentRepository payments, MailService mail, @Qualifier(MailConfig.EXECUTOR) TaskExecutor mailExecutor,
                          @Value("${app.gym.name}") String name, @Value("${app.gym.address}") String address,
                          @Value("${app.gym.phone}") String phone) {
        this.payments = payments;
        this.mail = mail;
        this.mailExecutor = mailExecutor;
        this.gym = new ReceiptPdf.GymInfo(name, address, phone);
    }

    public record Receipt(String filename, byte[] pdf) {}

    /** Quien cobra puede bajar cualquier recibo; un socio, solo los suyos. */
    public Receipt pdf(Long paymentId, AuthUser me) {
        Payment p = find(paymentId);
        if (!me.can(Permission.PAGOS) && !p.getUser().getId().equals(me.id())) {
            throw ApiException.forbidden("No puedes ver este recibo");
        }
        return new Receipt("recibo-" + ReceiptPdf.folio(p) + ".pdf", ReceiptPdf.render(p, gym));
    }

    /**
     * Envía el recibo al correo del socio. El PDF se arma aquí, dentro de la transacción, y el envío
     * sigue en segundo plano para no hacer esperar a recepción. Devuelve el correo de destino.
     */
    public String email(Long paymentId) {
        return email(find(paymentId));
    }

    String email(Payment p) {
        byte[] pdf = ReceiptPdf.render(p, gym);
        String folio = ReceiptPdf.folio(p);
        String to = p.getUser().getEmail();
        String name = MailService.firstName(p.getUser().getFullName());
        String amount = ReceiptPdf.money(p.getAmount());
        String period = DAY.format(p.getCoversFrom()) + " al " + DAY.format(p.getCoversTo());
        Map<String, String> vars = Map.of("nombre", name, "folio", folio, "monto", amount, "membresia", p.getPlanName(),
                "periodo", period);
        String text = "Hola " + name + ", recibimos tu pago de " + amount + " por " + p.getPlanName()
                + ". Cubre del " + period + ". Adjuntamos tu recibo " + folio + ".";
        List<MailService.Attachment> files = List.of(new MailService.Attachment("recibo-" + folio + ".pdf", pdf, "application/pdf"));
        mailExecutor.execute(() -> mail.send(to, "Recibo de pago " + folio, "recibo", vars, text, files));
        return to;
    }

    private Payment find(Long id) {
        return payments.findById(id).orElseThrow(() -> ApiException.notFound("Pago no encontrado"));
    }
}
