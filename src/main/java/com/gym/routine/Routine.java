package com.gym.routine;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Entity
@Table(name = "routines")
@Getter
@Setter
public class Routine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Discipline discipline = Discipline.GYM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id")
    private User trainer;

    @ManyToMany
    @JoinTable(name = "routine_members",
            joinColumns = @JoinColumn(name = "routine_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, position ASC")
    private List<RoutineExercise> exercises = new ArrayList<>();

    /** Descripción de cada día (clave 1 = lunes ... 7 = domingo); solo los días que la tienen. */
    @ElementCollection
    @CollectionTable(name = "routine_days", joinColumns = @JoinColumn(name = "routine_id"))
    @MapKeyColumn(name = "day_of_week")
    @Column(name = "description", nullable = false)
    private Map<Integer, String> dayDescriptions = new TreeMap<>();

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC")
    private List<RoutineFile> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
