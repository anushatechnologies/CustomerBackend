package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.VendorDashboardResponse;
import com.example.project.customer.dto.VendorPaymentsResponse;
import com.example.project.customer.dto.VendorPerformanceResponse;
import com.example.project.customer.service.VendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorDashboardController {

    private final VendorDashboardService vendorDashboardService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<VendorDashboardResponse>> getDashboard() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        VendorDashboardResponse dashboard = vendorDashboardService.getDashboard(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Vendor dashboard metrics retrieved successfully", dashboard));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<VendorPerformanceResponse>> getPerformance() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        VendorPerformanceResponse performance = vendorDashboardService.getPerformance(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Vendor performance metrics retrieved successfully", performance));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<VendorPaymentsResponse>> getPayments() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        VendorPaymentsResponse payments = vendorDashboardService.getPayments(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Vendor settlements and payments retrieved successfully", payments));
    }
}
