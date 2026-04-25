package com.auditvault.application.aspect;

import com.auditvault.application.annotation.Auditable;
import com.auditvault.application.event.AuditPublishEvent;
import com.auditvault.application.security.DataMaskingService;
import com.auditvault.application.security.UserContextResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final UserContextResolver userContextResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final DataMaskingService dataMaskingService;

    @Setter // For testing purposes
    private ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            publishAuditEvent(joinPoint, auditable, result);
        } catch (Exception e) {
            // Log error but do not interrupt the business flow
            System.err.println("Failed to publish audit event: " + e.getMessage());
        }

        return result;
    }

    private void publishAuditEvent(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) throws JsonProcessingException {
        String userId = userContextResolver.getCurrentUserId();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> payloadData = new HashMap<>();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                payloadData.put(parameterNames[i], args[i]);
            }
        } else {
            payloadData.put("args", args);
        }
        
        payloadData.put("result", result);

        String rawPayload = objectMapper.writeValueAsString(payloadData);
        
        // We defer masking to the async listener to avoid blocking, or do it here?
        // ADR-005: Mascaramento obrigatório de PII no payload ANTES de publicar ou ANTES de persistir.
        // Doing it before publish might be safer, but doing it in listener is more performant for the main thread.
        // Let's defer masking to the async listener to keep the interceptor fast!
        // Wait, the prompt says: "Mascarar os dados usando DataMaskingService" in the AuditAspect.
        // Let's do it here then as requested.
        
        DataMaskingService.MaskingResult maskingResult = dataMaskingService.maskJson(rawPayload);
        String finalPayload = maskingResult.getMaskedPayload();

        // Assume aggregateId could be retrieved from result if it's an entity, 
        // for now we generate a random one if we can't find one. 
        // In a real scenario, we'd extract it from the returned object or arguments.
        String aggregateId = UUID.randomUUID().toString();

        AuditPublishEvent event = new AuditPublishEvent(
                aggregateId,
                auditable.aggregateType(),
                auditable.eventType(),
                userId,
                finalPayload,
                Instant.now()
        );

        eventPublisher.publishEvent(event);
    }
}
