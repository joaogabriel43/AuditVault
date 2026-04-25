package com.auditvault.infrastructure.persistence;

import com.auditvault.domain.AuditEvent;
import com.auditvault.infrastructure.persistence.adapter.AuditEventRepositoryAdapter;
import com.auditvault.infrastructure.persistence.entity.AuditEventEntity;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditEventRepositoryAdapter.class) // Import the adapter into the context
class AuditEventRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // Flyway handles DDL
    }

    @Autowired
    private AuditEventRepositoryAdapter repositoryAdapter;

    @Autowired
    private SpringDataAuditEventRepository springDataRepository;

    @BeforeEach
    void setUp() {
        springDataRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveAuditEventWithJsonbPayload() {
        // Arrange
        String aggregateId = UUID.randomUUID().toString();
        String payloadJson = "{\"field1\": \"value1\", \"nested\": {\"key\": \"val\"}}";

        AuditEvent event = AuditEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("Order")
                .eventType("ORDER_PLACED")
                .timestamp(Instant.now())
                .userId("user123")
                .payload(payloadJson)
                .obfuscated(true)
                .build();

        // Act
        AuditEvent savedEvent = repositoryAdapter.save(event);

        // Assert
        assertNotNull(savedEvent.getId());

        Optional<AuditEventEntity> retrievedEntity = springDataRepository.findById(savedEvent.getId());
        assertTrue(retrievedEntity.isPresent());

        AuditEventEntity entity = retrievedEntity.get();
        assertEquals(aggregateId, entity.getAggregateId());
        assertEquals("Order", entity.getAggregateType());
        assertEquals("ORDER_PLACED", entity.getEventType());
        assertEquals("user123", entity.getUserId());
        assertTrue(entity.isObfuscated());
        
        // Assert JSONB was saved correctly
        assertEquals(payloadJson, entity.getPayload());
    }
}
