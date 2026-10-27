package com.gym.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
            select p from Payment p join fetch p.user join fetch p.registeredBy
            where (:userId is null or p.user.id = :userId)
              and p.paidAt >= :from and p.paidAt < :to
            order by p.paidAt desc""")
    List<Payment> search(Long userId, Instant from, Instant to);

    @Query("select max(p.coversTo) from Payment p where p.user.id = :userId")
    Optional<LocalDate> paidUntil(Long userId);

    /** Fecha hasta la que tiene pagado cada usuario: [userId, coversTo]. */
    @Query("select p.user.id, max(p.coversTo) from Payment p where p.user.id in :userIds group by p.user.id")
    List<Object[]> paidUntilByUser(Collection<Long> userIds);

    long countByPlanId(Long planId);

    /** Último día pagado de cada usuario que tiene pagos: [userId, coversTo]. */
    @Query("select p.user.id, max(p.coversTo) from Payment p group by p.user.id")
    List<Object[]> paidUntilAll();
}
