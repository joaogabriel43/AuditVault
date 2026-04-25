package com.auditvault.presentation;

import com.auditvault.application.dto.AuditEventDto;
import com.auditvault.application.security.UserContextResolver;
import com.auditvault.application.service.AuditStateRebuilderService;
import com.auditvault.domain.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditQueryController.class)
class AuditQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventRepository auditEventRepository;

    @MockBean
    private AuditStateRebuilderService auditStateRebuilderService;

    @MockBean
    private UserContextResolver userContextResolver;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private Job auditExportJob;

    @MockBean
    private com.auditvault.infrastructure.elasticsearch.repository.ElasticsearchAuditRepository elasticsearchAuditRepository;

    @Test
    void shouldReturnPagedAuditEvents() throws Exception {
        String aggregateId = UUID.randomUUID().toString();
        var dto = new AuditEventDto(
                UUID.randomUUID().toString(), aggregateId, "Order",
                "ORDER_PLACED", Instant.now(), "user1", "{}", false
        );

        when(auditEventRepository.findByAggregateId(eq(aggregateId), any()))
                .thenReturn(new PageImpl<>(
                        List.of(com.auditvault.domain.AuditEvent.builder()
                                .id(dto.id()).aggregateId(aggregateId)
                                .aggregateType("Order").eventType("ORDER_PLACED")
                                .timestamp(dto.timestamp()).userId("user1")
                                .payload("{}").build()),
                        PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/audit/events/{aggregateId}", aggregateId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].aggregateId").value(aggregateId))
                .andExpect(jsonPath("$.content[0].eventType").value("ORDER_PLACED"));
    }

    @Test
    void shouldReturnRebuiltState() throws Exception {
        String aggregateId = UUID.randomUUID().toString();
        String expectedState = "{\"status\":\"ACTIVE\",\"name\":\"John\"}";

        when(auditStateRebuilderService.rebuildState(eq(aggregateId), any()))
                .thenReturn(expectedState);

        mockMvc.perform(get("/api/audit/state/{aggregateId}", aggregateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedState));
    }
}
