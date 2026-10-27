package com.gym.user;

import com.gym.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequiredArgsConstructor
@Transactional
public class RoleController {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;

    public record PermissionResponse(Long id, String code, String name, String description) {
        static PermissionResponse from(Permission p) {
            return new PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getDescription());
        }
    }

    public record RoleResponse(Long id, String name, String description, Set<String> permissions, long users) {}

    public record RoleRequest(@NotBlank @Pattern(regexp = "[A-Za-z_]{3,30}", message = "Solo letras y guion bajo, de 3 a 30") String name,
                              String description,
                              Set<String> permissions) {}

    /** Pantallas que se pueden dar a un rol. */
    @GetMapping("/api/permissions")
    @PreAuthorize("hasAuthority('ROLES')")
    public List<PermissionResponse> permissions() {
        return permissions.findAllByOrderBySortOrder().stream().map(PermissionResponse::from).toList();
    }

    @GetMapping("/api/roles")
    @PreAuthorize("hasAnyAuthority('ROLES','USUARIOS')")
    public List<RoleResponse> list() {
        return roles.findAllByOrderByName().stream().map(this::toResponse).toList();
    }

    @GetMapping("/api/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES')")
    public RoleResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping("/api/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLES')")
    public RoleResponse create(@Valid @RequestBody RoleRequest req) {
        String name = normalize(req.name());
        if (roles.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese rol ya existe");
        }
        Role r = new Role();
        apply(r, name, req);
        return toResponse(roles.save(r));
    }

    @PutMapping("/api/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES')")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest req) {
        Role r = find(id);
        String name = normalize(req.name());
        if (!name.equals(r.getName()) && roles.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese rol ya existe");
        }
        if (Role.ADMINISTRADOR.equals(r.getName())) {
            if (!name.equals(r.getName())) {
                throw ApiException.badRequest("El rol ADMINISTRADOR no se puede renombrar");
            }
            if (req.permissions() == null || !req.permissions().contains(Permission.ROLES)) {
                throw ApiException.badRequest("El administrador debe conservar el permiso de Roles y permisos");
            }
        }
        apply(r, name, req);
        return toResponse(r);
    }

    @DeleteMapping("/api/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLES')")
    public void delete(@PathVariable Long id) {
        Role r = find(id);
        if (Role.ADMINISTRADOR.equals(r.getName())) {
            throw ApiException.badRequest("El rol ADMINISTRADOR no se puede eliminar");
        }
        if (users.countByRoles_Id(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Hay usuarios con este rol; quítaselo antes de eliminarlo");
        }
        roles.delete(r);
    }

    private void apply(Role r, String name, RoleRequest req) {
        r.setName(name);
        r.setDescription(req.description());
        Set<String> codes = req.permissions() == null ? Set.of() : req.permissions();
        Set<Permission> found = new HashSet<>(permissions.findByCodeIn(codes));
        if (found.size() != codes.size()) {
            throw ApiException.badRequest("Alguno de los permisos no existe");
        }
        r.getPermissions().clear();
        r.getPermissions().addAll(found);
    }

    private RoleResponse toResponse(Role r) {
        Set<String> codes = new TreeSet<>();
        r.getPermissions().forEach(p -> codes.add(p.getCode()));
        return new RoleResponse(r.getId(), r.getName(), r.getDescription(), codes, users.countByRoles_Id(r.getId()));
    }

    private Role find(Long id) {
        return roles.findById(id).orElseThrow(() -> ApiException.notFound("Rol no encontrado"));
    }

    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
