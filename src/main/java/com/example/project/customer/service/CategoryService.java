package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse getById(Integer id);
    List<CategoryResponse> getAll(Boolean active, Boolean includeSubcategories);
    ApiResponse<List<CategoryResponse>> getAll(Boolean active, Boolean includeSubcategories, int page, int limit);
    CategoryResponse update(Integer id, CategoryRequest request);
    void delete(Integer id);
}