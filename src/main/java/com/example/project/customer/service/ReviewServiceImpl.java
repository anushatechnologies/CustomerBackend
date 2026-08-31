package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CreateReviewRequest;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductRatingSummaryResponse;
import com.example.project.customer.dto.ReviewResponse;
import com.example.project.customer.dto.UpdateReviewRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.ProductReview;
import com.example.project.customer.entity.ReviewHelpfulVote;
import com.example.project.customer.entity.ReviewImage;
import com.example.project.customer.entity.ReviewStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.ProductReviewRepository;
import com.example.project.customer.repository.ReviewHelpfulVoteRepository;
import com.example.project.customer.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ReviewResponse submitReview(Integer customerId, CreateReviewRequest request) {
        int uid = customerId != null ? customerId : 101;

        // 1. Verify OrderItem exists
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + request.getOrderItemId()));

        Order order = orderItem.getOrder();
        if (order == null) {
            throw new ResourceNotFoundException("Associated order not found for order item: " + request.getOrderItemId());
        }

        // 2. Verify Order ownership (verified purchase check)
        if (!order.getUserId().equals(uid)) {
            throw new IllegalArgumentException("You can only review items from your own orders");
        }

        // 3. Verify OrderItem has not already been reviewed
        if (reviewRepository.existsByOrderItem_OrderItemId(orderItem.getOrderItemId())) {
            throw new ResourceConflictException("A review has already been submitted for this purchased item");
        }

        // 4. Retrieve Product
        Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + orderItem.getProductId()));

        // 5. Retrieve or initialize Customer
        Customer customer = customerRepository.findById(uid)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .customerId(uid)
                        .name("Verified Buyer #" + uid)
                        .email("buyer" + uid + "@hinchmart.com")
                        .phone("9800000" + String.format("%03d", uid % 1000))
                        .build()));

        // 6. Build and save ProductReview
        ProductReview review = ProductReview.builder()
                .product(product)
                .customer(customer)
                .order(order)
                .orderItem(orderItem)
                .rating(request.getRating())
                .title(request.getTitle().trim())
                .comment(request.getComment().trim())
                .status(ReviewStatus.APPROVED)
                .helpfulCount(0)
                .build();

        ProductReview savedReview = reviewRepository.save(review);

        // 7. Save review images if attached
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            Set<ReviewImage> images = new HashSet<>();
            for (String imgUrl : request.getImageUrls()) {
                if (imgUrl != null && !imgUrl.isBlank()) {
                    ReviewImage img = ReviewImage.builder()
                            .review(savedReview)
                            .imageUrl(imgUrl.trim())
                            .build();
                    images.add(reviewImageRepository.save(img));
                }
            }
            savedReview.setImages(images);
            savedReview = reviewRepository.save(savedReview);
        }

        // 8. Dynamically update Product average rating and review count
        recalculateProductRating(product.getProductId());

        return mapToResponse(savedReview);
    }

    @Override
    public ReviewResponse updateReview(Integer customerId, Long reviewId, UpdateReviewRequest request) {
        ProductReview review = findReview(reviewId);
        int uid = customerId != null ? customerId : 101;

        if (!review.getCustomer().getCustomerId().equals(uid)) {
            throw new IllegalArgumentException("You are not authorized to edit this review");
        }

        boolean ratingChanged = false;
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            review.setTitle(request.getTitle().trim());
        }
        if (request.getComment() != null && !request.getComment().isBlank()) {
            review.setComment(request.getComment().trim());
        }
        if (request.getRating() != null && request.getRating() >= 1 && request.getRating() <= 5) {
            if (review.getRating() != request.getRating()) {
                review.setRating(request.getRating());
                ratingChanged = true;
            }
        }

        ProductReview updated = reviewRepository.save(review);

        if (ratingChanged) {
            recalculateProductRating(review.getProduct().getProductId());
        }

        return mapToResponse(updated);
    }

    @Override
    public void deleteReview(Integer customerId, Long reviewId) {
        ProductReview review = findReview(reviewId);
        int uid = customerId != null ? customerId : 101;

        if (!review.getCustomer().getCustomerId().equals(uid)) {
            throw new IllegalArgumentException("You are not authorized to delete this review");
        }

        Integer productId = review.getProduct().getProductId();
        reviewRepository.delete(review);

        // Recalculate rating aggregate after deletion
        recalculateProductRating(productId);
    }

    @Override
    public ReviewResponse voteHelpful(Integer customerId, Long reviewId) {
        ProductReview review = findReview(reviewId);
        int uid = customerId != null ? customerId : 101;

        if (helpfulVoteRepository.existsByReview_IdAndCustomer_CustomerId(reviewId, uid)) {
            throw new ResourceConflictException("You have already voted this review as helpful");
        }

        Customer customer = customerRepository.findById(uid)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .customerId(uid)
                        .name("Verified Buyer #" + uid)
                        .email("buyer" + uid + "@hinchmart.com")
                        .phone("9800000" + String.format("%03d", uid % 1000))
                        .build()));

        ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                .review(review)
                .customer(customer)
                .build();
        helpfulVoteRepository.save(vote);

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ReviewResponse>> getProductReviews(Integer productId, Integer rating, int page, int limit) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProductReview> reviewPage;
        if (rating != null && rating >= 1 && rating <= 5) {
            reviewPage = reviewRepository.findByProduct_ProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, rating, pageable);
        } else {
            reviewPage = reviewRepository.findByProduct_ProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable);
        }

        List<ReviewResponse> responseList = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta pagination = PaginationMeta.of(page > 0 ? page : 1, pageSize, reviewPage.getTotalElements());
        return ApiResponse.paginated(responseList, pagination);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Integer productId) {
        Double avgRating = reviewRepository.averageRatingByProductAndStatus(productId, ReviewStatus.APPROVED);
        long totalReviews = reviewRepository.countByProductAndStatus(productId, ReviewStatus.APPROVED);
        List<Object[]> breakdownList = reviewRepository.getRatingBreakdown(productId, ReviewStatus.APPROVED);

        Map<Integer, Long> breakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            breakdown.put(i, 0L);
        }
        for (Object[] row : breakdownList) {
            if (row != null && row.length == 2) {
                Integer star = (Integer) row[0];
                Long count = (Long) row[1];
                breakdown.put(star, count);
            }
        }

        double roundedAvg = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        return ProductRatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(roundedAvg)
                .totalReviews(totalReviews)
                .ratingBreakdown(breakdown)
                .build();
    }

    @Override
    public ReviewResponse moderateReview(Long reviewId, ReviewStatus status) {
        ProductReview review = findReview(reviewId);
        review.setStatus(status);
        ProductReview saved = reviewRepository.save(review);
        recalculateProductRating(saved.getProduct().getProductId());
        return mapToResponse(saved);
    }

    private ProductReview findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Product review not found with id: " + reviewId));
    }

    private void recalculateProductRating(Integer productId) {
        productRepository.findById(productId).ifPresent(product -> {
            Double avgRating = reviewRepository.averageRatingByProductAndStatus(productId, ReviewStatus.APPROVED);
            long totalApproved = reviewRepository.countByProductAndStatus(productId, ReviewStatus.APPROVED);

            double roundedAvg = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
            product.setRating(roundedAvg);
            product.setReviewCount((int) totalApproved);
            productRepository.save(product);
            log.info("[RATING_UPDATED] productId={}, newRating={}, totalReviews={}", productId, roundedAvg, totalApproved);
        });
    }

    private ReviewResponse mapToResponse(ProductReview r) {
        List<String> imgUrls = r.getImages() != null ? r.getImages().stream().map(ReviewImage::getImageUrl).toList() : List.of();
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : "Anonymous Buyer";
        String productTitle = r.getProduct() != null ? r.getProduct().getTitle() : null;

        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProduct() != null ? r.getProduct().getProductId() : null)
                .productTitle(productTitle)
                .customerId(r.getCustomer() != null ? r.getCustomer().getCustomerId() : null)
                .customerName(customerName)
                .orderId(r.getOrder() != null ? r.getOrder().getOrderId() : null)
                .orderItemId(r.getOrderItem() != null ? r.getOrderItem().getOrderItemId() : null)
                .rating(r.getRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .status(r.getStatus())
                .helpfulCount(r.getHelpfulCount())
                .verifiedPurchase(r.getOrderItem() != null)
                .imageUrls(imgUrls)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
