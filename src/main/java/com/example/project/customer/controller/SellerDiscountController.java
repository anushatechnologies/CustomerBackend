package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.DiscountRejectionRequest;
import com.example.project.customer.dto.SellerDiscountRequest;
import com.example.project.customer.dto.SellerDiscountResponse;
import com.example.project.customer.service.SellerDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SellerDiscountController {

    private final SellerDiscountService service;

    @PostMapping("/seller/discounts")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> create(@Valid @RequestBody SellerDiscountRequest request) {
        Integer sellerId = 101;
        SellerDiscountResponse response = service.create(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Discount created successfully", response));
    }

    @GetMapping("/seller/discounts")
    public ResponseEntity<ApiResponse<List<SellerDiscountResponse>>> getBySeller() {
        return ResponseEntity.ok(ApiResponse.ok("Seller discounts retrieved successfully", service.getBySeller(101)));
    }

    @GetMapping("/seller/discounts/{discountId}")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> getById(@PathVariable Integer discountId) {
        return ResponseEntity.ok(ApiResponse.ok("Discount retrieved successfully", service.getById(101, discountId)));
    }

    @PutMapping("/seller/discounts/{discountId}")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> update(@PathVariable Integer discountId,
                                                                     @Valid @RequestBody SellerDiscountRequest request) {
        SellerDiscountResponse response = service.update(101, discountId, request);
        return ResponseEntity.ok(ApiResponse.ok("Discount updated successfully", response));
    }

    @PatchMapping("/seller/discounts/{discountId}/submit")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> submitForReview(@PathVariable Integer discountId) {
        SellerDiscountResponse response = service.submitForReview(101, discountId);
        return ResponseEntity.ok(ApiResponse.ok("Discount submitted for admin review", response));
    }

    @GetMapping("/admin/discounts/pending")
    public ResponseEntity<ApiResponse<List<SellerDiscountResponse>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok("Pending discounts retrieved successfully", service.getPendingForAdmin()));
    }

    @GetMapping("/admin/discounts")
    public ResponseEntity<ApiResponse<List<SellerDiscountResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.ok("Discounts retrieved successfully", service.getAllForAdmin()));
    }

    @PatchMapping("/admin/discounts/{discountId}/approve")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> approve(@PathVariable Integer discountId) {
        return ResponseEntity.ok(ApiResponse.ok("Discount approved successfully", service.approve(discountId)));
    }

    @PatchMapping("/admin/discounts/{discountId}/reject")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> reject(@PathVariable Integer discountId,
                                                                     @Valid @RequestBody DiscountRejectionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Discount rejected successfully", service.reject(discountId, request)));
    }

    @PutMapping("/admin/discounts/{discountId}")
    public ResponseEntity<ApiResponse<SellerDiscountResponse>> editByAdmin(@PathVariable Integer discountId,
                                                                          @Valid @RequestBody SellerDiscountRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Discount updated by admin", service.editByAdmin(discountId, request)));
    }

    @GetMapping("/customer/discounts")
    public ResponseEntity<ApiResponse<List<SellerDiscountResponse>>> getApplicableForCustomer() {
        return ResponseEntity.ok(ApiResponse.ok("Available discounts retrieved successfully", service.getApplicableForCustomer()));
    }
}
