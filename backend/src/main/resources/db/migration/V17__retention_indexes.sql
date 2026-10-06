-- Bounded cleanup scans must not scan the complete delivery/idempotency tables.
CREATE INDEX idx_push_deliveries_retention ON push_deliveries (status, created_at, id);
CREATE INDEX idx_idempotency_completed_expires ON idempotency_keys (status, expires_at, id);
