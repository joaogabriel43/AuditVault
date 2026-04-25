package com.auditvault.domain.repository;

import com.auditvault.domain.AuditEvent;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditEventRepository {
    AuditEvent save(AuditEvent auditEvent);
    Page<AuditEvent> findByAggregateId(String aggregateId, Pageable pageable);
    List<AuditEvent> findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(String aggregateId, Instant start, Instant end);
    List<AuditEvent> findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(String aggregateId, Instant end);
    long countByAggregateIdAndTimestampGreaterThan(String aggregateId, Instant timestamp);
}
