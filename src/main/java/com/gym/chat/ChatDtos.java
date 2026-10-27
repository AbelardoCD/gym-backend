package com.gym.chat;

import com.gym.user.UserDtos.UserSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ChatDtos {
    private ChatDtos() {}

    public record SendRequest(@NotNull Long receiverId) {}

    public record ChatRequestResponse(Long id, UserSummary sender, UserSummary receiver, ChatRequestStatus status,
                                      Instant createdAt, Long conversationId) {
        static ChatRequestResponse from(ChatRequest r, Long conversationId) {
            return new ChatRequestResponse(r.getId(), UserSummary.from(r.getSender()), UserSummary.from(r.getReceiver()),
                    r.getStatus(), r.getCreatedAt(), conversationId);
        }
    }

    public record NewMessage(@NotBlank @Size(max = 4000) String content) {}

    public record MessageResponse(Long id, Long conversationId, Long senderId, String content, Instant sentAt, Instant readAt) {
        static MessageResponse from(Message m) {
            return new MessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(), m.getContent(),
                    m.getSentAt(), m.getReadAt());
        }
    }

    public record ConversationResponse(Long id, UserSummary contact, MessageResponse lastMessage, long unread) {}

    /** Evento que se envía por WebSocket a /user/queue/events. */
    public record ChatEvent(String type, Object payload) {}
}
