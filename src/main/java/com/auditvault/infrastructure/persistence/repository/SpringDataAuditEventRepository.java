package com.auditvault.infrastructure.persistence.repository;

import com.auditvault.infrastructure.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataAuditEventRepository extends JpaRepository<AuditEventEntity, String> {
    Page<AuditEventEntity> findByAggregateId(String aggregateId, Pageable pageable);
    List<AuditEventEntity> findByAggregateIdAndTimestampBetweenOrderByTimestampAsc(String aggregateId, Instant start, Instant end);
    List<AuditEventEntity> findByAggregateIdAndTimestampLessThanEqualOrderByTimestampAsc(String aggregateId, Instant end);
    long countByAggregateIdAndTimestampGreaterThan(String aggregateId, Instant timestamp);
}
