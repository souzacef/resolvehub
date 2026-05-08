CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    actor_id UUID NULL REFERENCES users(id),
    ticket_id UUID NULL REFERENCES tickets(id),
    action VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_org_created_at ON audit_logs (organization_id, created_at);
CREATE INDEX idx_audit_logs_ticket_created_at ON audit_logs (ticket_id, created_at);
CREATE INDEX idx_audit_logs_actor_created_at ON audit_logs (actor_id, created_at);
