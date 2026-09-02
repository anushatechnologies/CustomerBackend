package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerPricingUpdateRequest;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerProductUpdateRequest;

public interface SellerProductService {

    SellerProductPageResponse getSellerProducts(
            Integer sellerId,
            String search,
            Object category,
            Object brand,
            String status,
            String stockStatus,
            String sortBy,
            int page,
            int limit
    );

    ProductResponse getSellerProductById(Integer sellerId, Integer productId);

    ProductResponse createSellerProduct(Integer sellerId, SellerProductCreateRequest request);

    ProductResponse updateSellerProduct(Integer sellerId, Integer productId, SellerProductUpdateRequest request);

    ProductResponse updateSellerStock(Integer sellerId, Integer productId, Integer stockQty);

    ProductResponse updateSellerPricing(Integer sellerId, Integer productId, SellerPricingUpdateRequest request);

    void deleteSellerProduct(Integer sellerId, Integer productId);
}
