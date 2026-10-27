package com.gym.routine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByOrderByCreatedAtDesc();

    List<Routine> findByTrainerIdOrderByCreatedAtDesc(Long trainerId);

    @Query("select r from Routine r join r.members m where m.id = :memberId order by r.createdAt desc")
    List<Routine> findAssignedTo(Long memberId);
}
