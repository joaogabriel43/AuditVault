package com.auditvault.infrastructure.elasticsearch.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "audit_events")
public class AuditDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String aggregateId;

    @Field(type = FieldType.Keyword)
    private String aggregateType;

    @Field(type = FieldType.Keyword)
    private String eventType;

    @Field(type = FieldType.Date)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text)
    private String payload;

    public AuditDocument() {
    }

    public AuditDocument(String id, String aggregateId, String aggregateType, String eventType, Instant timestamp, String userId, String payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.userId = userId;
        this.payload = payload;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
