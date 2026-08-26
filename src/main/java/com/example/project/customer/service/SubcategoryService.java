package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;

import java.util.List;

public interface SubcategoryService {
    SubcategoryResponse create(SubcategoryRequest request);
    SubcategoryResponse getById(Integer id);
    List<SubcategoryResponse> getAll(Integer categoryId, Boolean active);
    SubcategoryResponse update(Integer id, SubcategoryRequest request);
    void delete(Integer id);
}