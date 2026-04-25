package com.auditvault.infrastructure.persistence.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "audit_snapshots")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSnapshotEntity {

    @Id
    private String id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "last_event_id", nullable = false)
    private String lastEventId;

    @Column(name = "snapshot_timestamp", nullable = false)
    private Instant snapshotTimestamp;

    @Type(JsonBinaryType.class)
    @Column(name = "state_payload", columnDefinition = "jsonb")
    private String statePayload;
}
