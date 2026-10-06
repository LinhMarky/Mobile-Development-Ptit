-- =============================================
-- V1: Users table (Auth module)
-- =============================================

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(15),
    avatar_url  VARCHAR(512),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_host     BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_token TEXT,
    version     INT NOT NULL DEFAULT 0,
    created_by  VARCHAR(255),
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_by  VARCHAR(255),
    updated_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
