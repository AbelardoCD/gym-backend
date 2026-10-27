package com.gym.chat;

import com.gym.chat.ChatDtos.*;
import com.gym.common.ApiException;
import com.gym.security.AuthUser;
import com.gym.user.User;
import com.gym.user.UserDtos.UserSummary;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    static final String QUEUE = "/queue/events";

    private final ChatRequestRepository requests;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    private final SimpMessagingTemplate ws;

    // ---------- solicitudes ----------

    public ChatRequestResponse sendRequest(Long receiverId, AuthUser me) {
        if (receiverId.equals(me.id())) {
            throw ApiException.badRequest("No puedes enviarte una solicitud a ti mismo");
        }
        User receiver = users.findById(receiverId).filter(User::isActive)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
        if (findConversation(me.id(), receiverId) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya pueden chatear");
        }
        if (requests.existsPendingBetween(me.id(), receiverId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya hay una solicitud pendiente entre ustedes");
        }
        ChatRequest r = new ChatRequest();
        r.setSender(users.getReferenceById(me.id()));
        r.setReceiver(receiver);
        requests.save(r);
        ChatRequestResponse res = ChatRequestResponse.from(requests.findById(r.getId()).orElseThrow(), null);
        notify(receiverId, "SOLICITUD_RECIBIDA", res);
        return res;
    }

    @Transactional(readOnly = true)
    public List<ChatRequestResponse> incoming(AuthUser me) {
        return requests.findIncoming(me.id(), ChatRequestStatus.PENDIENTE).stream()
                .map(r -> ChatRequestResponse.from(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRequestResponse> outgoing(AuthUser me) {
        return requests.findOutgoing(me.id(), ChatRequestStatus.PENDIENTE).stream()
                .map(r -> ChatRequestResponse.from(r, null)).toList();
    }

    public ChatRequestResponse respond(Long requestId, boolean accept, AuthUser me) {
        ChatRequest r = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        if (!r.getReceiver().getId().equals(me.id())) {
            throw ApiException.forbidden("Solo quien recibe la solicitud puede responderla");
        }
        if (r.getStatus() != ChatRequestStatus.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud ya fue respondida");
        }
        r.setStatus(accept ? ChatRequestStatus.ACEPTADA : ChatRequestStatus.RECHAZADA);
        r.setRespondedAt(Instant.now());
        Long conversationId = null;
        if (accept) {
            conversationId = getOrCreateConversation(r.getSender(), r.getReceiver()).getId();
        }
        ChatRequestResponse res = ChatRequestResponse.from(r, conversationId);
        notify(r.getSender().getId(), accept ? "SOLICITUD_ACEPTADA" : "SOLICITUD_RECHAZADA", res);
        return res;
    }

    // ---------- conversaciones y mensajes ----------

    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(AuthUser me) {
        return conversations.findForUser(me.id()).stream()
                .map(c -> new ConversationResponse(c.getId(), UserSummary.from(c.other(me.id())),
                        messages.findFirstByConversationIdOrderByIdDesc(c.getId()).map(MessageResponse::from).orElse(null),
                        messages.countByConversationIdAndSenderIdNotAndReadAtIsNull(c.getId(), me.id())))
                .sorted(Comparator.comparing((ConversationResponse c) ->
                        c.lastMessage() == null ? Instant.EPOCH : c.lastMessage().sentAt()).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> messages(Long conversationId, Long beforeId, int size, AuthUser me) {
        requireParticipant(conversationId, me);
        return messages.findPage(conversationId, beforeId, PageRequest.of(0, Math.min(size, 100))).stream()
                .map(MessageResponse::from).toList();
    }

    public MessageResponse send(Long conversationId, String content, AuthUser me) {
        Conversation c = requireParticipant(conversationId, me);
        Message m = new Message();
        m.setConversation(c);
        m.setSender(users.getReferenceById(me.id()));
        m.setContent(content.strip());
        MessageResponse res = MessageResponse.from(messages.save(m));
        notify(c.getUserA().getId(), "MENSAJE", res);
        notify(c.getUserB().getId(), "MENSAJE", res);
        return res;
    }

    public void markRead(Long conversationId, AuthUser me) {
        Conversation c = requireParticipant(conversationId, me);
        if (messages.markRead(conversationId, me.id(), Instant.now()) > 0) {
            notify(c.other(me.id()).getId(), "LEIDO", conversationId);
        }
    }

    private Conversation requireParticipant(Long conversationId, AuthUser me) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversación no encontrada"));
        if (!c.hasParticipant(me.id())) {
            throw ApiException.forbidden("No participas en esta conversación");
        }
        return c;
    }

    private Conversation findConversation(Long a, Long b) {
        return conversations.findByUserAIdAndUserBId(Math.min(a, b), Math.max(a, b)).orElse(null);
    }

    private Conversation getOrCreateConversation(User x, User y) {
        Conversation existing = findConversation(x.getId(), y.getId());
        if (existing != null) {
            return existing;
        }
        Conversation c = new Conversation();
        c.setUserA(x.getId() < y.getId() ? x : y);
        c.setUserB(x.getId() < y.getId() ? y : x);
        return conversations.save(c);
    }

    private void notify(Long userId, String type, Object payload) {
        ws.convertAndSendToUser(String.valueOf(userId), QUEUE, new ChatEvent(type, payload));
    }
}
