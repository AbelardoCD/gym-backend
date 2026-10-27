package com.gym.user;

import com.gym.common.ApiException;
import com.gym.mail.UserCreatedEvent;
import com.gym.routine.Discipline;
import com.gym.user.UserDtos.CreateUserRequest;
import com.gym.user.UserDtos.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public List<User> list(String role) {
        return role == null ? users.findAllByOrderByFullName() : users.findDistinctByRoles_NameOrderByFullName(role);
    }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    @Transactional
    public User create(CreateUserRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        User u = new User();
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        u.setRoles(resolveRoles(req.roles()));
        u.setDisciplines(resolveDisciplines(u, req.disciplines()));
        if (req.enrolledOn() != null) {
            u.setEnrolledOn(req.enrolledOn());
        }
        User saved = users.save(u);
        events.publishEvent(new UserCreatedEvent(saved.getEmail(), saved.getFullName()));
        return saved;
    }

    @Transactional
    public User regenerateQr(Long id) {
        User u = get(id);
        u.setQrToken(User.newQrToken());
        return u;
    }

    @Transactional
    public User update(Long id, UpdateUserRequest req) {
        User u = get(id);
        boolean losesAdmin = u.hasRole(Role.ADMINISTRADOR) && (!req.roles().contains(Role.ADMINISTRADOR) || !req.active());
        if (losesAdmin && users.countActiveWithRole(Role.ADMINISTRADOR) <= 1) {
            throw ApiException.badRequest("Debe quedar al menos un administrador activo");
        }
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        u.setRoles(resolveRoles(req.roles()));
        u.setDisciplines(resolveDisciplines(u, req.disciplines()));
        if (req.enrolledOn() != null) {
            u.setEnrolledOn(req.enrolledOn());
        }
        u.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return u;
    }

    /** Un socio necesita al menos un subrol; quien no es socio no lleva ninguno. */
    private Set<Discipline> resolveDisciplines(User u, Set<Discipline> requested) {
        if (!u.hasRole(Role.SOCIO)) {
            return new HashSet<>();
        }
        if (requested == null || requested.isEmpty()) {
            throw ApiException.badRequest("Elige si el socio es de Gym, CrossFit o ambos");
        }
        return new HashSet<>(requested);
    }

    private Set<Role> resolveRoles(Set<String> names) {
        Set<Role> found = new HashSet<>(roles.findByNameIn(names));
        if (found.size() != names.size()) {
            throw ApiException.badRequest("Alguno de los roles no existe");
        }
        return found;
    }

    public List<User> search(String name) {
        return users.findByActiveTrueAndFullNameContainingIgnoreCaseOrderByFullName(name == null ? "" : name);
    }
}
