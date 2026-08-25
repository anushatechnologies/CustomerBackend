package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SearchSuggestionsResponse;

import java.math.BigDecimal;

public interface ProductService {
    PagedResponse<ProductResponse> getProducts(Integer categoryId, Integer subcategoryId, String search,
                                               BigDecimal minPrice, BigDecimal maxPrice, String brand,
                                               Boolean is24HourDelivery, String sort, int page, int limit);
    ProductResponse getById(Integer id);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Integer id, ProductRequest request);
    void delete(Integer id);
    SearchSuggestionsResponse getSearchSuggestions(String query);
}