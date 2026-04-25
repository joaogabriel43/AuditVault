package com.auditvault.infrastructure.batch;

import com.auditvault.application.security.UserContextResolver;
import com.auditvault.infrastructure.persistence.entity.AuditEventEntity;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AuditExportJobIntegrationTest {

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
        registry.add("spring.batch.jdbc.initialize-schema", () -> "always");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public UserContextResolver testUserContextResolver() {
            return () -> "batch-test-user";
        }
    }

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job auditExportJob;

    @Autowired
    private SpringDataAuditEventRepository springDataAuditEventRepository;

    private String pdfOutputPath;

    @AfterEach
    void cleanUp() {
        if (pdfOutputPath != null) {
            new File(pdfOutputPath).delete();
        }
        springDataAuditEventRepository.deleteAll();
    }

    @Test
    void shouldRunBatchJobAndCreatePdfFile() throws Exception {
        // Arrange: insert some events
        for (int i = 1; i <= 3; i++) {
            AuditEventEntity entity = AuditEventEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateId("agg-batch-test")
                    .aggregateType("Order")
                    .eventType("ORDER_EVENT_" + i)
                    .timestamp(Instant.now())
                    .userId("user" + i)
                    .payload("{\"step\":" + i + "}")
                    .obfuscated(false)
                    .build();
            springDataAuditEventRepository.save(entity);
        }

        pdfOutputPath = System.getProperty("java.io.tmpdir") + "/auditvault-export-test-" + UUID.randomUUID() + ".pdf";

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("aggregateId", "agg-batch-test")
                .addString("outputPath", pdfOutputPath)
                .addLong("startTime", System.currentTimeMillis())
                .toJobParameters();

        // Act
        var execution = jobLauncher.run(auditExportJob, jobParameters);

        // Assert
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        pdfOutputPath = System.getProperty("java.io.tmpdir") + "/audit_export_" + execution.getId() + ".pdf";

        File defaultOutput = new File(pdfOutputPath);
        assertTrue(defaultOutput.exists(), "PDF file should have been created");
        assertTrue(defaultOutput.length() > 0, "PDF file should not be empty");
    }
}
