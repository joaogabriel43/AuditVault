package com.auditvault.infrastructure.batch;

import com.auditvault.domain.AuditEvent;
import org.springframework.batch.item.ItemProcessor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AuditEventItemProcessor implements ItemProcessor<AuditEvent, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    @Override
    public String process(AuditEvent event) {
        return String.format("[%s] [%s] User=%s | AggregateType=%s | Payload=%s",
                FORMATTER.format(event.getTimestamp()),
                event.getEventType(),
                event.getUserId() != null ? event.getUserId() : "N/A",
                event.getAggregateType() != null ? event.getAggregateType() : "N/A",
                event.getPayload() != null ? event.getPayload() : "{}");
    }
}
