package com.gym.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    /** El token solo identifica al usuario; roles y permisos se leen de la base en cada petición. */
    public String generate(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Optional<Long> parseUserId(String token) {
        try {
            return Optional.of(Long.valueOf(Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Autoridades: ROLE_<rol> por cada rol y el código de cada permiso (por ejemplo USUARIOS). */
    public static UsernamePasswordAuthenticationToken toAuthentication(AuthUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        user.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        user.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }
}
