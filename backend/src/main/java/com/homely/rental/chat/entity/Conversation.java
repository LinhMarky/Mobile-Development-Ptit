package com.homely.rental.chat.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.catalog.entity.Room;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "tenant_id"}))
@Getter
@Setter
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "room_title", length = 150)
    private String roomTitle;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    @Column(nullable = false)
    private long sequence;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
}
