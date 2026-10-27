package com.gym.user;

import com.gym.routine.Discipline;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public final class UserDtos {
    private UserDtos() {}

    /** permissions = pantallas que el usuario puede ver, la suma de las de todos sus roles. */
    public record UserResponse(Long id, String email, String fullName, String phone, Set<String> roles,
                               Set<String> permissions, Set<Discipline> disciplines, LocalDate enrolledOn, boolean active, Instant createdAt,
                               String photoVersion) {
        public static UserResponse from(User u) {
            Set<String> perms = new java.util.TreeSet<>();
            u.getRoles().forEach(r -> r.getPermissions().forEach(p -> perms.add(p.getCode())));
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.roleNames(), perms,
                    new java.util.TreeSet<>(u.getDisciplines()), u.getEnrolledOn(),
                    u.isActive(), u.getCreatedAt(), u.photoVersion());
        }
    }

    /** Vista reducida para buscar contactos en el chat. */
    public record UserSummary(Long id, String fullName, Set<String> roles) {
        public static UserSummary from(User u) {
            return new UserSummary(u.getId(), u.getFullName(), u.roleNames());
        }
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank String fullName,
            String phone,
            @NotEmpty Set<String> roles,
            Set<Discipline> disciplines,
            LocalDate enrolledOn) {}

    public record UpdateUserRequest(
            @NotBlank String fullName,
            String phone,
            @NotEmpty Set<String> roles,
            Set<Discipline> disciplines,
            LocalDate enrolledOn,
            boolean active,
            @Size(min = 6, max = 100) String password) {}
}
