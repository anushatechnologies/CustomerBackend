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

    List<ProductReview> findByProduct_ProductId(Integer productId);

    boolean existsByOrderItem_OrderItemId(Integer orderItemId);

    Optional<ProductReview> findByCustomer_CustomerIdAndId(Integer customerId, Long id);

    Page<ProductReview> findByProduct_ProductIdAndStatus(Integer productId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByProduct_ProductIdAndStatusAndRating(Integer productId, ReviewStatus status, int rating, Pageable pageable);

    Page<ProductReview> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.productId = :productId AND r.status = :status")
    long countByProductAndStatus(@Param("productId") Integer productId, @Param("status") ReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ProductReview r WHERE r.product.productId = :productId AND r.status = :status")
    Double averageRatingByProductAndStatus(@Param("productId") Integer productId, @Param("status") ReviewStatus status);

    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r WHERE r.product.productId = :productId AND r.status = :status GROUP BY r.rating")
    List<Object[]> getRatingBreakdown(@Param("productId") Integer productId, @Param("status") ReviewStatus status);
}
