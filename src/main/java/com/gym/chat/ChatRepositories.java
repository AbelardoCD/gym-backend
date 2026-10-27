package com.gym.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {
    @Query("""
            select count(r) > 0 from ChatRequest r
            where r.status = com.gym.chat.ChatRequestStatus.PENDIENTE
              and ((r.sender.id = :a and r.receiver.id = :b) or (r.sender.id = :b and r.receiver.id = :a))""")
    boolean existsPendingBetween(Long a, Long b);

    @Query("select r from ChatRequest r join fetch r.sender where r.receiver.id = :userId and r.status = :status order by r.createdAt desc")
    List<ChatRequest> findIncoming(Long userId, ChatRequestStatus status);

    @Query("select r from ChatRequest r join fetch r.receiver where r.sender.id = :userId and r.status = :status order by r.createdAt desc")
    List<ChatRequest> findOutgoing(Long userId, ChatRequestStatus status);
}

interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserAIdAndUserBId(Long userAId, Long userBId);

    @Query("select c from Conversation c join fetch c.userA join fetch c.userB where c.userA.id = :userId or c.userB.id = :userId")
    List<Conversation> findForUser(Long userId);
}

interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m where m.conversation.id = :conversationId and (:beforeId is null or m.id < :beforeId) order by m.id desc")
    List<Message> findPage(Long conversationId, Long beforeId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderByIdDesc(Long conversationId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long userId);

    @Modifying
    @Query("update Message m set m.readAt = :now where m.conversation.id = :conversationId and m.sender.id <> :userId and m.readAt is null")
    int markRead(Long conversationId, Long userId, Instant now);
}
