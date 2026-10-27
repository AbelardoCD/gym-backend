package com.gym.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    /** Entrada sin salida desde cierto momento (las de días anteriores sin salida ya no cuentan). */
    Optional<Attendance> findFirstByUserIdAndCheckOutAtIsNullAndCheckInAtGreaterThanEqualOrderByCheckInAtDesc(Long userId, Instant since);

    @Query("""
            select a from Attendance a join fetch a.user
            where (:userId is null or a.user.id = :userId)
              and a.checkInAt >= :from and a.checkInAt < :to
            order by a.checkInAt desc""")
    List<Attendance> search(Long userId, Instant from, Instant to);
}
