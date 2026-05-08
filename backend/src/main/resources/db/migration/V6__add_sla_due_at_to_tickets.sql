ALTER TABLE tickets
    ADD COLUMN sla_due_at TIMESTAMPTZ;

UPDATE tickets
SET sla_due_at = CASE priority
    WHEN 'URGENT' THEN created_at + INTERVAL '4 hours'
    WHEN 'HIGH' THEN created_at + INTERVAL '8 hours'
    WHEN 'MEDIUM' THEN created_at + INTERVAL '24 hours'
    WHEN 'LOW' THEN created_at + INTERVAL '72 hours'
    ELSE created_at + INTERVAL '24 hours'
END
WHERE sla_due_at IS NULL;

ALTER TABLE tickets
    ALTER COLUMN sla_due_at SET NOT NULL;

CREATE INDEX idx_tickets_sla_due_at ON tickets (sla_due_at);
