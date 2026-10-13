package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentRefundController {

    private final PaymentService paymentService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> getPendingRefunds() {
        List<PaymentStatusResponse> pending = paymentService.getPendingRefunds();
        return ResponseEntity.ok(ApiResponse.ok("Pending refunds retrieved successfully", pending));
    }

    @PostMapping("/retry/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> retryPendingRefund(
            @PathVariable Integer paymentId) {
        PaymentStatusResponse result = paymentService.retryPendingRefund(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Refund retry completed", result));
    }

    @PostMapping("/reconcile-stale")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reconcileStaleRefunds(
            @RequestParam(defaultValue = "15") int olderThanMinutes) {
        int reconciled = paymentService.reconcileStaleRefunds(olderThanMinutes);
        return ResponseEntity.ok(ApiResponse.ok("Stale refunds reconciliation completed", Map.of(
                "reconciledCount", reconciled,
                "olderThanMinutes", olderThanMinutes
        )));
    }
}
