package com.auditvault.application.service;

import com.auditvault.domain.AuditSnapshot;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.domain.repository.AuditSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SnapshotTriggerService {

    private final AuditEventRepository eventRepository;
    private final AuditSnapshotRepository snapshotRepository;
    private final AuditStateRebuilderService stateRebuilderService;

    @Value("${auditvault.snapshot.threshold:50}")
    private int snapshotThreshold;

    @Async("auditTaskExecutor")
    public void evaluateAndTriggerSnapshot(String aggregateId, String aggregateType, String lastEventId) {
        Instant now = Instant.now();
        Optional<AuditSnapshot> lastSnapshot = snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, now);

        Instant lastSnapshotTime = lastSnapshot.map(AuditSnapshot::getSnapshotTimestamp).orElse(Instant.EPOCH);
        
        long eventsSinceSnapshot = eventRepository.countByAggregateIdAndTimestampGreaterThan(aggregateId, lastSnapshotTime);

        if (eventsSinceSnapshot >= snapshotThreshold) {
            String consolidatedState = stateRebuilderService.rebuildState(aggregateId, now);

            AuditSnapshot newSnapshot = AuditSnapshot.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .lastEventId(lastEventId)
                    .snapshotTimestamp(now)
                    .statePayload(consolidatedState)
                    .build();

            snapshotRepository.save(newSnapshot);
        }
    }
}
