package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class OrderControllerSecurityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private UserContextUtil userContextUtil;

    @Test
    @DisplayName("PATCH /api/orders/101/cancel - Unauthenticated request rejected with 401 or 403")
    void cancelOrder_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(patch("/api/orders/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"cancel request\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/orders/101/cancel - Customer cancelling own order succeeds")
    void cancelOrder_CustomerOwnOrder_Success() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(101)
                .orderNumber("ORD-101")
                .orderStatus("CANCELLED")
                .paymentStatus("REFUND_PENDING")
                .build();

        when(orderService.cancelOrder(eq(101), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/orders/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Home\",\"description\":\"Cancelled by user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/orders/202/cancel - IDOR attempt returns 403 Forbidden")
    void cancelOrder_IdorAttempt_Forbidden() throws Exception {
        when(orderService.cancelOrder(eq(202), any(), any()))
                .thenThrow(new ForbiddenException("Access denied: You can only cancel your own orders."));

        mockMvc.perform(patch("/api/orders/202/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Trying to cancel another user's order\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: You can only cancel your own orders."));
    }
}
