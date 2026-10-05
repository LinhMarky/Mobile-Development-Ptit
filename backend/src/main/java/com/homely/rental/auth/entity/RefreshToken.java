package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Refresh token entity for session management (SEC-02).
 * Each login creates a new session/refresh token record.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", length = 36, nullable = false)
    private String installationId;

    @Column(name = "device_name", length = 100, nullable = false)
    private String deviceName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked")
    private boolean revoked = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}
