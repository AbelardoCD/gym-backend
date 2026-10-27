package com.gym.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndCreatedAtAfter(Long userId, Instant after);

    /** Invalida los enlaces anteriores del usuario al pedir uno nuevo. */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    void invalidateAll(Long userId, Instant now);
}
