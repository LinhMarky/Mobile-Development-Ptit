package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * User notification preferences (AUTH11).
 * Tracks which notification channels the user has enabled.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "transaction_push", nullable = false)
    private boolean transactionPush = true;

    @Column(name = "transaction_email", nullable = false)
    private boolean transactionEmail = true;

    @Column(name = "chat_push", nullable = false)
    private boolean chatPush = true;

    @Column(name = "recommendation_push", nullable = false)
    private boolean recommendationPush = true;

    @Version
    private int version;

    @Column(name = "created_at", updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Instant updatedAt = Instant.now();
}
