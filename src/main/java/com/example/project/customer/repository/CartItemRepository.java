package com.example.project.customer.repository;

import com.example.project.customer.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart_CartId(Integer cartId);
    Optional<CartItem> findByCart_CartIdAndCartItemId(Integer cartId, Integer cartItemId);
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(Integer cartId, Integer productId);
    List<CartItem> findAllByCart_CartIdAndProduct_ProductId(Integer cartId, Integer productId);
    void deleteByCart_CartId(Integer cartId);
}
