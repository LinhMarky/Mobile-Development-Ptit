-- =============================================
-- V7: Media module tables (Phase 3, §2.3)
-- =============================================

CREATE TABLE media (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id       BIGINT NOT NULL,
    purpose           VARCHAR(30) NOT NULL,
    resource_type     VARCHAR(50),
    resource_id       BIGINT,
    content_type      VARCHAR(100) NOT NULL,
    file_size_bytes   BIGINT NOT NULL,
    storage_key       VARCHAR(512) NOT NULL UNIQUE,
    original_filename VARCHAR(255),
    width             INT,
    height            INT,
    duration_secs     INT,
    status            VARCHAR(20) NOT NULL DEFAULT 'READY',
    display_order     INT NOT NULL DEFAULT 0,
    version           INT NOT NULL DEFAULT 0,
    created_at        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_media_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    INDEX idx_media_uploader (uploader_id),
    INDEX idx_media_resource (resource_type, resource_id, status),
    INDEX idx_media_status_created (status, created_at),
    INDEX idx_media_purpose (purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
