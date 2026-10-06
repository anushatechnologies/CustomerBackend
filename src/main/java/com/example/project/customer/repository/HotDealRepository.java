package com.example.project.customer.repository;

import com.example.project.customer.entity.HotDeal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotDealRepository extends JpaRepository<HotDeal, Long> {

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.brand.subcategory",
            "product.brand.subcategory.category",
            "product.store",
            "product.seller"
    })
    List<HotDeal> findByActiveTrueOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.brand.subcategory",
            "product.brand.subcategory.category",
            "product.store",
            "product.seller"
    })
    List<HotDeal> findAllByOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.brand.subcategory",
            "product.brand.subcategory.category",
            "product.store",
            "product.seller"
    })
    @Override
    Optional<HotDeal> findById(Long id);

    boolean existsByProduct_ProductId(Integer productId);

    Optional<HotDeal> findByProduct_ProductId(Integer productId);

    @Query("SELECT COALESCE(MAX(h.displayOrder), 0) FROM HotDeal h")
    int findMaxDisplayOrder();
}
