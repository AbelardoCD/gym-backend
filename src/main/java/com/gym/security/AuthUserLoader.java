package com.gym.security;

import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserLoader {
    private final JwtService jwtService;
    private final UserRepository users;

    /** Valida el token y carga al usuario activo con sus roles y permisos actuales. */
    @Transactional(readOnly = true)
    public Optional<AuthUser> fromToken(String token) {
        return jwtService.parseUserId(token)
                .flatMap(users::findById)
                .filter(User::isActive)
                .map(AuthUser::from);
    }
}
