package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StoreResponse;
import com.example.project.customer.dto.StoreStatusUpdateRequest;
import com.example.project.customer.dto.StoreUpdateRequest;

import com.example.project.customer.dto.CategoryResponse;

import java.util.List;

public interface StoreService {

    ApiResponse<List<StoreResponse>> getActiveStores(String search, int page, int limit);

    default ApiResponse<List<StoreResponse>> getActiveStores(int page, int limit) {
        return getActiveStores(null, page, limit);
    }

    StoreResponse getStoreBySlug(String slug);

    StoreResponse getStoreById(Integer storeId);

    StoreResponse getSellerStore(Integer sellerId);

    StoreResponse updateSellerStore(Integer sellerId, StoreUpdateRequest request);

    StoreResponse updateStoreStatusByAdmin(Integer storeId, StoreStatusUpdateRequest request);

    ApiResponse<List<ProductResponse>> getStoreProducts(String slugOrId, String search, Integer categoryId, Integer subcategoryId, int page, int limit, String sortBy);

    ApiResponse<List<CategoryResponse>> getStoreCategories(String slugOrId);
}
