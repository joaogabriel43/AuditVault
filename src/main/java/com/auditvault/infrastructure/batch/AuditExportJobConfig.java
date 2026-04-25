package com.auditvault.infrastructure.batch;

import com.auditvault.domain.AuditEvent;
import com.auditvault.infrastructure.persistence.entity.AuditEventEntity;
import com.auditvault.infrastructure.persistence.repository.SpringDataAuditEventRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class AuditExportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SpringDataAuditEventRepository auditEventJpaRepository;

    public AuditExportJobConfig(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                SpringDataAuditEventRepository auditEventJpaRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.auditEventJpaRepository = auditEventJpaRepository;
    }

    @Bean
    public Job auditExportJob(Step auditExportStep) {
        return new JobBuilder("auditExportJob", jobRepository)
                .start(auditExportStep)
                .build();
    }

    @Bean
    public Step auditExportStep() {
        return new StepBuilder("auditExportStep", jobRepository)
                .<AuditEventEntity, String>chunk(100, transactionManager)
                .reader(auditEventReader())
                .processor(new AuditEventItemProcessorEntity())
                .writer(auditPdfItemWriter(null)) // Null is injected via StepScope late binding
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public AuditPdfItemWriterEntity auditPdfItemWriter(
            @org.springframework.beans.factory.annotation.Value("#{stepExecution.jobExecution.id}") Long jobExecutionId) {
        String outputPath = System.getProperty("java.io.tmpdir") + "/audit_export_" + jobExecutionId + ".pdf";
        return new AuditPdfItemWriterEntity(outputPath);
    }

    @Bean
    public RepositoryItemReader<AuditEventEntity> auditEventReader() {
        return new RepositoryItemReaderBuilder<AuditEventEntity>()
                .name("auditEventReader")
                .repository(auditEventJpaRepository)
                .methodName("findAll")
                .pageSize(100)
                .sorts(Map.of("timestamp", Sort.Direction.ASC))
                .build();
    }
}
