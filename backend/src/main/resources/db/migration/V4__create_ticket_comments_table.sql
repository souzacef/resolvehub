CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    author_id UUID NOT NULL REFERENCES users(id),
    body VARCHAR(3000) NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments (ticket_id);
CREATE INDEX idx_ticket_comments_author_id ON ticket_comments (author_id);
CREATE INDEX idx_ticket_comments_ticket_created_at ON ticket_comments (ticket_id, created_at);
