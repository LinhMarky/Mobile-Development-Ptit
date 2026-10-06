-- =============================================
-- V10: Payment module tables (Phase 6, §2.6)
-- payments, payment_webhook_receipts, refunds
-- =============================================

CREATE TABLE payments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id           BIGINT NOT NULL,
    payer_id             BIGINT NOT NULL,
    provider             VARCHAR(20) NOT NULL DEFAULT 'MOCK_SANDBOX',
    provider_payment_id  VARCHAR(100) UNIQUE,
    amount_vnd           DECIMAL(18,0) NOT NULL,
    currency             CHAR(3) NOT NULL DEFAULT 'VND',
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at           DATETIME(3) NOT NULL,
    succeeded_at         DATETIME(3),
    failure_code         VARCHAR(100),
    is_test              BOOLEAN NOT NULL DEFAULT TRUE,
    version              INT NOT NULL DEFAULT 0,
    created_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_payments_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    INDEX idx_payments_booking (booking_id),
    INDEX idx_payments_status (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Generated column: only 1 pending payment per booking
ALTER TABLE payments
    ADD COLUMN pending_booking_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN booking_id ELSE NULL END
    ) STORED,
    ADD UNIQUE KEY uk_payments_pending_booking (pending_booking_id);

-- FK from bookings.allocated_payment_id to payments
ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_payment FOREIGN KEY (allocated_payment_id) REFERENCES payments(id);

CREATE TABLE payment_webhook_receipts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider      VARCHAR(20) NOT NULL,
    event_id      CHAR(36) NOT NULL,
    payment_id    BIGINT,
    payload_hash  BINARY(32) NOT NULL,
    outcome       VARCHAR(30),
    processed_at  DATETIME(3) NOT NULL,
    created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_webhook_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    UNIQUE KEY uk_webhook_provider_event (provider, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refunds (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id           BIGINT NOT NULL UNIQUE,
    provider_refund_id   VARCHAR(100) UNIQUE,
    amount_vnd           DECIMAL(18,0) NOT NULL,
    reason               VARCHAR(30),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count        INT NOT NULL DEFAULT 0,
    next_attempt_at      DATETIME(3),
    last_error_code      VARCHAR(100),
    completed_at         DATETIME(3),
    version              INT NOT NULL DEFAULT 0,
    created_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_refunds_status (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
