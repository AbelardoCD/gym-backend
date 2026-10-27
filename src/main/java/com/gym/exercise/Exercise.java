package com.gym.exercise;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "exercises")
@Getter
@Setter
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false)
    private MuscleGroup muscleGroup;

    /** Los inactivos no se ofrecen para rutinas nuevas, pero se conservan en las que ya los usan. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
