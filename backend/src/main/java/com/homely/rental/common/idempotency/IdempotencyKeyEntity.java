package com.homely.rental.common.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores idempotency keys to prevent duplicate processing of POST mutation requests.
 * Key = Idempotency-Key header + principal.id + request_path.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 of principal, HTTP method, request path and client key. */
    @Column(name = "idempotency_key", length = 512, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_content_type", length = 255)
    private String responseContentType;

    @Column(name = "response_location", length = 2048)
    private String responseLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public enum IdempotencyStatus {
        PROCESSING,
        COMPLETED
    }
}
