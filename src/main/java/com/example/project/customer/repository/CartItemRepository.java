package com.example.project.customer.repository;

import com.example.project.customer.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    void deleteByCart_CartId(@Param("cartId") Integer cartId);

    boolean existsByProduct_ProductId(Integer productId);
}
