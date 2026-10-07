-- =============================================
-- V2: Auth module tables (Phase 1)
-- refresh_tokens, one_time_tokens, device_tokens,
-- notification_preferences, account_deletion_requests,
-- idempotency_keys
-- =============================================

-- Refresh tokens (session management, SEC-02)
CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    installation_id VARCHAR(36) NOT NULL,
    device_name     VARCHAR(100) NOT NULL,
    expires_at      DATETIME(3) NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_refresh_tokens_user (user_id),
    INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- One-time tokens (email verification, SEC-03)
CREATE TABLE one_time_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL,
    purpose     VARCHAR(30) NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  DATETIME(3) NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_one_time_tokens_user (user_id),
    INDEX idx_one_time_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Device tokens (FCM push, AUTH10)
CREATE TABLE device_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token           VARCHAR(4096) NOT NULL,
    platform        VARCHAR(10) NOT NULL DEFAULT 'ANDROID',
    device_name     VARCHAR(100) NOT NULL,
    installation_id VARCHAR(36) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_device_tokens_user_install (user_id, installation_id),
    INDEX idx_device_tokens_user_active (user_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification preferences (AUTH11)
CREATE TABLE notification_preferences (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT NOT NULL UNIQUE,
    transaction_push     BOOLEAN NOT NULL DEFAULT TRUE,
    transaction_email    BOOLEAN NOT NULL DEFAULT TRUE,
    chat_push            BOOLEAN NOT NULL DEFAULT TRUE,
    recommendation_push  BOOLEAN NOT NULL DEFAULT TRUE,
    version              INT NOT NULL DEFAULT 0,
    created_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_notification_prefs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Account deletion requests (§2.8, AUTH12)
CREATE TABLE account_deletion_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    reason          VARCHAR(1000) NOT NULL DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_code    VARCHAR(100),
    completed_at    DATETIME(3),
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_deletion_requests_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_deletion_requests_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Idempotency keys (BR-12)
CREATE TABLE idempotency_keys (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key     VARCHAR(512) NOT NULL UNIQUE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    response_status_code INT,
    response_body       TEXT,
    expires_at          DATETIME(3) NOT NULL,
    created_at          DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_idempotency_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
