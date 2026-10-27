package com.gym.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Correo de bienvenida para usuarios nuevos. Se envía en segundo plano y después del commit,
 * así un fallo del servidor de correo nunca impide crear el usuario.
 */
@Service
@RequiredArgsConstructor
public class WelcomeMailService {
    private final MailService mail;

    @Value("${app.mobile-app-url}")
    private String mobileAppUrl;
    @Value("${app.web-url}")
    private String webUrl;

    @Async(MailConfig.EXECUTOR)
    @TransactionalEventListener
    public void onUserCreated(UserCreatedEvent event) {
        String name = MailService.firstName(event.fullName());
        String text = """
                Hola %s, te damos la bienvenida al gimnasio.

                Tu cuenta ya está lista. Entra con tu correo: %s
                Descarga la app móvil: %s
                O entra desde la web: %s

                Si no conoces tu contraseña, pídela en recepción.
                """.formatted(name, event.email(), mobileAppUrl, webUrl);
        mail.send(event.email(), "Bienvenido al gimnasio", "bienvenida",
                Map.of("nombre", name, "correo", event.email(), "appUrl", mobileAppUrl, "webUrl", webUrl), text);
    }
}
