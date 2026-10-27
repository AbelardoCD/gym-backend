package com.gym.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/** Un recordatorio ya enviado; la llave evita repetirlo para el mismo vencimiento. */
@Entity
@Table(name = "membership_reminders")
@IdClass(MembershipReminder.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class MembershipReminder {
    public enum Kind { POR_VENCER, VENCIDA }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "paid_until")
    private LocalDate paidUntil;

    @Id
    @Enumerated(EnumType.STRING)
    private Kind kind;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    public MembershipReminder(Long userId, LocalDate paidUntil, Kind kind) {
        this.userId = userId;
        this.paidUntil = paidUntil;
        this.kind = kind;
    }

    public record Key(Long userId, LocalDate paidUntil, Kind kind) implements Serializable {
        public Key() {
            this(null, null, null);
        }
    }
}
