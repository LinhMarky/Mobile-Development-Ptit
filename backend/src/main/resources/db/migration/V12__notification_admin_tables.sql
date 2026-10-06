-- =============================================
-- V12: Notification + Admin tables (Phase 8)
-- notifications, audit_logs
-- =============================================

-- Notifications
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        VARCHAR(2000),
    ref_type    VARCHAR(50),
    ref_id      BIGINT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notifications_user (user_id, is_read, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit logs
CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id        BIGINT,
    actor_type      VARCHAR(20) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
    target_id       BIGINT NOT NULL,
    detail          JSON,
    ip_address      VARCHAR(45),
    trace_id        VARCHAR(64),
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_audit_target (target_type, target_id, created_at),
    INDEX idx_audit_actor (actor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
