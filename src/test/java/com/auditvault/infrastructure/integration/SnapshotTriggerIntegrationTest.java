package com.auditvault.infrastructure.integration;

import com.auditvault.application.service.SnapshotTriggerService;
import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.AuditSnapshot;
import com.auditvault.domain.repository.AuditEventRepository;
import com.auditvault.domain.repository.AuditSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "auditvault.snapshot.threshold=5" // Low threshold for testing
})
@Testcontainers
class SnapshotTriggerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public com.auditvault.application.security.UserContextResolver testUserContextResolver() {
            return () -> "snapshot-test-user";
        }
    }

    @Autowired
    private AuditEventRepository eventRepository;

    @Autowired
    private AuditSnapshotRepository snapshotRepository;

    @Autowired
    private SnapshotTriggerService snapshotTriggerService;

    @BeforeEach
    void setUp() {
        // We cannot easily delete all from repositories because we don't have a deleteAll method 
        // in our domain repository, but we can use unique aggregate IDs for tests.
    }

    @Test
    void shouldTriggerSnapshotWhenThresholdIsReached() {
        String aggregateId = "agg-trigger-test-" + UUID.randomUUID();
        
        // Insert 4 events
        for (int i = 1; i <= 4; i++) {
            AuditEvent event = AuditEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType("Order")
                    .eventType("ORDER_UPDATED")
                    .timestamp(Instant.now().minus(10 - i, ChronoUnit.MINUTES))
                    .userId("user1")
                    .payload("{\"status\":\"STATUS_" + i + "\"}")
                    .build();
            
            eventRepository.save(event);
            snapshotTriggerService.evaluateAndTriggerSnapshot(aggregateId, "Order", event.getId());
        }

        // Wait to ensure async execution finishes (if it was async, but we call it directly here for testing 
        // or through the listener. Wait, our evaluateAndTriggerSnapshot is @Async! 
        // We must wait for it to not trigger snapshot yet)
        
        await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
        
        Optional<AuditSnapshot> snapshotOpt = snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, Instant.now());
        assertTrue(snapshotOpt.isEmpty(), "Snapshot should not exist yet");

        // Insert 5th event (threshold is 5)
        AuditEvent event5 = AuditEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("Order")
                .eventType("ORDER_UPDATED")
                .timestamp(Instant.now())
                .userId("user1")
                .payload("{\"status\":\"STATUS_5\"}")
                .build();
        
        AuditEvent savedEvent5 = eventRepository.save(event5);
        snapshotTriggerService.evaluateAndTriggerSnapshot(aggregateId, "Order", savedEvent5.getId());

        // Await for the snapshot to be created asynchronously
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<AuditSnapshot> snapshot = snapshotRepository.findLatestByAggregateIdBeforeTimestamp(aggregateId, Instant.now());
            assertTrue(snapshot.isPresent(), "Snapshot should have been created");
            org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper mapper = new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper();
            assertEquals(
                    mapper.readTree("{\"status\":\"STATUS_5\"}"),
                    mapper.readTree(snapshot.get().getStatePayload())
            );
        });
    }
}
