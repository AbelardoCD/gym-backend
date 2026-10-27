package com.gym.chat;

import com.gym.chat.ChatDtos.*;
import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatRequestResponse send(@Valid @RequestBody SendRequest req, @CurrentUser AuthUser me) {
        return chatService.sendRequest(req.receiverId(), me);
    }

    @GetMapping("/requests/incoming")
    public List<ChatRequestResponse> incoming(@CurrentUser AuthUser me) {
        return chatService.incoming(me);
    }

    @GetMapping("/requests/outgoing")
    public List<ChatRequestResponse> outgoing(@CurrentUser AuthUser me) {
        return chatService.outgoing(me);
    }

    @PostMapping("/requests/{id}/accept")
    public ChatRequestResponse accept(@PathVariable Long id, @CurrentUser AuthUser me) {
        return chatService.respond(id, true, me);
    }

    @PostMapping("/requests/{id}/reject")
    public ChatRequestResponse reject(@PathVariable Long id, @CurrentUser AuthUser me) {
        return chatService.respond(id, false, me);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(@CurrentUser AuthUser me) {
        return chatService.conversations(me);
    }

    /** Mensajes del más nuevo al más viejo; usa beforeId para cargar los anteriores. */
    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id, @RequestParam(required = false) Long beforeId,
                                          @RequestParam(defaultValue = "30") int size, @CurrentUser AuthUser me) {
        return chatService.messages(id, beforeId, size, me);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable Long id, @Valid @RequestBody NewMessage msg, @CurrentUser AuthUser me) {
        return chatService.send(id, msg.content(), me);
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @CurrentUser AuthUser me) {
        chatService.markRead(id, me);
    }
}
