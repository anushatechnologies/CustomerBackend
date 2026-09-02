package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.WarehouseRequest;
import com.example.project.customer.entity.Warehouse;
import com.example.project.customer.service.SellerWarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/warehouses")
@RequiredArgsConstructor
public class SellerWarehouseController {

    private final SellerWarehouseService warehouseService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Warehouse>>> listWarehouses() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        List<Warehouse> warehouses = warehouseService.getWarehouses(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Warehouses retrieved successfully", warehouses));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Warehouse created = warehouseService.createWarehouse(sellerId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Warehouse added successfully");
        response.put("data", created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
