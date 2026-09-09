package com.example.project.customer.repository;

import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer>, JpaSpecificationExecutor<Store> {

    Optional<Store> findBySlugIgnoreCase(String slug);

    Optional<Store> findBySeller_SellerId(Integer sellerId);

    default Optional<Store> findBySellerSellerId(Integer sellerId) {
        return findBySeller_SellerId(sellerId);
    }

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndStoreIdNot(String slug, Integer storeId);

    Page<Store> findByStatus(StoreStatus status, Pageable pageable);
}
