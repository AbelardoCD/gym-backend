package com.gym.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Pattern FILE_PATH = Pattern.compile("^/api/(routines/\\d+/files/\\d+|profiles/\\d+/photo)$");

    private final AuthUserLoader authUserLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            authUserLoader.fromToken(token).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(JwtService.toAuthentication(user)));
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // Las etiquetas <video> e <img> no pueden mandar cabeceras, así que para descargar
        // archivos de rutinas y fotos de perfil también se acepta el token como parámetro.
        if ("GET".equals(request.getMethod()) && FILE_PATH.matcher(request.getRequestURI()).matches()) {
            return request.getParameter("token");
        }
        return null;
    }
}
