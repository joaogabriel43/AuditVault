package com.auditvault.infrastructure.persistence.adapter;

import com.auditvault.domain.AuditSnapshot;
import com.auditvault.domain.repository.AuditSnapshotRepository;
import com.auditvault.infrastructure.persistence.entity.AuditSnapshotEntity;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditSnapshotRepositoryAdapter implements AuditSnapshotRepository {

    private final SpringDataAuditSnapshotRepository springDataRepository;

    @Override
    public AuditSnapshot save(AuditSnapshot auditSnapshot) {
        AuditSnapshotEntity entity = AuditSnapshotEntity.builder()
                .id(auditSnapshot.getId())
                .aggregateId(auditSnapshot.getAggregateId())
                .aggregateType(auditSnapshot.getAggregateType())
                .lastEventId(auditSnapshot.getLastEventId())
                .snapshotTimestamp(auditSnapshot.getSnapshotTimestamp())
                .statePayload(auditSnapshot.getStatePayload())
                .build();

        AuditSnapshotEntity savedEntity = springDataRepository.save(entity);

        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<AuditSnapshot> findLatestByAggregateIdBeforeTimestamp(String aggregateId, Instant timestamp) {
        return springDataRepository.findFirstByAggregateIdAndSnapshotTimestampLessThanEqualOrderBySnapshotTimestampDesc(aggregateId, timestamp)
                .map(this::mapToDomain);
    }

    private AuditSnapshot mapToDomain(AuditSnapshotEntity entity) {
        return AuditSnapshot.builder()
                .id(entity.getId())
                .aggregateId(entity.getAggregateId())
                .aggregateType(entity.getAggregateType())
                .lastEventId(entity.getLastEventId())
                .snapshotTimestamp(entity.getSnapshotTimestamp())
                .statePayload(entity.getStatePayload())
                .build();
    }
}
