package com.auditvault.application.event;

import java.time.Instant;

public record AuditPublishEvent(
        String aggregateId,
        String aggregateType,
        String eventType,
        String userId,
        String rawPayload,
        Instant timestamp
) {}
