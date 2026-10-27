package com.gym.attendance;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attendances")
@Getter
@Setter
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt = Instant.now();

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;
}
