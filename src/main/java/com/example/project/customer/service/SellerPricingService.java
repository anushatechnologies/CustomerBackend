package com.example.project.customer.service;

import com.example.project.customer.dto.BulkPriceAdjustmentRequest;

import java.util.Map;

public interface SellerPricingService {

    Map<String, Object> bulkAdjustPricing(Integer sellerId, BulkPriceAdjustmentRequest request);
}
