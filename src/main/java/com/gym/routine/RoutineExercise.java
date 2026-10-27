package com.gym.routine;

import com.gym.exercise.Exercise;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Un ejercicio de un día de la rutina, con su orden, series y repeticiones. */
@Entity
@Table(name = "routine_exercises")
@Getter
@Setter
public class RoutineExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    /** 1 = lunes ... 7 = domingo. */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private int position;

    private Integer sets;

    /** Texto libre: "12", "8-10", "30 s", "al fallo". */
    private String reps;

    private String notes;
}
