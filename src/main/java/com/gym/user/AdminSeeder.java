package com.gym.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el primer administrador si la base de datos no tiene ninguno. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRoles_Name(Role.ADMINISTRADOR)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Administrador");
        admin.getRoles().add(roles.findByName(Role.ADMINISTRADOR).orElseThrow());
        users.save(admin);
        log.warn("Administrador inicial creado: {} (cambia la contraseña)", email);
    }
}
