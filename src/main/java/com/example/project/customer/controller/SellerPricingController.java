package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.BulkPriceAdjustmentRequest;
import com.example.project.customer.service.SellerPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/pricing")
@RequiredArgsConstructor
public class SellerPricingController {

    private final SellerPricingService sellerPricingService;
    private final SellerContextUtil sellerContextUtil;

    @PostMapping("/bulk-adjust")
    public ResponseEntity<Map<String, Object>> bulkAdjustPricing(@Valid @RequestBody BulkPriceAdjustmentRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Map<String, Object> result = sellerPricingService.bulkAdjustPricing(sellerId, request);
        return ResponseEntity.ok(result);
    }
}
