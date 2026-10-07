-- Legacy records do not match the new hashed, authenticated key scope.
ALTER TABLE idempotency_keys
    ADD COLUMN request_hash VARCHAR(64) NULL,
    ADD COLUMN response_content_type VARCHAR(255) NULL,
    ADD COLUMN response_location VARCHAR(2048) NULL,
    MODIFY COLUMN response_body LONGTEXT;
