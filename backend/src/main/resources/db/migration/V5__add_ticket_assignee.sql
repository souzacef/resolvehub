ALTER TABLE tickets
    ADD COLUMN assignee_id UUID NULL REFERENCES users(id);

CREATE INDEX idx_tickets_assignee_id ON tickets (assignee_id);
