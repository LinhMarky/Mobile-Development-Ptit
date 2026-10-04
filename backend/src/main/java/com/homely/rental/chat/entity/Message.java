package com.homely.rental.chat.entity;

import com.homely.rental.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"conversation_id", "client_message_id"}),
        @UniqueConstraint(columnNames = {"conversation_id", "sequence"})})
@Getter
@Setter
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false, updatable = false)
    private String content;

    @Column(name = "client_message_id", length = 36, nullable = false, updatable = false)
    private String clientMessageId;

    @Column(nullable = false, updatable = false)
    private long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20, nullable = false, updatable = false)
    private MessageContentType contentType = MessageContentType.TEXT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
}
