package com.auditvault.infrastructure.integration;

import com.auditvault.application.annotation.Auditable;
import com.auditvault.application.security.UserContextResolver;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AuditAspectIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @org.springframework.test.context.DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public UserContextResolver testUserContextResolver() {
            return () -> "integration-test-user";
        }

        @Bean
        public DummyService dummyService() {
            return new DummyService();
        }
    }

    @Service
    static class DummyService {
        @Auditable(aggregateType = "User", eventType = "USER_REGISTERED")
        public String registerUser(String username, String password, String cpf) {
            return "User Registered";
        }
    }

    @Autowired
    private DummyService dummyService;

    @Autowired
    private SpringDataAuditEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldInterceptAndPersistAuditEventAsynchronously() {
        // Act - Call the auditable method
        dummyService.registerUser("john_doe", "secret123", "123.456.789-00");

        // Assert - Awaitility to wait for async processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, repository.count());
            
            var savedEvent = repository.findAll().get(0);
            assertEquals("User", savedEvent.getAggregateType());
            assertEquals("USER_REGISTERED", savedEvent.getEventType());
            assertEquals("integration-test-user", savedEvent.getUserId());
            assertTrue(savedEvent.isObfuscated());
            
            String payload = savedEvent.getPayload();
            assertTrue(payload.contains("john_doe"));
            assertTrue(payload.contains("***")); // Password should be masked
            assertTrue(payload.contains("User Registered")); // Result should be in payload
        });
    }
}
