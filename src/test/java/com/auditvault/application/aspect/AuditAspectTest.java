package com.auditvault.application.aspect;

import com.auditvault.application.annotation.Auditable;
import com.auditvault.application.event.AuditPublishEvent;
import com.auditvault.application.security.DataMaskingService;
import com.auditvault.application.security.UserContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private UserContextResolver userContextResolver;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DataMaskingService dataMaskingService;

    @InjectMocks
    private AuditAspect auditAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        auditAspect.setObjectMapper(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void shouldPublishAuditPublishEventWhenMethodIsCalled() throws Throwable {
        // Arrange
        String userId = "user-123";
        when(userContextResolver.getCurrentUserId()).thenReturn(userId);

        Method method = DummyClass.class.getMethod("dummyMethod", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"input"});
        
        Object[] args = {"arg1"};
        when(joinPoint.getArgs()).thenReturn(args);
        
        Object returnValue = "result";
        when(joinPoint.proceed()).thenReturn(returnValue);
        
        when(dataMaskingService.maskJson(anyString())).thenAnswer(invocation -> 
            new DataMaskingService.MaskingResult(invocation.getArgument(0), false)
        );

        Auditable auditable = method.getAnnotation(Auditable.class);

        // Act
        Object result = auditAspect.auditMethod(joinPoint, auditable);

        // Assert
        assertEquals(returnValue, result);

        ArgumentCaptor<AuditPublishEvent> eventCaptor = ArgumentCaptor.forClass(AuditPublishEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        AuditPublishEvent event = eventCaptor.getValue();
        assertEquals("DummyType", event.aggregateType());
        assertEquals("DUMMY_CREATED", event.eventType());
        assertEquals(userId, event.userId());
        assertNotNull(event.timestamp());
        
        // Payload should be JSON string containing args and return
        assertNotNull(event.rawPayload());
        assertTrue(event.rawPayload().contains("arg1"));
        assertTrue(event.rawPayload().contains("result"));
    }

    static class DummyClass {
        @Auditable(aggregateType = "DummyType", eventType = "DUMMY_CREATED")
        public String dummyMethod(String input) {
            return "result";
        }
    }
}
