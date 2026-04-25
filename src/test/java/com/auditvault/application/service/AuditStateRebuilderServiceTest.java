package com.auditvault.application.service;

import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.AuditSnapshot;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.domain.repository.AuditSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditStateRebuilderServiceTest {

    @Mock
    private AuditEventRepository eventRepository;

    @Mock
    private AuditSnapshotRepository snapshotRepository;

    private AuditStateRebuilderService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AuditStateRebuilderService(eventRepository, snapshotRepository, objectMapper);
    }

    @Test
    void shouldRebuildStateFromZeroSnapshotsAndMultipleEvents() {
        // Arrange
        String aggregateId = "agg-123";
        Instant targetTime = Instant.now();

        when(snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, targetTime))
                .thenReturn(Optional.empty());

        AuditEvent event1 = AuditEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("User")
                .eventType("USER_CREATED")
                .timestamp(targetTime.minus(2, ChronoUnit.DAYS))
                .payload("{\"name\":\"John\", \"age\":30}")
                .build();

        AuditEvent event2 = AuditEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("User")
                .eventType("USER_UPDATED")
                .timestamp(targetTime.minus(1, ChronoUnit.DAYS))
                .payload("{\"age\":31, \"city\":\"NY\"}")
                .build();

        when(eventRepository.findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(aggregateId, targetTime))
                .thenReturn(Arrays.asList(event1, event2));

        // Act
        String finalState = service.rebuildState(aggregateId, targetTime);

        // Assert
        // We expect the final state to be {"name":"John","age":31,"city":"NY"}
        try {
            JsonNode resultNode = objectMapper.readTree(finalState);
            assertEquals("John", resultNode.get("name").asText());
            assertEquals(31, resultNode.get("age").asInt());
            assertEquals("NY", resultNode.get("city").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldRebuildStateFromSnapshotAndSubsequentEvents() {
        // Arrange
        String aggregateId = "agg-456";
        Instant targetTime = Instant.now();
        Instant snapshotTime = targetTime.minus(5, ChronoUnit.DAYS);

        AuditSnapshot snapshot = AuditSnapshot.builder()
                .aggregateId(aggregateId)
                .aggregateType("Order")
                .lastEventId(UUID.randomUUID().toString())
                .snapshotTimestamp(snapshotTime)
                .statePayload("{\"status\":\"PENDING\", \"total\":100}")
                .build();

        when(snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, targetTime))
                .thenReturn(Optional.of(snapshot));

        AuditEvent event1 = AuditEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("Order")
                .eventType("ORDER_PAID")
                .timestamp(targetTime.minus(2, ChronoUnit.DAYS))
                .payload("{\"status\":\"PAID\", \"paymentMethod\":\"CC\"}")
                .build();

        when(eventRepository.findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(aggregateId, snapshotTime, targetTime))
                .thenReturn(Collections.singletonList(event1));

        // Act
        String finalState = service.rebuildState(aggregateId, targetTime);

        // Assert
        // We expect the final state to be {"status":"PAID","total":100,"paymentMethod":"CC"}
        try {
            JsonNode resultNode = objectMapper.readTree(finalState);
            assertEquals("PAID", resultNode.get("status").asText());
            assertEquals(100, resultNode.get("total").asInt());
            assertEquals("CC", resultNode.get("paymentMethod").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
