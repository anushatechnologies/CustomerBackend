package com.example.project.customer.repository;

import com.example.project.customer.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    @EntityGraph(attributePaths = {"product"})
    List<CartItem> findByCart_CartId(Integer cartId);

    @EntityGraph(attributePaths = {"product"})
    Optional<CartItem> findByCart_CartIdAndCartItemId(Integer cartId, Integer cartItemId);

    @EntityGraph(attributePaths = {"product"})
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(Integer cartId, Integer productId);

    @EntityGraph(attributePaths = {"product"})
    List<CartItem> findAllByCart_CartIdAndProduct_ProductId(Integer cartId, Integer productId);

    void deleteByCart_CartId(Integer cartId);
}
