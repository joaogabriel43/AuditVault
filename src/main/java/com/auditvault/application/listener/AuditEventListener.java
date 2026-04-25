package com.auditvault.application.listener;

import com.auditvault.application.event.AuditPublishEvent;
import com.auditvault.application.service.SnapshotTriggerService;
import com.auditvault.application.service.SseNotificationService;
import com.auditvault.application.dto.AuditEventDto;
import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.infrastructure.elasticsearch.document.AuditDocument;
import com.auditvault.infrastructure.elasticsearch.repository.ElasticsearchAuditRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditEventRepository auditEventRepository;
    private final SnapshotTriggerService snapshotTriggerService;
    private final ElasticsearchAuditRepository elasticsearchAuditRepository;
    private final SseNotificationService sseNotificationService;

    @Async("auditTaskExecutor")
    @EventListener
    public void handleAuditPublishEvent(AuditPublishEvent event) {
        boolean isObfuscated = event.rawPayload() != null && event.rawPayload().contains("***");

        AuditEvent auditEvent = AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .aggregateId(event.aggregateId())
                .aggregateType(event.aggregateType())
                .eventType(event.eventType())
                .userId(event.userId())
                .timestamp(event.timestamp())
                .payload(event.rawPayload())
                .obfuscated(isObfuscated)
                .build();

        AuditEvent savedEvent = auditEventRepository.save(auditEvent);
        
        snapshotTriggerService.evaluateAndTriggerSnapshot(
                savedEvent.getAggregateId(), 
                savedEvent.getAggregateType(), 
                savedEvent.getId()
        );

        // Sync with Elasticsearch
        try {
            AuditDocument doc = new AuditDocument(
                    savedEvent.getId(),
                    savedEvent.getAggregateId(),
                    savedEvent.getAggregateType(),
                    savedEvent.getEventType(),
                    savedEvent.getTimestamp(),
                    savedEvent.getUserId(),
                    savedEvent.getPayload()
            );
            elasticsearchAuditRepository.save(doc);
        } catch (Exception e) {
            logger.warn("Failed to index audit event {} in Elasticsearch: {}", savedEvent.getId(), e.getMessage());
        }

        // Broadcast to SSE clients
        AuditEventDto dto = new AuditEventDto(
                savedEvent.getId(),
                savedEvent.getAggregateId(),
                savedEvent.getAggregateType(),
                savedEvent.getEventType(),
                savedEvent.getTimestamp(),
                savedEvent.getUserId(),
                savedEvent.getPayload(),
                savedEvent.isObfuscated()
        );
        sseNotificationService.broadcastEvent(dto);
    }
}
