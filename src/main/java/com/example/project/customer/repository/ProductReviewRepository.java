package com.example.project.customer.repository;

import com.example.project.customer.entity.ProductReview;
import com.example.project.customer.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Existing methods
    List<ProductReview> findByProductId(Long productId);
    boolean existsByOrderItemId(Long orderItemId);

    // New query methods
    Optional<ProductReview> findByCustomerIdAndId(Long customerId, Long id);

    Page<ProductReview> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByProductIdAndStatusAndRating(Long productId, ReviewStatus status, int rating, Pageable pageable);

    Page<ProductReview> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.productId = :productId AND r.status = com.example.project.customer.entity.ReviewStatus.APPROVED")
    long countApprovedByProduct(@Param("productId") Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.productId = :productId AND r.status = com.example.project.customer.entity.ReviewStatus.APPROVED")
    Double averageRatingByProduct(@Param("productId") Long productId);
}
