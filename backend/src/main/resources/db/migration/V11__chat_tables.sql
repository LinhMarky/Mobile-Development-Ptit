-- Phase 7: room conversations, ordered messages and per-member read markers.
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,
    room_title VARCHAR(150),
    last_message_at DATETIME(3),
    last_message_preview VARCHAR(200),
    sequence BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_conversation_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_conversation_tenant FOREIGN KEY (tenant_id) REFERENCES users(id),
    CONSTRAINT fk_conversation_host FOREIGN KEY (host_id) REFERENCES users(id),
    CONSTRAINT ck_conversation_members CHECK (tenant_id <> host_id),
    CONSTRAINT ck_conversation_sequence CHECK (sequence >= 0),
    UNIQUE KEY uk_conversation_room_tenant (room_id, tenant_id),
    INDEX idx_conversation_tenant (tenant_id, last_message_at, id),
    INDEX idx_conversation_host (host_id, last_message_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    client_message_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sequence BIGINT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT ck_message_sequence CHECK (sequence > 0),
    UNIQUE KEY uk_message_client (conversation_id, client_message_id),
    UNIQUE KEY uk_message_sequence (conversation_id, sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE read_markers (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_sequence BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_marker_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_marker_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_marker_sequence CHECK (last_read_sequence >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
