package com.example.project.customer.service;

import com.example.project.customer.dto.CategorySpecificationRequest;
import com.example.project.customer.dto.CategorySpecificationResponse;
import com.example.project.customer.dto.CategorySpecificationUpdateRequest;
import com.example.project.customer.dto.SpecificationOptionRequest;
import com.example.project.customer.dto.SpecificationOptionResponse;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;

import java.util.List;

public interface SpecificationService {

    // Master Specifications
    SpecificationResponse create(SpecificationRequest request);

    List<SpecificationResponse> getAll(Boolean active, String search);

    SpecificationResponse getById(Integer id);

    SpecificationResponse update(Integer id, SpecificationRequest request);

    void delete(Integer id);

    // Specification Options
    SpecificationOptionResponse addOption(Integer specificationId, SpecificationOptionRequest request);

    List<SpecificationOptionResponse> getOptions(Integer specificationId);

    SpecificationOptionResponse updateOption(Integer specificationId, Integer optionId, SpecificationOptionRequest request);

    void deleteOption(Integer specificationId, Integer optionId);

    // Category Specification Mappings
    List<CategorySpecificationResponse> getCategorySpecifications(Integer categoryId, Boolean activeOnly);

    CategorySpecificationResponse addCategorySpecification(Integer categoryId, CategorySpecificationRequest request);

    CategorySpecificationResponse updateCategorySpecification(Integer categoryId, Integer specificationId, CategorySpecificationUpdateRequest request);

    void removeCategorySpecification(Integer categoryId, Integer specificationId);

    List<CategorySpecificationResponse> batchConfigureCategorySpecifications(Integer categoryId, List<CategorySpecificationRequest> requests);
}
