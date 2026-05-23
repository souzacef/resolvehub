CREATE SEQUENCE ticket_number_seq START WITH 1001;

ALTER TABLE tickets ADD COLUMN ticket_number VARCHAR(32);
ALTER TABLE tickets ALTER COLUMN ticket_number SET DEFAULT ('RH-' || nextval('ticket_number_seq'));
UPDATE tickets SET ticket_number = DEFAULT WHERE ticket_number IS NULL;
ALTER TABLE tickets ALTER COLUMN ticket_number SET NOT NULL;
ALTER TABLE tickets ADD CONSTRAINT uq_tickets_ticket_number UNIQUE (ticket_number);
CREATE INDEX idx_tickets_ticket_number ON tickets (ticket_number);
