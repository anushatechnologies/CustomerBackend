package com.example.project.customer.repository;

import com.example.project.customer.entity.SellerProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerProductRepository extends JpaRepository<SellerProduct, String> {
    
    // Find all products by seller ID with pagination
    Page<SellerProduct> findBySellerId(String sellerId, Pageable pageable);
    
    // Find product by ID and seller ID (ownership check)
    Optional<SellerProduct> findByIdAndSellerId(String id, String sellerId);
    
    // Check if SKU exists for a seller
    boolean existsBySkuAndSellerId(String sku, String sellerId);
    
    // Find by status
    Page<SellerProduct> findBySellerIdAndStatus(String sellerId, String status, Pageable pageable);
    
    // Custom search query
    @Query("SELECT p FROM SellerProduct p WHERE p.sellerId = :sellerId " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SellerProduct> searchProducts(@Param("sellerId") String sellerId, 
                                       @Param("search") String search, 
                                       Pageable pageable);
    
    // Find by category and seller
    Page<SellerProduct> findBySellerIdAndCategoryId(String sellerId, Integer categoryId, Pageable pageable);
    
    // Find by brand and seller
    Page<SellerProduct> findBySellerIdAndBrandId(String sellerId, Integer brandId, Pageable pageable);
    
    // Find by status and seller
    List<SellerProduct> findBySellerIdAndStatus(String sellerId, String status);
}
