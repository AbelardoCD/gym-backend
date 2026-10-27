package com.gym.chat;

import com.gym.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;
}
