CREATE TABLE push_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    last_error VARCHAR(100),
    created_at DATETIME(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_push_notification FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_push_device FOREIGN KEY (device_id) REFERENCES device_tokens(id),
    UNIQUE KEY uk_push_notification_device (notification_id, device_id),
    INDEX idx_push_due (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
