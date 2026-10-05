package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Account deletion request entity (§2.8).
 * Tracks user-initiated account deletion with status lifecycle.
 */
@Entity
@Table(name = "account_deletion_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "reason", length = 1000, nullable = false)
    private String reason = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DeletionStatus status = DeletionStatus.PENDING;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public enum DeletionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
