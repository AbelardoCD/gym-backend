package com.gym.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findAllByOrderByMuscleGroupAscNameAsc();

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCase(String name);

    @Query("select count(distinct re.routine.id) from RoutineExercise re where re.exercise.id = :exerciseId")
    long countUses(Long exerciseId);
}
