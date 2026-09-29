package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.DeliveryVerificationRequest;
import com.example.project.customer.dto.DeliveryVerificationResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.RiderOfferRespondRequest;
import com.example.project.customer.dto.RiderOfferResponse;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import com.example.project.customer.entity.DeliveryRider;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.DeliveryVerificationService;
import com.example.project.customer.service.RiderDispatchService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SellerOrderController.class, RiderController.class})
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

    @MockBean
    private RiderDispatchService riderDispatchService;

    @MockBean
    private DeliveryVerificationService deliveryVerificationService;

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

    @Test
    @DisplayName("Rider GET /api/rider/offers/active -> Returns active dispatch offer with coordinates and navigation URLs")
    void testRiderGetActiveOfferEndpoint() throws Exception {
        RiderOfferResponse offer = RiderOfferResponse.builder()
                .offerId(77L)
                .orderId(555)
                .sequenceIndex(1)
                .distanceKm(1.5)
                .offeredFare(BigDecimal.valueOf(58.00))
                .remainingSeconds(28L)
                .storeName("Balaji Cement Hub")
                .storeLatitude(17.440081)
                .storeLongitude(78.348915)
                .deliveryLocation("Plot 55, Gachibowli, Hyderabad")
                .deliveryLatitude(17.448294)
                .deliveryLongitude(78.355012)
                .pickupNavigationUrl("https://www.google.com/maps/dir/?api=1&destination=17.440081,78.348915")
                .deliveryNavigationUrl("https://www.google.com/maps/dir/?api=1&destination=17.448294,78.355012")
                .build();

        when(riderDispatchService.getActiveOfferForRider(eq(10L))).thenReturn(Optional.of(offer));

        mockMvc.perform(get("/api/rider/offers/active")
                        .header("X-Rider-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value(77))
                .andExpect(jsonPath("$.data.distanceKm").value(1.5))
                .andExpect(jsonPath("$.data.storeName").value("Balaji Cement Hub"))
                .andExpect(jsonPath("$.data.deliveryLatitude").value(17.448294))
                .andExpect(jsonPath("$.data.deliveryLongitude").value(78.355012))
                .andExpect(jsonPath("$.data.deliveryNavigationUrl").value("https://www.google.com/maps/dir/?api=1&destination=17.448294,78.355012"));
    }

    @Test
    @DisplayName("Rider GET /api/rider/orders/active -> Returns active assigned delivery order with navigation URLs")
    void testRiderGetActiveAssignedOrderEndpoint() throws Exception {
        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(555)
                .orderNumber("ORD-20260919-XYZ")
                .orderStatus("IN_TRANSIT")
                .storeName("Balaji Cement Hub")
                .storeLatitude(17.440081)
                .storeLongitude(78.348915)
                .deliveryLocation("Plot 55, Gachibowli, Hyderabad")
                .deliveryLatitude(17.448294)
                .deliveryLongitude(78.355012)
                .recipientName("Ramesh Gupta")
                .recipientPhone("9849012345")
                .pickupNavigationUrl("https://www.google.com/maps/dir/?api=1&destination=17.440081,78.348915")
                .deliveryNavigationUrl("https://www.google.com/maps/dir/?api=1&destination=17.448294,78.355012")
                .build();

        when(riderDispatchService.getActiveAssignedOrder(eq(10L))).thenReturn(Optional.of(orderResponse));

        mockMvc.perform(get("/api/rider/orders/active")
                        .header("X-Rider-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(555))
                .andExpect(jsonPath("$.data.orderStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.deliveryLatitude").value(17.448294))
                .andExpect(jsonPath("$.data.recipientName").value("Ramesh Gupta"))
                .andExpect(jsonPath("$.data.deliveryNavigationUrl").value("https://www.google.com/maps/dir/?api=1&destination=17.448294,78.355012"));
    }

    @Test
    @DisplayName("Rider GET /api/rider/orders/active -> Returns null data when no active order assigned")
    void testRiderGetActiveAssignedOrder_None() throws Exception {
        when(riderDispatchService.getActiveAssignedOrder(eq(10L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rider/orders/active")
                        .header("X-Rider-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Rider POST /api/rider/offers/{offerId}/respond -> Accept returns 200 and assigns rider")
    void testRiderRespondOfferEndpoint() throws Exception {
        when(riderDispatchService.respondToOffer(eq(10L), eq(77L), any(RiderOfferRespondRequest.class)))
                .thenReturn(true);

        RiderOfferRespondRequest req = RiderOfferRespondRequest.builder()
                .action("ACCEPT")
                .build();

        mockMvc.perform(post("/api/rider/offers/77/respond")
                        .header("X-Rider-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("Rider POST /api/rider/orders/{orderId}/pickup -> Returns 200 and order IN_TRANSIT")
    void testRiderConfirmPickupEndpoint() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(555)
                .orderStatus("IN_TRANSIT")
                .build();

        when(deliveryVerificationService.confirmPickup(eq(10L), eq(555))).thenReturn(response);

        mockMvc.perform(post("/api/rider/orders/555/pickup")
                        .header("X-Rider-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("IN_TRANSIT"));
    }

    @Test
    @DisplayName("Rider POST /api/rider/orders/{orderId}/verify-delivery -> Validates OTP & Payment, marks DELIVERED")
    void testRiderVerifyDeliveryEndpoint() throws Exception {
        DeliveryVerificationResponse response = DeliveryVerificationResponse.builder()
                .success(true)
                .message("Order delivered and verified successfully.")
                .orderId(555)
                .orderStatus("DELIVERED")
                .paymentStatus("PAID")
                .deliveredAt(LocalDateTime.now())
                .build();

        when(deliveryVerificationService.verifyAndCompleteDelivery(eq(10L), eq(555), any(DeliveryVerificationRequest.class)))
                .thenReturn(response);

        DeliveryVerificationRequest req = DeliveryVerificationRequest.builder()
                .otp("654321")
                .build();

        mockMvc.perform(post("/api/rider/orders/555/verify-delivery")
                        .header("X-Rider-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }
}
