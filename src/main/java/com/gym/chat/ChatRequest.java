package com.gym.chat;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "chat_requests")
@Getter
@Setter
public class ChatRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRequestStatus status = ChatRequestStatus.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;
}
