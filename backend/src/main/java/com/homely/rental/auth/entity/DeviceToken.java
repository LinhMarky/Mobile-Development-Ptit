package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores FCM device tokens for push notifications (AUTH10).
 */
@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "installation_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", length = 4096, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 10, nullable = false)
    private Platform platform = Platform.ANDROID;

    @Column(name = "device_name", length = 100, nullable = false)
    private String deviceName;

    @Column(name = "installation_id", length = 36, nullable = false)
    private String installationId;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public enum Platform {
        ANDROID
    }
}
