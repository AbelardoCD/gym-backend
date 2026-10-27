package com.gym.auth;

import com.gym.security.AuthUser;
import com.gym.security.JwtService;
import com.gym.user.User;
import com.gym.user.UserDtos.UserResponse;
import com.gym.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetService passwordReset;

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(String token, UserResponse user) {}

    public record ForgotRequest(@NotBlank @Email String email) {}

    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 6, max = 100) String password) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email())
                .filter(User::isActive)
                .filter(x -> passwordEncoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        String token = jwtService.generate(AuthUser.from(u));
        return new LoginResponse(token, UserResponse.from(u));
    }

    /** Manda el enlace para restablecer la contraseña. Responde igual aunque el correo no exista. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotRequest req) {
        passwordReset.request(req.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest req) {
        passwordReset.reset(req.token(), req.password());
    }
}
