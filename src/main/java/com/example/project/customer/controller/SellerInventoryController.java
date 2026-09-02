package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.InventoryAdjustmentRequest;
import com.example.project.customer.service.SellerWarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/inventory")
@RequiredArgsConstructor
public class SellerInventoryController {

    private final SellerWarehouseService warehouseService;
    private final SellerContextUtil sellerContextUtil;

    @PostMapping("/adjust")
    public ResponseEntity<Map<String, Object>> adjustInventory(@Valid @RequestBody InventoryAdjustmentRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Map<String, Object> adjustmentData = warehouseService.adjustInventory(sellerId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Stock adjusted successfully");
        response.put("data", adjustmentData);

        return ResponseEntity.ok(response);
    }
}
