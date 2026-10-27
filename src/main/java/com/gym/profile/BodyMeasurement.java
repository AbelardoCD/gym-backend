package com.gym.profile;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Una toma de medidas corporales de un socio. */
@Entity
@Table(name = "body_measurements")
@Getter
@Setter
public class BodyMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "body_fat_pct")
    private BigDecimal bodyFatPct;

    @Column(name = "waist_cm")
    private BigDecimal waistCm;

    @Column(name = "chest_cm")
    private BigDecimal chestCm;

    @Column(name = "hip_cm")
    private BigDecimal hipCm;

    @Column(name = "arm_cm")
    private BigDecimal armCm;

    @Column(name = "thigh_cm")
    private BigDecimal thighCm;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
