CREATE TABLE audit_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255),
    last_event_id VARCHAR(36) NOT NULL,
    snapshot_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    state_payload JSONB,
    CONSTRAINT uk_audit_snapshots_agg_event UNIQUE (aggregate_id, last_event_id)
);

CREATE INDEX idx_audit_snapshots_agg_time ON audit_snapshots(aggregate_id, snapshot_timestamp DESC);
