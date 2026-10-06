-- Keep the newest registration when upgrading legacy duplicate devices.
UPDATE device_tokens older
JOIN device_tokens newer ON older.id < newer.id AND newer.active = TRUE
    AND (older.installation_id = newer.installation_id OR BINARY older.token = BINARY newer.token)
SET older.active = FALSE
WHERE older.active = TRUE;

ALTER TABLE device_tokens
    ADD COLUMN active_installation VARCHAR(36)
        GENERATED ALWAYS AS (IF(active, installation_id, NULL)) STORED,
    ADD COLUMN active_token_hash VARCHAR(64)
        GENERATED ALWAYS AS (IF(active, SHA2(token, 256), NULL)) STORED,
    ADD UNIQUE KEY uk_device_active_installation (active_installation),
    ADD UNIQUE KEY uk_device_active_token (active_token_hash);
