ALTER TABLE booking_cases
    ADD COLUMN tenant_refund_vnd DECIMAL(18,0) NULL,
    ADD COLUMN host_retained_vnd DECIMAL(18,0) NULL;
