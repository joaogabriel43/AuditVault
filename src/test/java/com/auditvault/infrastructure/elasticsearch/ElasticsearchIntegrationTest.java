package com.auditvault.infrastructure.elasticsearch;

import com.auditvault.infrastructure.elasticsearch.document.AuditDocument;
import com.auditvault.infrastructure.elasticsearch.repository.ElasticsearchAuditRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.auditvault.infrastructure.AbstractIntegrationTest;

public class ElasticsearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ElasticsearchAuditRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindAuditDocument() {
        AuditDocument doc = new AuditDocument(
                UUID.randomUUID().toString(),
                "agg-123",
                "Order",
                "ORDER_CREATED",
                Instant.now(),
                "user-1",
                "{\"product\": \"laptop\", \"price\": 1200}"
        );

        repository.save(doc);

        var page = repository.findByPayloadContaining("laptop", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAggregateId()).isEqualTo("agg-123");
    }
}
