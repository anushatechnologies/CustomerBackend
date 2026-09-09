package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StoreResponse;
import com.example.project.customer.dto.StoreStatusUpdateRequest;
import com.example.project.customer.dto.StoreUpdateRequest;
import com.example.project.customer.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final SellerContextUtil sellerContextUtil;

    // -------------------------------------------------------------------------
    // Public Buyer-Facing Discovery Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getActiveStores(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(storeService.getActiveStores(page, limit));
    }

    @GetMapping("/stores/{slugOrId}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreLandingPage(@PathVariable String slugOrId) {
        StoreResponse response;
        try {
            int storeId = Integer.parseInt(slugOrId);
            response = storeService.getStoreById(storeId);
        } catch (NumberFormatException e) {
            response = storeService.getStoreBySlug(slugOrId);
        }
        return ResponseEntity.ok(ApiResponse.ok("Store details retrieved successfully", response));
    }

    @GetMapping("/stores/{slugOrId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getStoreProducts(
            @PathVariable String slugOrId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "featured") String sortBy) {
        return ResponseEntity.ok(storeService.getStoreProducts(slugOrId, search, categoryId, page, limit, sortBy));
    }

    // -------------------------------------------------------------------------
    // Seller-Facing Store Management Endpoints (Scoped by Authenticated JWT)
    // -------------------------------------------------------------------------

    @GetMapping("/seller/store")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreResponse>> getOwnStore() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        StoreResponse response = storeService.getSellerStore(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller store profile retrieved successfully", response));
    }

    @PutMapping("/seller/store")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreResponse>> updateOwnStore(@Valid @RequestBody StoreUpdateRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        StoreResponse response = storeService.updateSellerStore(sellerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Store profile updated successfully", response));
    }

    // -------------------------------------------------------------------------
    // Admin-Facing Store Status Management
    // -------------------------------------------------------------------------

    @PatchMapping("/admin/stores/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStoreStatus(
            @PathVariable Integer id,
            @Valid @RequestBody StoreStatusUpdateRequest request) {
        StoreResponse response = storeService.updateStoreStatusByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Store status updated successfully", response));
    }
}
