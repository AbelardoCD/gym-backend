package com.gym.chat;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre el usuario con el id menor, para que exista una sola conversación por pareja. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id")
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id")
    private User userB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean hasParticipant(Long userId) {
        return userA.getId().equals(userId) || userB.getId().equals(userId);
    }

    public User other(Long userId) {
        return userA.getId().equals(userId) ? userB : userA;
    }
}
