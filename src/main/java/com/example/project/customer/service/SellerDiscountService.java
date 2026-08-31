package com.example.project.customer.service;

import com.example.project.customer.dto.DiscountRejectionRequest;
import com.example.project.customer.dto.SellerDiscountRequest;
import com.example.project.customer.dto.SellerDiscountResponse;

import java.util.List;

public interface SellerDiscountService {

    SellerDiscountResponse create(Integer sellerId, SellerDiscountRequest request);

    SellerDiscountResponse getById(Integer sellerId, Integer discountId);

    List<SellerDiscountResponse> getBySeller(Integer sellerId);

    SellerDiscountResponse update(Integer sellerId, Integer discountId, SellerDiscountRequest request);

    SellerDiscountResponse submitForReview(Integer sellerId, Integer discountId);

    SellerDiscountResponse approve(Integer discountId);

    SellerDiscountResponse reject(Integer discountId, DiscountRejectionRequest request);

    SellerDiscountResponse editByAdmin(Integer discountId, SellerDiscountRequest request);

    List<SellerDiscountResponse> getPendingForAdmin();

    List<SellerDiscountResponse> getAllForAdmin();

    List<SellerDiscountResponse> getApplicableForCustomer();
}
