package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CreateReviewRequest;
import com.example.project.customer.dto.ProductRatingSummaryResponse;
import com.example.project.customer.dto.ReviewResponse;
import com.example.project.customer.dto.UpdateReviewRequest;
import com.example.project.customer.entity.ReviewStatus;
import com.example.project.customer.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse response = reviewService.submitReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review submitted successfully", response));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review updated successfully", response));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully", null));
    }

    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> voteHelpful(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Long reviewId
    ) {
        ReviewResponse response = reviewService.voteHelpful(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok("Helpful vote recorded successfully", response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProductReviews(
            @PathVariable Integer productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, rating, page, limit));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductRatingSummary(
            @PathVariable Integer productId
    ) {
        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable Long reviewId,
            @RequestParam ReviewStatus status
    ) {
        ReviewResponse response = reviewService.moderateReview(reviewId, status);
        return ResponseEntity.ok(ApiResponse.ok("Review status updated successfully", response));
    }
}
