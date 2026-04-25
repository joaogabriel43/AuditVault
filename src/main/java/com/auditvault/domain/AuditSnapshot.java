package com.auditvault.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class AuditSnapshot {
    private final String id;
    private final String aggregateId;
    private final String aggregateType;
    private final String lastEventId;
    private final Instant snapshotTimestamp;
    private final String statePayload;

    @Builder
    public AuditSnapshot(String id, String aggregateId, String aggregateType, String lastEventId, 
                         Instant snapshotTimestamp, String statePayload) {
        
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("aggregateId cannot be null or blank");
        }
        if (lastEventId == null || lastEventId.trim().isEmpty()) {
            throw new IllegalArgumentException("lastEventId cannot be null or blank");
        }
        if (snapshotTimestamp == null) {
            throw new IllegalArgumentException("snapshotTimestamp cannot be null");
        }

        this.id = id != null ? id : UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.lastEventId = lastEventId;
        this.snapshotTimestamp = snapshotTimestamp;
        this.statePayload = statePayload;
    }
}
