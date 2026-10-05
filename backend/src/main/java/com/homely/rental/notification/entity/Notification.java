package com.homely.rental.notification.entity;

import com.homely.rental.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "body", length = 2000)
    private String body;

    /** Reference type: "booking", "viewing", "review", "listing", "message" */
    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
}
