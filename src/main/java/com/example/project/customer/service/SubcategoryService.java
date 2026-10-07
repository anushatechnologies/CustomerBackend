package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;

import java.util.List;

public interface SubcategoryService {
    SubcategoryResponse create(SubcategoryRequest request);
    SubcategoryResponse getById(Integer id);
    List<SubcategoryResponse> getAll(Integer categoryId, Boolean active);
    List<SubcategoryResponse> getAll(Integer categoryId, Boolean active, Boolean visibleOnWebsite);
    List<SubcategoryResponse> getWebsiteSubcategories(Integer categoryId);
    SubcategoryResponse update(Integer id, SubcategoryRequest request);
    SubcategoryResponse updateWebsiteVisibility(Integer id, Boolean visibleOnWebsite);
    void delete(Integer id);
}