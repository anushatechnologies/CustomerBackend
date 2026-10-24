package com.example.project.customer.controller;

import com.example.project.customer.dto.settlement.CommissionRuleCreateRequest;
import com.example.project.customer.dto.settlement.CommissionRuleResponse;
import com.example.project.customer.dto.settlement.CommissionRuleUpdateRequest;
import com.example.project.customer.service.CommissionRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AdminCommissionRuleControllerTest {

    @Mock
    private CommissionRuleService commissionRuleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CommissionRuleResponse sampleRule;

    @BeforeEach
    void setUp() {
        AdminCommissionRuleController controller = new AdminCommissionRuleController(commissionRuleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        sampleRule = CommissionRuleResponse.builder()
                .ruleId(1)
                .categoryId(null)
                .categoryName("Global (All Categories)")
                .rate(new BigDecimal("7.50"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .effectiveTo(null)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. GET /api/admin/commission-rules lists all rules")
    void testListRules() throws Exception {
        when(commissionRuleService.getRules(null, null)).thenReturn(List.of(sampleRule));

        mockMvc.perform(get("/api/admin/commission-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].ruleId").value(1))
                .andExpect(jsonPath("$.data[0].rate").value(7.50))
                .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    @DisplayName("2. GET /api/admin/commission-rules/{id} returns single rule")
    void testGetRuleById() throws Exception {
        when(commissionRuleService.getRuleById(1)).thenReturn(sampleRule);

        mockMvc.perform(get("/api/admin/commission-rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.ruleId").value(1))
                .andExpect(jsonPath("$.data.categoryName").value("Global (All Categories)"));
    }

    @Test
    @DisplayName("3. POST /api/admin/commission-rules creates new holding fee rule")
    void testCreateRule() throws Exception {
        CommissionRuleCreateRequest req = CommissionRuleCreateRequest.builder()
                .rate(new BigDecimal("8.00"))
                .isActive(true)
                .build();

        when(commissionRuleService.createRule(any())).thenReturn(sampleRule);

        mockMvc.perform(post("/api/admin/commission-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.ruleId").value(1));
    }

    @Test
    @DisplayName("4. PUT /api/admin/commission-rules/{id} updates rule")
    void testUpdateRule() throws Exception {
        CommissionRuleUpdateRequest req = CommissionRuleUpdateRequest.builder()
                .rate(new BigDecimal("10.00"))
                .isActive(true)
                .build();

        when(commissionRuleService.updateRule(eq(1), any())).thenReturn(sampleRule);

        mockMvc.perform(put("/api/admin/commission-rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.ruleId").value(1));
    }

    @Test
    @DisplayName("5. PATCH /api/admin/commission-rules/{id}/status toggles active status")
    void testToggleRuleStatus() throws Exception {
        when(commissionRuleService.toggleRuleStatus(eq(1), eq(false))).thenReturn(
                sampleRule.toBuilder().isActive(false).build()
        );

        mockMvc.perform(patch("/api/admin/commission-rules/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isActive", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("6. DELETE /api/admin/commission-rules/{id} deactivates rule")
    void testDeleteRule() throws Exception {
        mockMvc.perform(delete("/api/admin/commission-rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Commission rule deactivated successfully"));

        verify(commissionRuleService).deleteRule(1);
    }

    @Test
    @DisplayName("7. Also responds under /api/admin/holding-fees alias")
    void testHoldingFeesAlias() throws Exception {
        when(commissionRuleService.getRules(null, null)).thenReturn(List.of(sampleRule));

        mockMvc.perform(get("/api/admin/holding-fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].ruleId").value(1));
    }
}
