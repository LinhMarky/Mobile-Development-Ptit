package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One-time token for email verification and password reset flows (SEC-03).
 */
@Entity
@Table(name = "one_time_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30, nullable = false)
    private OneTimeTokenPurpose purpose;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public enum OneTimeTokenPurpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
