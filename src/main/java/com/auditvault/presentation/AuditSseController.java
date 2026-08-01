package com.auditvault.presentation;

import com.auditvault.application.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Stream", description = "Endpoint for SSE (Server-Sent Events) live log streaming")
public class AuditSseController {

    private final SseNotificationService sseNotificationService;

    public AuditSseController(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @Operation(summary = "Stream live events", description = "Subscribes to an SSE stream for real-time audit event notifications")
    @ApiResponse(responseCode = "200", description = "Stream established")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseNotificationService.subscribe();
    }
}
