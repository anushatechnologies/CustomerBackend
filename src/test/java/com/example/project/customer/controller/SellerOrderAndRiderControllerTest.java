package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.SellerOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerOrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "seller@test.com", roles = {"SELLER", "ADMIN"})
public class SellerOrderAndRiderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SellerOrderService sellerOrderService;

    @MockBean
    private SellerContextUtil sellerContextUtil;

    @BeforeEach
    void setUp() {
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(1001);
    }

    @Test
    @DisplayName("Seller POST /api/seller/orders/{id}/accept -> Returns 200 and accepted order with Delivery OTP")
    void testSellerAcceptOrderEndpoint() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(555)
                .orderNumber("ORD-555")
                .orderStatus("ACCEPTED_BY_SELLER")
                .deliveryOtp("654321")
                .sellerAcceptedAt(LocalDateTime.now())
                .build();

        when(sellerOrderService.acceptOrder(eq(1001), eq(555), any())).thenReturn(response);

        mockMvc.perform(post("/api/seller/orders/555/accept")
                        .header("X-Seller-Id", 1001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(555))
                .andExpect(jsonPath("$.data.orderStatus").value("ACCEPTED_BY_SELLER"))
                .andExpect(jsonPath("$.data.deliveryOtp").value("654321"));
    }

    @Test
    @DisplayName("Seller POST /api/seller/orders/{id}/accept with Vehicle Type -> Returns 200 with selected vehicle type")
    void testSellerAcceptOrderWithVehicleTypeEndpoint() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(555)
                .orderNumber("ORD-555")
                .orderStatus("ACCEPTED_BY_SELLER")
                .selectedVehicleType("TATA_ACE")
                .deliveryOtp("654321")
                .sellerAcceptedAt(LocalDateTime.now())
                .build();

        when(sellerOrderService.acceptOrder(eq(1001), eq(555), any())).thenReturn(response);

        String json = "{\"vehicleTypeCode\":\"TATA_ACE\"}";

        mockMvc.perform(post("/api/seller/orders/555/accept")
                        .header("X-Seller-Id", 1001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(555))
                .andExpect(jsonPath("$.data.selectedVehicleType").value("TATA_ACE"))
                .andExpect(jsonPath("$.data.orderStatus").value("ACCEPTED_BY_SELLER"))
                .andExpect(jsonPath("$.data.deliveryOtp").value("654321"));
    }

    @Test
    @DisplayName("Seller POST /api/seller/orders/{id}/reject -> Returns 200 and rejected order")
    void testSellerRejectOrderEndpoint() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(555)
                .orderNumber("ORD-555")
                .orderStatus("REJECTED_BY_SELLER")
                .build();

        when(sellerOrderService.rejectOrder(eq(1001), eq(555), any(SellerOrderRejectRequest.class)))
                .thenReturn(response);

        SellerOrderRejectRequest req = SellerOrderRejectRequest.builder()
                .reason("Shop closed for inventory count")
                .build();

        mockMvc.perform(post("/api/seller/orders/555/reject")
                        .header("X-Seller-Id", 1001)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("REJECTED_BY_SELLER"));
    }
}
