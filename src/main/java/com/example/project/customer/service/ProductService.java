package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SearchSuggestionResponse;
import com.example.project.customer.dto.StockQuantityUpdateRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(Integer id);

    ApiResponse<List<ProductResponse>> getAll(
            Integer categoryId,
            Integer subcategoryId,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String brand,
            Boolean is24HourDelivery,
            String sort,
            int page,
            int limit
    );

    ProductResponse update(Integer id, ProductRequest request);

    ProductResponse updateStockQuantity(Integer id, StockQuantityUpdateRequest request);

    void delete(Integer id);

    // Product activation
    ProductResponse activate(Integer id);

    ProductResponse deactivate(Integer id);

    // Admin product management
    ProductListResponse getPending();

    ProductListResponse getAdminAll();

    ProductResponse getAdminById(Integer id);

    ProductResponse approve(Integer id);

    ProductResponse reject(
            Integer id,
            ProductRejectionRequest request
    );

    // Search
    SearchSuggestionResponse getSearchSuggestions(String query);
}
