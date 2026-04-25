package com.auditvault.infrastructure.persistence.repository;

import com.auditvault.infrastructure.persistence.entity.AuditSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SpringDataAuditSnapshotRepository extends JpaRepository<AuditSnapshotEntity, String> {
    Optional<AuditSnapshotEntity> findFirstByAggregateIdAndSnapshotTimestampLessThanEqualOrderBySnapshotTimestampDesc(String aggregateId, Instant timestamp);
}
