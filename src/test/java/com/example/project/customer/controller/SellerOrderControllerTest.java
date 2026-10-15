package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.SellerOrderDto;
import com.example.project.customer.dto.SellerOrderPageData;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.OrderService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerOrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "seller@example.com", roles = {"SELLER"})
class SellerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private SellerContextUtil sellerContextUtil;

    private SellerOrderDto mockOrderDto;

    @BeforeEach
    void setUp() {
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(31);

        mockOrderDto = SellerOrderDto.builder()
                .id(14)
                .orderNumber("ORD-20261014-F6E499")
                .createdAt("2026-10-14T06:15:30.000Z")
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .totalAmount(new BigDecimal("274.94"))
                .subtotal(new BigDecimal("233.00"))
                .gstAmount(new BigDecimal("41.94"))
                .paymentMethod("ONLINE")
                .buyer(SellerOrderDto.BuyerDto.builder()
                        .name("Supriya")
                        .phone("+919014397044")
                        .email("customer@gmail.com")
                        .company("Supriya Enterprises")
                        .build())
                .deliveryAddress(SellerOrderDto.DeliveryAddressDto.builder()
                        .completeAddress("Flat 402, Building A, Hyderabad")
                        .city("Hyderabad")
                        .state("Telangana")
                        .pincode("500081")
                        .build())
                .items(List.of(
                        SellerOrderDto.SellerOrderItemDto.builder()
                                .id(1)
                                .productId(1123)
                                .name("ssssssss")
                                .sku("sku3445")
                                .quantity(1)
                                .unit("Unit")
                                .price(new BigDecimal("233.00"))
                                .total(new BigDecimal("233.00"))
                                .imageUrl("https://hinchmart-storage.../image.jpg")
                                .sellerId(31)
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("GET /api/seller/orders returns 200 with matching schema and seller orders")
    void testGetSellerOrders_Success() throws Exception {
        SellerOrderPageData pageData = SellerOrderPageData.builder()
                .content(List.of(mockOrderDto))
                .totalElements(1)
                .totalPages(1)
                .page(1)
                .limit(20)
                .build();

        when(orderService.getSellerOrders(eq(31), any(), eq(1), eq(20))).thenReturn(pageData);

        mockMvc.perform(get("/api/seller/orders")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Seller orders retrieved successfully"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.limit").value(20))
                .andExpect(jsonPath("$.data.content[0].id").value(14))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-20261014-F6E499"))
                .andExpect(jsonPath("$.data.content[0].orderStatus").value("PLACED"))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].totalAmount").value(274.94))
                .andExpect(jsonPath("$.data.content[0].subtotal").value(233.00))
                .andExpect(jsonPath("$.data.content[0].gstAmount").value(41.94))
                .andExpect(jsonPath("$.data.content[0].buyer.name").value("Supriya"))
                .andExpect(jsonPath("$.data.content[0].buyer.phone").value("+919014397044"))
                .andExpect(jsonPath("$.data.content[0].buyer.company").value("Supriya Enterprises"))
                .andExpect(jsonPath("$.data.content[0].deliveryAddress.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.content[0].items[0].productId").value(1123))
                .andExpect(jsonPath("$.data.content[0].items[0].name").value("ssssssss"))
                .andExpect(jsonPath("$.data.content[0].items[0].sku").value("sku3445"))
                .andExpect(jsonPath("$.data.content[0].items[0].sellerId").value(31));
    }

    @Test
    @DisplayName("GET /api/seller/orders/{id} returns single order details for authorized seller")
    void testGetSellerOrderById_Success() throws Exception {
        when(orderService.getSellerOrderById(eq(14), eq(31))).thenReturn(mockOrderDto);

        mockMvc.perform(get("/api/seller/orders/14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Seller order retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(14))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20261014-F6E499"))
                .andExpect(jsonPath("$.data.items[0].sellerId").value(31));
    }

    @Test
    @DisplayName("PATCH /api/seller/orders/{id}/status updates status and returns updated order")
    void testPatchSellerOrderStatus_Success() throws Exception {
        mockOrderDto.setOrderStatus("PROCESSING");
        when(orderService.getSellerOrderById(eq(14), eq(31))).thenReturn(mockOrderDto);

        mockMvc.perform(patch("/api/seller/orders/14/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "PROCESSING",
                                "notes", "Order packed and ready for carrier pickup"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Order status updated successfully"))
                .andExpect(jsonPath("$.data.orderStatus").value("PROCESSING"));

        verify(orderService).updateOrderStatus(
                eq(14), eq("PROCESSING"), any(), eq("Order packed and ready for carrier pickup"),
                any(), any(), any(), any(), any()
        );
    }
}
