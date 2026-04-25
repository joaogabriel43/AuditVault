package com.auditvault.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditSnapshotTest {

    @Test
    void shouldCreateAuditSnapshotSuccessfully() {
        String aggregateId = UUID.randomUUID().toString();
        String aggregateType = "User";
        String lastEventId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        String payload = "{\"name\":\"John Doe\"}";

        AuditSnapshot snapshot = AuditSnapshot.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .lastEventId(lastEventId)
                .snapshotTimestamp(timestamp)
                .statePayload(payload)
                .build();

        assertNotNull(snapshot.getId());
        assertEquals(aggregateId, snapshot.getAggregateId());
        assertEquals(aggregateType, snapshot.getAggregateType());
        assertEquals(lastEventId, snapshot.getLastEventId());
        assertEquals(timestamp, snapshot.getSnapshotTimestamp());
        assertEquals(payload, snapshot.getStatePayload());
    }

    @Test
    void shouldThrowExceptionWhenAggregateIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AuditSnapshot.builder()
                        .aggregateType("User")
                        .lastEventId(UUID.randomUUID().toString())
                        .snapshotTimestamp(Instant.now())
                        .build()
        );
        assertEquals("aggregateId cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLastEventIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AuditSnapshot.builder()
                        .aggregateId(UUID.randomUUID().toString())
                        .aggregateType("User")
                        .snapshotTimestamp(Instant.now())
                        .build()
        );
        assertEquals("lastEventId cannot be null or blank", exception.getMessage());
    }
}
