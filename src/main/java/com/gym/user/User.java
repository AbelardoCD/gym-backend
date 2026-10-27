package com.gym.user;

import com.gym.routine.Discipline;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /** Subroles del socio (Gym, CrossFit o ambos). Vacío si no es socio. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_disciplines", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "discipline")
    private Set<Discipline> disciplines = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    /** Fecha de inscripción; los pagos de su primer periodo empiezan aquí. */
    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn = LocalDate.now();

    /** Clave de la foto en el almacenamiento; null si no tiene. */
    @Column(name = "photo_key")
    private String photoKey;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "emergency_name")
    private String emergencyName;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    /** Lesiones, alergias o padecimientos que el entrenador debe conocer. */
    @Column(name = "medical_notes")
    private String medicalNotes;

    /** Código de la credencial QR; se puede regenerar si el socio la pierde. */
    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken = newQrToken();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Solo letras y números: los lectores USB "teclean" el código y los símbolos cambian según la distribución del teclado. */
    public static String newQrToken() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    /** Lo que va dentro del QR: un prefijo para reconocerlo y el código. */
    public String qrCode() {
        return QR_PREFIX + qrToken;
    }

    public static final String QR_PREFIX = "GYM:";

    /** Cambia con cada foto nueva; el cliente lo agrega a la URL para no mostrar la anterior desde caché. */
    public String photoVersion() {
        return photoKey == null ? null : Integer.toHexString(photoKey.hashCode());
    }

    public Set<String> roleNames() {
        Set<String> names = new java.util.TreeSet<>();
        roles.forEach(r -> names.add(r.getName()));
        return names;
    }

    public boolean hasRole(String name) {
        return roles.stream().anyMatch(r -> r.getName().equals(name));
    }
}
