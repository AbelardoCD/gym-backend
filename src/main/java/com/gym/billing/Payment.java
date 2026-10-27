package com.gym.billing;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private MembershipPlan plan;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanPeriod period;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(name = "covers_from", nullable = false)
    private LocalDate coversFrom;

    @Column(name = "covers_to", nullable = false)
    private LocalDate coversTo;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt = Instant.now();
}
