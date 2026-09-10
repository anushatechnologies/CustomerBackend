package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaymentOrderCreateRequest;
import com.example.project.customer.dto.PaymentOrderCreateResponse;
import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;
import com.example.project.customer.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserContextUtil userContextUtil;

    /**
     * Step 1: Create Razorpay Order
     * Generates a razorpay_order_id in paise to initialize frontend checkout modal.
     */
    @PostMapping("/create-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentOrderCreateResponse>> createPaymentOrder(
            @Valid @RequestBody PaymentOrderCreateRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        PaymentOrderCreateResponse response = paymentService.createPaymentOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Razorpay order created successfully", response));
    }

    /**
     * Step 2: Verify Razorpay Payment Signature & Status
     * Called by frontend immediately after successful checkout modal authorization.
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        PaymentStatusResponse response = paymentService.verifyPayment(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and order updated successfully", response));
    }

    /**
     * Step 3: Get Real-time Payment Status by Razorpay Payment ID (e.g. pay_xxx)
     */
    @GetMapping("/{paymentId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String paymentId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Payment status retrieved successfully", response));
    }

    /**
     * Step 4: Get Latest Payment Status for an Internal Order
     */
    @GetMapping("/order/{orderId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getOrderStatus(
            @PathVariable Integer orderId) {
        PaymentStatusResponse response = paymentService.getLatestOrderPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order payment status retrieved successfully", response));
    }

    /**
     * Step 5: Get All Payments for a Specific Customer
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.List<PaymentStatusResponse>>> getCustomerPayments(
            @PathVariable Integer customerId) {
        java.util.List<PaymentStatusResponse> list = paymentService.getCustomerPayments(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Customer payments retrieved successfully", list));
    }

    /**
     * Step 6: Razorpay Webhook Endpoint
     * Asynchronously receives and processes payment.captured and payment.failed
     * events.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed successfully");
    }
}
