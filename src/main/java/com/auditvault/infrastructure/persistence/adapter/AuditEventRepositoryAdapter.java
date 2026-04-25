package com.auditvault.infrastructure.persistence.adapter;

import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.infrastructure.persistence.entity.AuditEventEntity;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final SpringDataAuditEventRepository springDataRepository;

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        AuditEventEntity entity = AuditEventEntity.builder()
                .id(auditEvent.getId())
                .aggregateId(auditEvent.getAggregateId())
                .aggregateType(auditEvent.getAggregateType())
                .eventType(auditEvent.getEventType())
                .timestamp(auditEvent.getTimestamp())
                .userId(auditEvent.getUserId())
                .payload(auditEvent.getPayload())
                .obfuscated(auditEvent.isObfuscated())
                .build();

        return mapToDomain(springDataRepository.save(entity));
    }

    @Override
    public Page<AuditEvent> findByAggregateId(String aggregateId, Pageable pageable) {
        return springDataRepository.findByAggregateId(aggregateId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public List<AuditEvent> findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(String aggregateId, Instant start, Instant end) {
        return springDataRepository.findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(aggregateId, start, end)
                .stream().map(this::mapToDomain).toList();
    }

    @Override
    public List<AuditEvent> findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(String aggregateId, Instant end) {
        return springDataRepository.findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(aggregateId, end)
                .stream().map(this::mapToDomain).toList();
    }

    @Override
    public long countByAggregateIdAndTimestampGreaterThan(String aggregateId, Instant timestamp) {
        return springDataRepository.countByAggregateIdAndTimestampGreaterThan(aggregateId, timestamp);
    }

    private AuditEvent mapToDomain(AuditEventEntity e) {
        return AuditEvent.builder()
                .id(e.getId())
                .aggregateId(e.getAggregateId())
                .aggregateType(e.getAggregateType())
                .eventType(e.getEventType())
                .timestamp(e.getTimestamp())
                .userId(e.getUserId())
                .payload(e.getPayload())
                .obfuscated(e.isObfuscated())
                .build();
    }
}
