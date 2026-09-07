package com.example.project.customer.repository;

import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Integer>,
                JpaSpecificationExecutor<Product> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForStockUpdate(@Param("productId") Integer productId);

    @Query("SELECT p FROM Product p WHERE p.seller.sellerId = :sellerId")
    List<Product> findBySellerId(@Param("sellerId") Integer sellerId);

    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.seller.sellerId = :sellerId")
    Optional<Product> findByProductIdAndSellerId(@Param("productId") Integer productId, @Param("sellerId") Integer sellerId);

    @Query("SELECT p FROM Product p WHERE p.seller.sellerId = :sellerId AND p.brand.brandId = :brandId")
    List<Product> findBySellerIdAndBrand_BrandId(@Param("sellerId") Integer sellerId, @Param("brandId") Integer brandId);

    @Query("SELECT p FROM Product p WHERE p.seller.sellerId = :sellerId AND p.brand.subcategory.category.categoryId = :categoryId")
    List<Product> findBySellerIdAndBrand_Subcategory_Category_CategoryId(@Param("sellerId") Integer sellerId, @Param("categoryId") Integer categoryId);

    // Product approval workflow
    List<Product> findByApprovalStatusAndActive(
            ApprovalStatus approvalStatus,
            boolean active
    );

    List<Product> findByApprovalStatus(
            ApprovalStatus approvalStatus
    );

    Optional<Product> findByProductIdAndApprovalStatusAndActive(
            Integer productId,
            ApprovalStatus approvalStatus,
            boolean active
    );

    // Product validation / brand, subcategory, category counts
    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndProductIdNot(
            String slug,
            Integer productId
    );

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndProductIdNot(
            String title,
            Integer productId
    );

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndProductIdNot(
            String sku,
            Integer productId
    );

    int countByBrand_BrandId(
            Integer brandId
    );

    int countByBrand_Subcategory_SubcategoryId(
            Integer subcategoryId
    );

    int countByBrand_Subcategory_Category_CategoryId(
            Integer categoryId
    );

    // Search suggestions
    List<Product> findTop5ByTitleContainingIgnoreCaseOrBrand_NameContainingIgnoreCase(
            String title,
            String brandName
    );
}
