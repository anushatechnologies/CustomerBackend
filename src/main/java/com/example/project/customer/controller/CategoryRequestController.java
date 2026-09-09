package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequestCreateRequest;
import com.example.project.customer.dto.CategoryRequestResponse;
import com.example.project.customer.dto.RejectCategoryRequest;
import com.example.project.customer.entity.CategoryRequestStatus;
import com.example.project.customer.service.CategoryRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryRequestController {

    private final CategoryRequestService categoryRequestService;
    private final SellerContextUtil sellerContextUtil;

    // -------------------------------------------------------------------------
    // Seller-Facing Category Request Endpoints
    // -------------------------------------------------------------------------

    @PostMapping("/seller/category-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CategoryRequestResponse>> submitCategoryRequest(
            @Valid @RequestBody CategoryRequestCreateRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        CategoryRequestResponse response = categoryRequestService.submitCategoryRequest(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Category proposal submitted for admin review", response));
    }

    @GetMapping("/seller/category-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CategoryRequestResponse>>> getSellerCategoryRequests() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        List<CategoryRequestResponse> list = categoryRequestService.getSellerRequests(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller category requests retrieved successfully", list));
    }

    // -------------------------------------------------------------------------
    // Admin-Facing Category Request Approval / Rejection Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/admin/category-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryRequestResponse>>> getAdminCategoryRequests(
            @RequestParam(required = false) CategoryRequestStatus status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(categoryRequestService.getAdminRequests(status, page, limit));
    }

    @PostMapping("/admin/category-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryRequestResponse>> approveCategoryRequest(@PathVariable Integer id) {
        CategoryRequestResponse response = categoryRequestService.approveCategoryRequest(id);
        return ResponseEntity.ok(ApiResponse.ok("Category request approved and added to global catalog", response));
    }

    @PostMapping("/admin/category-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryRequestResponse>> rejectCategoryRequest(
            @PathVariable Integer id,
            @Valid @RequestBody RejectCategoryRequest request) {
        CategoryRequestResponse response = categoryRequestService.rejectCategoryRequest(id, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Category request rejected", response));
    }
}
