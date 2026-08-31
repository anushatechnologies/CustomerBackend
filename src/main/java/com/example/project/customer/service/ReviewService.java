package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CreateReviewRequest;
import com.example.project.customer.dto.ProductRatingSummaryResponse;
import com.example.project.customer.dto.ReviewResponse;
import com.example.project.customer.dto.UpdateReviewRequest;
import com.example.project.customer.entity.ReviewStatus;

import java.util.List;

public interface ReviewService {

    ReviewResponse submitReview(Integer customerId, CreateReviewRequest request);

    ReviewResponse updateReview(Integer customerId, Long reviewId, UpdateReviewRequest request);

    void deleteReview(Integer customerId, Long reviewId);

    ReviewResponse voteHelpful(Integer customerId, Long reviewId);

    ApiResponse<List<ReviewResponse>> getProductReviews(Integer productId, Integer rating, int page, int limit);

    ProductRatingSummaryResponse getProductRatingSummary(Integer productId);

    ReviewResponse moderateReview(Long reviewId, ReviewStatus status);
}
