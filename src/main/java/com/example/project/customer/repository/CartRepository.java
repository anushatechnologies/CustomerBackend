package com.example.project.customer.repository;

import com.example.project.customer.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByCustomer_CustomerIdAndIsActiveTrue(Integer customerId);

    Optional<Cart> findByCustomer_CustomerIdAndStore_StoreId(Integer customerId, Integer storeId);

    List<Cart> findByCustomer_CustomerId(Integer customerId);
}
