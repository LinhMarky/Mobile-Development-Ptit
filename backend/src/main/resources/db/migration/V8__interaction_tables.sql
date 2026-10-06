-- =============================================
-- V8: Interaction module tables (Phase 4, §2.4)
-- viewing_slots, viewings, reviews, reports,
-- viewing_status_history
-- =============================================

-- Viewing slots (host creates available time slots)
CREATE TABLE viewing_slots (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT NOT NULL,
    host_id     BIGINT NOT NULL,
    start_at    DATETIME(3) NOT NULL,
    end_at      DATETIME(3) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_viewing_slots_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_viewing_slots_host FOREIGN KEY (host_id) REFERENCES users(id),
    INDEX idx_viewing_slots_room (room_id, status, start_at),
    INDEX idx_viewing_slots_host (host_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Viewings (tenant books a slot)
CREATE TABLE viewings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    viewing_slot_id   BIGINT NOT NULL,
    tenant_id         BIGINT NOT NULL,
    room_id           BIGINT NOT NULL,
    host_id           BIGINT NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    note              VARCHAR(1000),
    cancelled_reason  VARCHAR(1000),
    completed_at      DATETIME(3),
    version           INT NOT NULL DEFAULT 0,
    created_at        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_viewings_slot FOREIGN KEY (viewing_slot_id) REFERENCES viewing_slots(id),
    CONSTRAINT fk_viewings_tenant FOREIGN KEY (tenant_id) REFERENCES users(id),
    CONSTRAINT fk_viewings_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_viewings_host FOREIGN KEY (host_id) REFERENCES users(id),
    INDEX idx_viewings_tenant (tenant_id, status),
    INDEX idx_viewings_room (room_id, status),
    INDEX idx_viewings_host (host_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Viewing status history
CREATE TABLE viewing_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    viewing_id  BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    actor_id    BIGINT,
    actor_type  VARCHAR(20) NOT NULL,
    reason      VARCHAR(1000),
    trace_id    VARCHAR(64) NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_viewing_history_viewing FOREIGN KEY (viewing_id) REFERENCES viewings(id),
    CONSTRAINT fk_viewing_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_viewing_history (viewing_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reviews (tenant reviews room after booking COMPLETED)
CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT NOT NULL UNIQUE,
    reviewer_id BIGINT NOT NULL,
    room_id     BIGINT NOT NULL,
    rating      INT NOT NULL,
    comment     VARCHAR(2000),
    visible     BOOLEAN NOT NULL DEFAULT TRUE,
    hide_reason VARCHAR(500),
    version     INT NOT NULL DEFAULT 0,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    INDEX idx_reviews_room (room_id, visible, created_at),
    INDEX idx_reviews_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reports (user reports content/behavior)
CREATE TABLE reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id     BIGINT NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
    target_id       BIGINT NOT NULL,
    reason_code     VARCHAR(50) NOT NULL,
    description     VARCHAR(2000),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by     BIGINT,
    resolution_note VARCHAR(1000),
    resolved_at     DATETIME(3),
    version         INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by) REFERENCES users(id),
    INDEX idx_reports_target (target_type, target_id),
    INDEX idx_reports_status (status),
    INDEX idx_reports_reporter (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
