package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> all() {
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved successfully", service.getAdminAll()));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<ProductListResponse>> pending() {
        return ResponseEntity.ok(ApiResponse.ok("Pending products retrieved successfully", service.getPending()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", service.getAdminById(id)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponse>> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product approved successfully", service.approve(id)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponse>> reject(@PathVariable Integer id,
                                                                @Valid @RequestBody ProductRejectionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product rejected", service.reject(id, request)));
    }
}