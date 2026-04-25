package com.auditvault.application.dto;

import java.time.Instant;

public record AuditEventDto(
        String id,
        String aggregateId,
        String aggregateType,
        String eventType,
        Instant timestamp,
        String userId,
        String payload,
        boolean obfuscated
) {}
