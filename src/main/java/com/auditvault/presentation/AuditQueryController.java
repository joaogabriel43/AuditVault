package com.auditvault.presentation;

import com.auditvault.application.dto.AuditEventDto;
import com.auditvault.application.service.AuditStateRebuilderService;
import com.auditvault.domain.AuditEvent;
import com.auditvault.domain.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Queries", description = "Endpoints for searching and retrieving audit events")
public class AuditQueryController {

    private final AuditEventRepository auditEventRepository;
    private final AuditStateRebuilderService auditStateRebuilderService;
    private final JobLauncher jobLauncher;
    private final Job auditExportJob;
    private final com.auditvault.infrastructure.elasticsearch.repository.ElasticsearchAuditRepository elasticsearchAuditRepository;

    @Operation(summary = "Get events by aggregate", description = "Retrieves paginated audit events for a specific aggregate ID")
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/events/{aggregateId}")
    public ResponseEntity<Page<AuditEventDto>> getEvents(
            @PathVariable String aggregateId,
            Pageable pageable) {
        Page<AuditEventDto> page = auditEventRepository
                .findByAggregateId(aggregateId, pageable)
                .map(this::toDto);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Search events", description = "Full-text search across all event payloads")
    @ApiResponse(responseCode = "200", description = "Successful search results")
    @GetMapping("/search")
    public ResponseEntity<Page<com.auditvault.infrastructure.elasticsearch.document.AuditDocument>> searchEvents(
            @RequestParam String query,
            Pageable pageable) {
        Page<com.auditvault.infrastructure.elasticsearch.document.AuditDocument> page = elasticsearchAuditRepository.findByPayloadContaining(query, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Reconstruct state", description = "Reconstructs the aggregate state at a specific point in time")
    @ApiResponse(responseCode = "200", description = "State reconstructed successfully")
    @GetMapping("/state/{aggregateId}")
    public ResponseEntity<String> getState(
            @PathVariable String aggregateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant targetTime) {
        Instant resolvedTime = targetTime != null ? targetTime : Instant.now();
        String state = auditStateRebuilderService.rebuildState(aggregateId, resolvedTime);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(state);
    }

    @Operation(summary = "Trigger PDF export", description = "Starts an asynchronous batch job to export events to a PDF")
    @ApiResponse(responseCode = "202", description = "Export job started")
    @PostMapping("/export/{aggregateId}")
    public ResponseEntity<Map<String, Object>> triggerExport(@PathVariable String aggregateId) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("aggregateId", aggregateId)
                    .addLong("startTime", System.currentTimeMillis())
                    .toJobParameters();

            var execution = jobLauncher.run(auditExportJob, jobParameters);
            return ResponseEntity.accepted().body(Map.of(
                    "jobExecutionId", execution.getId(),
                    "status", execution.getStatus().name(),
                    "aggregateId", aggregateId
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to start export job: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Download exported PDF", description = "Downloads the generated PDF report by job execution ID")
    @ApiResponse(responseCode = "200", description = "PDF file")
    @ApiResponse(responseCode = "404", description = "File not found")
    @GetMapping("/export/download/{jobExecutionId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadExport(@PathVariable Long jobExecutionId) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "audit_export_" + jobExecutionId + ".pdf");
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_export_" + jobExecutionId + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("Error accessing export file: " + e.getMessage(), e);
        }
    }

    private AuditEventDto toDto(AuditEvent event) {
        return new AuditEventDto(
                event.getId(),
                event.getAggregateId(),
                event.getAggregateType(),
                event.getEventType(),
                event.getTimestamp(),
                event.getUserId(),
                event.getPayload(),
                event.isObfuscated()
        );
    }
}
