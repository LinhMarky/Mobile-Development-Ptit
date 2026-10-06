-- =============================================
-- V9: Booking module tables (Phase 5, §2.5)
-- bookings, booking_status_history, booking_cases,
-- case_evidence
-- =============================================

-- Bookings
CREATE TABLE bookings (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL,
    host_id                     BIGINT NOT NULL,
    room_id                     BIGINT NOT NULL,
    listing_id                  BIGINT NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    desired_move_in             DATE,
    occupant_count              INT NOT NULL DEFAULT 1,
    rent_vnd                    DECIMAL(18,0) NOT NULL,
    deposit_vnd                 DECIMAL(18,0) NOT NULL DEFAULT 0,
    note                        VARCHAR(1000),
    terms_snapshot              JSON NOT NULL,
    snapshot_schema_version     INT NOT NULL DEFAULT 1,
    request_expires_at          DATETIME(3) NOT NULL,
    hold_expires_at             DATETIME(3),
    handover_due_at             DATETIME(3),
    tenant_terms_accepted_at    DATETIME(3),
    tenant_handover_confirmed_at DATETIME(3),
    host_handover_confirmed_at  DATETIME(3),
    allocated_payment_id        BIGINT UNIQUE,
    completed_at                DATETIME(3),
    cancelled_at                DATETIME(3),
    last_reason                 VARCHAR(1000),
    version                     INT NOT NULL DEFAULT 0,
    created_at                  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_bookings_tenant FOREIGN KEY (tenant_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_host FOREIGN KEY (host_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_bookings_listing FOREIGN KEY (listing_id) REFERENCES listings(id),
    INDEX idx_bookings_tenant (tenant_id, status, created_at, id),
    INDEX idx_bookings_host (host_id, status, created_at, id),
    INDEX idx_bookings_expire_pending (status, request_expires_at),
    INDEX idx_bookings_expire_hold (status, hold_expires_at),
    INDEX idx_bookings_expire_handover (status, handover_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Generated columns for concurrency control (BR-07)
-- active_room_id: only non-null when APPROVED or CONFIRMED → UNIQUE ensures 1 active booking per room
ALTER TABLE bookings
    ADD COLUMN active_room_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('APPROVED','CONFIRMED') THEN room_id ELSE NULL END
    ) STORED,
    ADD UNIQUE KEY uk_bookings_active_room (active_room_id);

-- open_room_id: only non-null when PENDING/APPROVED/CONFIRMED → UNIQUE(tenant_id, open_room_id)
-- prevents same tenant from having multiple open bookings for the same room
ALTER TABLE bookings
    ADD COLUMN open_room_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING','APPROVED','CONFIRMED') THEN room_id ELSE NULL END
    ) STORED,
    ADD UNIQUE KEY uk_bookings_tenant_open_room (tenant_id, open_room_id);

-- Booking status history
CREATE TABLE booking_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    actor_id    BIGINT,
    actor_type  VARCHAR(20) NOT NULL,
    reason      VARCHAR(1000),
    trace_id    VARCHAR(64) NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_booking_history_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_booking_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_booking_history (booking_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Booking cases (disputes)
CREATE TABLE booking_cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT NOT NULL,
    type            VARCHAR(30) NOT NULL,
    opened_by       BIGINT,
    description     TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    decision        VARCHAR(30),
    resolution_note TEXT,
    resolved_by     BIGINT,
    resolved_at     DATETIME(3),
    version         INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_cases_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_cases_opener FOREIGN KEY (opened_by) REFERENCES users(id),
    CONSTRAINT fk_cases_resolver FOREIGN KEY (resolved_by) REFERENCES users(id),
    INDEX idx_cases_booking (booking_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Generated column: only 1 open case per booking
ALTER TABLE booking_cases
    ADD COLUMN open_booking_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('OPEN','IN_REVIEW') THEN booking_id ELSE NULL END
    ) STORED,
    ADD UNIQUE KEY uk_cases_open_booking (open_booking_id);

-- Case evidence (media linked to cases)
CREATE TABLE case_evidence (
    case_id     BIGINT NOT NULL,
    media_id    BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (case_id, media_id),
    CONSTRAINT fk_evidence_case FOREIGN KEY (case_id) REFERENCES booking_cases(id),
    CONSTRAINT fk_evidence_media FOREIGN KEY (media_id) REFERENCES media(id),
    CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
