package com.homely.rental.chat.repository;

import com.homely.rental.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Optional<Message> findByConversationIdAndClientMessageId(Long conversationId, String clientMessageId);
    List<Message> findByConversationIdOrderBySequenceDesc(Long conversationId, Pageable pageable);
    List<Message> findByConversationIdAndSequenceLessThanOrderBySequenceDesc(Long conversationId, long sequence, Pageable pageable);
    List<Message> findByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(Long conversationId, long sequence, Pageable pageable);
    long countByConversationIdAndSenderIdNotAndSequenceGreaterThan(Long conversationId, Long senderId, long sequence);
}
