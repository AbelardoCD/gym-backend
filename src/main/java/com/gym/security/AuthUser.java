package com.gym.security;

import com.gym.user.Permission;
import com.gym.user.Role;
import com.gym.user.User;

import java.security.Principal;
import java.util.Set;
import java.util.TreeSet;

/**
 * Usuario autenticado. Se arma en cada petición desde la base de datos, así que un cambio de
 * roles o permisos aplica de inmediato. El nombre del principal es el id, para que los mensajes de
 * WebSocket dirigidos a "/user/{id}/..." lleguen a la persona correcta.
 */
public record AuthUser(Long id, String email, Set<String> roles, Set<String> permissions) implements Principal {

    public static AuthUser from(User u) {
        Set<String> perms = new TreeSet<>();
        u.getRoles().forEach(r -> r.getPermissions().forEach(p -> perms.add(p.getCode())));
        return new AuthUser(u.getId(), u.getEmail(), u.roleNames(), perms);
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean can(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMINISTRADOR);
    }

    /** Puede registrar y consultar la asistencia de otros usuarios. */
    public boolean managesAttendance() {
        return can(Permission.ASISTENCIA_GESTION);
    }
}
