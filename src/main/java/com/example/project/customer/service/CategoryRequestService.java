package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequestCreateRequest;
import com.example.project.customer.dto.CategoryRequestResponse;
import com.example.project.customer.entity.CategoryRequestStatus;

import java.util.List;

public interface CategoryRequestService {

    CategoryRequestResponse submitCategoryRequest(Integer sellerId, CategoryRequestCreateRequest request);

    List<CategoryRequestResponse> getSellerRequests(Integer sellerId);

    ApiResponse<List<CategoryRequestResponse>> getAdminRequests(CategoryRequestStatus status, int page, int limit);

    CategoryRequestResponse approveCategoryRequest(Integer requestId);

    CategoryRequestResponse rejectCategoryRequest(Integer requestId, String reason);
}
