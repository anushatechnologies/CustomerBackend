package com.example.project.customer.repository;

import com.example.project.customer.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId);
    Optional<WishlistItem> findByCustomer_CustomerIdAndProduct_ProductId(Integer userId, Integer productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem w WHERE w.customer.customerId = :userId AND w.product.productId = :productId")
    void deleteByUserIdAndProduct_ProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Query("SELECT COUNT(w) FROM WishlistItem w WHERE w.customer.customerId = :userId")
    int countByUserId(@Param("userId") Integer userId);
}
