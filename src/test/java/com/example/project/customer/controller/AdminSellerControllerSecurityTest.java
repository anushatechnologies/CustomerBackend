package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.SellerOnboardingService;
import com.example.project.customer.service.StoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSellerController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminSellerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SellerOnboardingService onboardingService;

    @MockBean
    private StoreService storeService;

    @Test
    @DisplayName("DELETE /api/admin/sellers/10 - Unauthenticated call is rejected with 401/403")
    void deleteSeller_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(delete("/api/admin/sellers/10"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(sc)
                            .withFailMessage("Expected 401 or 403 for unauthenticated delete seller, got: " + sc)
                            .isIn(401, 403);
                });
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("DELETE /api/admin/sellers/10 - Regular SELLER role is rejected with 403 Forbidden")
    void deleteSeller_SellerRole_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/sellers/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/sellers/10 - ADMIN role soft-deletes seller successfully")
    void deleteSeller_AdminRole_Success() throws Exception {
        Seller softDeletedSeller = Seller.builder()
                .sellerId(10)
                .name("Merchant")
                .email("merchant@hinchmart.com")
                .isDeleted(true)
                .verificationStatus(VerificationStatus.REJECTED)
                .build();

        when(onboardingService.softDeleteSeller(eq(10), any())).thenReturn(softDeletedSeller);

        mockMvc.perform(delete("/api/admin/sellers/10")
                        .param("reason", "Violated terms of service")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seller account and associated products soft-deleted successfully"))
                .andExpect(jsonPath("$.data.sellerId").value(10))
                .andExpect(jsonPath("$.data.isDeleted").value(true));

        verify(onboardingService).softDeleteSeller(eq(10), eq("Violated terms of service"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/sellers?includeDeleted=true - Passes includeDeleted flag to service")
    void getAllSellers_WithIncludeDeleted_Success() throws Exception {
        Seller activeSeller = Seller.builder().sellerId(1).name("Active").isDeleted(false).build();
        Seller deletedSeller = Seller.builder().sellerId(2).name("Deleted").isDeleted(true).build();

        when(onboardingService.getAllSellersForAdmin(any(), any(), eq(true)))
                .thenReturn(List.of(activeSeller, deletedSeller));

        mockMvc.perform(get("/api/admin/sellers")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(onboardingService).getAllSellersForAdmin(any(), any(), eq(true));
    }
}
