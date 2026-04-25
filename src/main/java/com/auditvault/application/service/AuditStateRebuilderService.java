package com.auditvault.application.service;

import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.AuditSnapshot;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.domain.repository.AuditSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditStateRebuilderService {

    private final AuditEventRepository eventRepository;
    private final AuditSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public String rebuildState(String aggregateId, Instant targetTime) {
        Optional<AuditSnapshot> latestSnapshotOpt = snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, targetTime);

        String baseStateJson = "{}";
        List<AuditEvent> eventsToApply;

        if (latestSnapshotOpt.isPresent()) {
            AuditSnapshot snapshot = latestSnapshotOpt.get();
            baseStateJson = snapshot.getStatePayload();
            // Events after the snapshot up to targetTime
            eventsToApply = eventRepository.findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(
                    aggregateId, snapshot.getSnapshotTimestamp(), targetTime);
        } else {
            // No snapshot, replay all events up to targetTime
            eventsToApply = eventRepository.findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(
                    aggregateId, targetTime);
        }

        return applyEventsToState(baseStateJson, eventsToApply);
    }

    private String applyEventsToState(String baseStateJson, List<AuditEvent> events) {
        if (events.isEmpty()) {
            return baseStateJson;
        }

        try {
            JsonNode currentState = objectMapper.readTree(baseStateJson);
            ObjectReader reader = objectMapper.readerForUpdating(currentState);

            for (AuditEvent event : events) {
                if (event.getPayload() != null && !event.getPayload().isBlank()) {
                    // Update current state with the event payload mutatively
                    currentState = reader.readValue(event.getPayload());
                }
            }

            return objectMapper.writeValueAsString(currentState);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to rebuild state due to JSON processing error", e);
        }
    }
}
