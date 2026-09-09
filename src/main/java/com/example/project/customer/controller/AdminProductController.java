package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService service;
    private final com.example.project.customer.service.SellerProductService sellerProductService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductListResponse>> all() {
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved successfully", service.getAdminAll()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductListResponse>> pending() {
        return ResponseEntity.ok(ApiResponse.ok("Pending products retrieved successfully", service.getPending()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", service.getAdminById(id)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product approved successfully", service.approve(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> reject(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRejectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Product rejected", service.reject(id, request)));
    }

    @PostMapping("/sellers/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createForSeller(
            @PathVariable Integer sellerId,
            @Valid @RequestBody com.example.project.customer.dto.SellerProductCreateRequest request
    ) {
        ProductResponse created = sellerProductService.createSellerProduct(sellerId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.created("Product created successfully for seller", created));
    }

    @GetMapping("/sellers/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.example.project.customer.dto.SellerProductPageResponse> listForSeller(
            @PathVariable Integer sellerId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Object category,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Object brand,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String stockStatus,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "newest") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "12") int limit
    ) {
        com.example.project.customer.dto.SellerProductPageResponse response = sellerProductService.getSellerProducts(
                sellerId, search, category, brand, status, stockStatus, sortBy, page, limit
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sellers/{sellerId}/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateForSeller(
            @PathVariable Integer sellerId,
            @PathVariable Integer productId,
            @RequestBody com.example.project.customer.dto.SellerProductUpdateRequest request
    ) {
        ProductResponse updated = sellerProductService.updateSellerProduct(sellerId, productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully for seller", updated));
    }

    @DeleteMapping("/sellers/{sellerId}/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteForSeller(
            @PathVariable Integer sellerId,
            @PathVariable Integer productId
    ) {
        sellerProductService.deleteSellerProduct(sellerId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully for seller", null));
    }
}