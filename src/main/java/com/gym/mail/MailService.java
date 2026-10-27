package com.gym.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Envía los correos del sistema con la plantilla de {@code resources/mail}. Con {@code app.mail.enabled=false}
 * no envía nada: escribe el texto en el log, que basta para probar en desarrollo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean enabled;
    @Value("${app.mail.from}")
    private String from;

    public record Attachment(String filename, byte[] data, String contentType) {}

    /** Devuelve true si el correo salió (o se anotó en el log con el correo deshabilitado). */
    public boolean send(String to, String subject, String template, Map<String, String> vars, String plainText) {
        return send(to, subject, template, vars, plainText, List.of());
    }

    public boolean send(String to, String subject, String template, Map<String, String> vars, String plainText,
                        List<Attachment> attachments) {
        if (!enabled) {
            log.info("Correo deshabilitado (MAIL_ENABLED=false). Para {} «{}»{}:\n{}", to, subject,
                    attachments.isEmpty() ? "" : " con " + attachments.size() + " adjunto(s)", plainText);
            return true;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText, render(subject, template, vars));
            for (Attachment a : attachments) {
                helper.addAttachment(a.filename(), new ByteArrayResource(a.data()), a.contentType());
            }
            mailSender.send(message);
            log.info("Correo «{}» enviado a {}", subject, to);
            return true;
        } catch (MessagingException | MailException e) {
            log.warn("No se pudo enviar «{}» a {}: {}", subject, to, e.getMessage());
            return false;
        }
    }

    String render(String subject, String template, Map<String, String> vars) {
        String body = read("mail/" + template + ".html");
        for (Map.Entry<String, String> v : vars.entrySet()) {
            body = body.replace("{{" + v.getKey() + "}}", HtmlUtils.htmlEscape(v.getValue()));
        }
        return read("mail/_layout.html")
                .replace("{{asunto}}", HtmlUtils.htmlEscape(subject))
                .replace("{{contenido}}", body);
    }

    public static String firstName(String fullName) {
        return fullName.trim().split("\\s+")[0];
    }

    private static String read(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
