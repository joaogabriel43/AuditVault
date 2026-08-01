package com.auditvault.application.service;

import com.auditvault.infrastructure.elasticsearch.document.AuditDocument;
import com.auditvault.infrastructure.elasticsearch.repository.ElasticsearchAuditRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ElasticsearchSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSyncService.class);
    private final ElasticsearchAuditRepository elasticsearchAuditRepository;

    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "syncFallback")
    @Retry(name = "elasticsearch")
    public void syncToElasticsearch(AuditDocument doc) {
        elasticsearchAuditRepository.save(doc);
    }

    public void syncFallback(AuditDocument doc, Throwable t) {
        // In a real production system, this would write to a Dead Letter Queue (DLQ) like Kafka or RabbitMQ.
        // For now, we log the failure, but the CircuitBreaker prevents cascading failures.
        logger.error("CircuitBreaker/Fallback triggered for Elasticsearch sync. Event ID: {}. Error: {}", doc.getId(), t.getMessage());
    }
}
