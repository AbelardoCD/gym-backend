package com.gym.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Una pantalla del sistema. Los roles la reciben en role_permissions. */
@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {
    public static final String INICIO = "INICIO";
    public static final String RUTINAS = "RUTINAS";
    public static final String ASISTENCIA = "ASISTENCIA";
    public static final String ASISTENCIA_GESTION = "ASISTENCIA_GESTION";
    public static final String CHAT = "CHAT";
    public static final String USUARIOS = "USUARIOS";
    public static final String ROLES = "ROLES";
    public static final String PAGOS = "PAGOS";
    public static final String MEMBRESIAS = "MEMBRESIAS";
    public static final String REPORTES = "REPORTES";
    public static final String PROGRESO = "PROGRESO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
