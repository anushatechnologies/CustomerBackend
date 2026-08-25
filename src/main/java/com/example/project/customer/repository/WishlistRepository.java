package com.example.project.customer.repository;

import com.example.project.customer.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);
    Optional<WishlistItem> findByUserIdAndProduct_ProductId(Integer userId, Integer productId);
    void deleteByUserIdAndProduct_ProductId(Integer userId, Integer productId);
    int countByUserId(Integer userId);
}
