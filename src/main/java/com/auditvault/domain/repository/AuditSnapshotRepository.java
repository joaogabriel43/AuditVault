package com.auditvault.domain.repository;

import com.auditvault.domain.AuditSnapshot;

import java.time.Instant;
import java.util.Optional;

public interface AuditSnapshotRepository {
    AuditSnapshot save(AuditSnapshot auditSnapshot);
    Optional<AuditSnapshot> findLatestByAggregateIdBeforeTimestamp(String aggregateId, Instant timestamp);
}
