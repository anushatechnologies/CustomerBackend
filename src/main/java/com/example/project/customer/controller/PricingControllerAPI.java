package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BulkPriceAdjustmentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/pricing")
public class PricingControllerAPI {

    /**
     * POST /api/seller/pricing/bulk-adjust
     * Apply percentage/fixed price adjustments across Category or Brand
     */
    @PostMapping("/bulk-adjust")
    public ResponseEntity<ApiResponse<?>> bulkPriceAdjustment(
            @RequestBody BulkPriceAdjustmentRequest request) {
        // TODO: Implement bulk price adjustment logic
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Bulk price adjustment applied")
                .data(null)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
