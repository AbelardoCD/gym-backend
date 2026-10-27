package com.gym.user;

import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import com.gym.user.UserDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@CurrentUser AuthUser me) {
        return UserResponse.from(userService.get(me.id()));
    }

    public record QrResponse(String code) {}

    /** Código de la credencial del usuario conectado, para mostrarlo como QR en recepción. */
    @GetMapping("/me/qr")
    public QrResponse myQr(@CurrentUser AuthUser me) {
        return new QrResponse(userService.get(me.id()).qrCode());
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public QrResponse qr(@PathVariable Long id) {
        return new QrResponse(userService.get(id).qrCode());
    }

    /** Invalida la credencial anterior (por ejemplo, si el socio perdió o compartió su QR). */
    @PostMapping("/{id}/qr")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public QrResponse regenerateQr(@PathVariable Long id) {
        return new QrResponse(userService.regenerateQr(id).qrCode());
    }

    /** Búsqueda de usuarios activos, usada para enviar solicitudes de chat. */
    @GetMapping("/search")
    public List<UserSummary> search(@RequestParam(defaultValue = "") String q, @CurrentUser AuthUser me) {
        return userService.search(q).stream()
                .filter(u -> !u.getId().equals(me.id()))
                .map(UserSummary::from).toList();
    }

    /** Listado completo: para el catálogo de usuarios, asignar rutinas, registrar asistencia o cobrar. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USUARIOS','RUTINAS','ASISTENCIA_GESTION','PAGOS')")
    public List<UserResponse> list(@RequestParam(required = false) String role) {
        return userService.list(role).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USUARIOS')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return UserResponse.from(userService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return UserResponse.from(userService.update(id, req));
    }
}
