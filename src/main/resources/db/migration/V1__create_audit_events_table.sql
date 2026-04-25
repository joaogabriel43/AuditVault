CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255),
    event_type VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id VARCHAR(255),
    payload JSONB,
    obfuscated BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_audit_events_aggregate_id ON audit_events(aggregate_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
