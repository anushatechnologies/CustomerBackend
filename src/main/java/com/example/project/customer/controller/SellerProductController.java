package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerPricingUpdateRequest;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.dto.SellerStockUpdateRequest;
import com.example.project.customer.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping
    public ResponseEntity<SellerProductPageResponse> listSellerProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Object category,
            @RequestParam(required = false) Object brand,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "12") int limit
    ) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        SellerProductPageResponse response = sellerProductService.getSellerProducts(
                sellerId, search, category, brand, status, stockStatus, sortBy, page, limit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getSellerProductById(@PathVariable String id) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Integer productId = parseProductId(id);
        ProductResponse response = sellerProductService.getSellerProductById(sellerId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", response));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSellerProduct(@Valid @RequestBody SellerProductCreateRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        ProductResponse created = sellerProductService.createSellerProduct(sellerId, request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "sp_" + created.getProductId());
        data.put("productId", "sp_" + created.getProductId());
        data.put("sellerId", "seller_" + sellerId);
        data.put("title", created.getTitle());
        data.put("sku", created.getSku());
        data.put("status", created.getStatus());
        data.put("createdAt", created.getCreatedAt());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Product submitted successfully for admin review.");
        response.put("data", data);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSellerProduct(
            @PathVariable String id,
            @RequestBody SellerProductUpdateRequest request
    ) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Integer productId = parseProductId(id);
        ProductResponse updated = sellerProductService.updateSellerProduct(sellerId, productId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Product updated successfully");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateSellerStock(
            @PathVariable String id,
            @Valid @RequestBody SellerStockUpdateRequest request
    ) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Integer productId = parseProductId(id);
        ProductResponse updated = sellerProductService.updateSellerStock(sellerId, productId, request.getStockQty());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "sp_" + updated.getProductId());
        data.put("stockQty", updated.getStockQty());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Stock updated successfully");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pricing")
    public ResponseEntity<Map<String, Object>> updateSellerPricing(
            @PathVariable String id,
            @RequestBody SellerPricingUpdateRequest request
    ) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Integer productId = parseProductId(id);
        sellerProductService.updateSellerPricing(sellerId, productId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Product pricing updated successfully");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSellerProduct(@PathVariable String id) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Integer productId = parseProductId(id);
        sellerProductService.deleteSellerProduct(sellerId, productId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Product removed from inventory");

        return ResponseEntity.ok(response);
    }

    private Integer parseProductId(String id) {
        if (id == null) return null;
        if (id.startsWith("sp_")) {
            return Integer.parseInt(id.substring(3));
        }
        return Integer.parseInt(id);
    }
}
