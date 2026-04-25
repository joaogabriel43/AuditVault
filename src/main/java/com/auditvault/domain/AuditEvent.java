package com.auditvault.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class AuditEvent {
    private final String id;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final Instant timestamp;
    private final String userId;
    private final String payload;
    private final boolean obfuscated;

    @Builder
    public AuditEvent(String id, String aggregateId, String aggregateType, String eventType, 
                      Instant timestamp, String userId, String payload, Boolean obfuscated) {
        
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("aggregateId cannot be null or blank");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("eventType cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }

        this.id = id != null ? id : UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.userId = userId;
        this.payload = payload;
        this.obfuscated = obfuscated != null ? obfuscated : false;
    }
}
