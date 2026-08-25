package com.example.project.customer.repository;

import com.example.project.customer.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    List<CartItem> findByCustomerCustomerId(Integer customerId);

    Optional<CartItem> findByCustomerCustomerIdAndProductProductId(Integer customerId, Integer productId);

    boolean existsByCustomerCustomerIdAndProductProductId(Integer customerId, Integer productId);

    boolean existsByCustomerCustomerIdAndProductProductIdAndCartItemIdNot(Integer customerId, Integer productId, Integer cartItemId);

    void deleteByCustomerCustomerId(Integer customerId);
}
