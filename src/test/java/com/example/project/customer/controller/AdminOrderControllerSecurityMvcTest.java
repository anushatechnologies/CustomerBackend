package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminOrderController.class, OrderController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminOrderControllerSecurityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private UserContextUtil userContextUtil;

    @Test
    @DisplayName("GET /api/admin/orders - Unauthenticated request is rejected with 401 or 403")
    void getAllOrders_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/admin/orders - Customer is rejected with 403 Forbidden")
    void getAllOrders_CustomerRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/orders - Admin succeeds and returns all platform orders")
    void getAllOrders_AdminRole_Success() throws Exception {
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .orderId(101)
                .orderNumber("ORD-101")
                .customerId(50)
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .totalAmount(new BigDecimal("1500.00"))
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .itemCount(2)
                .createdAt(LocalDateTime.now())
                .build();

        PaginationMeta meta = PaginationMeta.of(1, 20, 1);
        ApiResponse<List<OrderSummaryResponse>> apiResponse = ApiResponse.paginated(
                "Orders retrieved successfully", List.of(summary), meta
        );

        when(orderService.getAllOrdersForAdmin(any(), anyInt(), anyInt())).thenReturn(apiResponse);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderId").value(101))
                .andExpect(jsonPath("$.data[0].customerName").value("Jane Doe"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(1500.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/orders - Admin user querying /api/orders receives all platform orders")
    void getOrders_AdminUser_ReturnsPlatformOrders() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(999);

        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .orderId(202)
                .orderNumber("ORD-202")
                .customerId(77)
                .customerName("John Smith")
                .totalAmount(new BigDecimal("2999.00"))
                .orderStatus("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        PaginationMeta meta = PaginationMeta.of(1, 20, 1);
        ApiResponse<List<OrderSummaryResponse>> apiResponse = ApiResponse.paginated(
                "Orders retrieved successfully", List.of(summary), meta
        );

        when(orderService.getOrders(any(), any(), anyInt(), anyInt())).thenReturn(apiResponse);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderId").value(202))
                .andExpect(jsonPath("$.data[0].customerName").value("John Smith"));
    }
}
