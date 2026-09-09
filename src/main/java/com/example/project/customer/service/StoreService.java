package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StoreResponse;
import com.example.project.customer.dto.StoreStatusUpdateRequest;
import com.example.project.customer.dto.StoreUpdateRequest;

import java.util.List;

public interface StoreService {

    ApiResponse<List<StoreResponse>> getActiveStores(int page, int limit);

    StoreResponse getStoreBySlug(String slug);

    StoreResponse getStoreById(Integer storeId);

    StoreResponse getSellerStore(Integer sellerId);

    StoreResponse updateSellerStore(Integer sellerId, StoreUpdateRequest request);

    StoreResponse updateStoreStatusByAdmin(Integer storeId, StoreStatusUpdateRequest request);

    ApiResponse<List<ProductResponse>> getStoreProducts(String slugOrId, String search, Integer categoryId, int page, int limit, String sortBy);
}
