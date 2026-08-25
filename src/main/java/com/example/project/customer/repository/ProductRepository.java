package com.example.project.customer.repository;

import com.example.project.customer.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findBySubcategory_SubcategoryId(Integer subcategoryId);
    List<Product> findBySubcategory_SubcategoryIdAndActive(Integer subcategoryId, boolean active);
    List<Product> findByCategory_CategoryId(Integer categoryId);
    List<Product> findByCategory_CategoryIdAndActive(Integer categoryId, boolean active);
    List<Product> findByActive(boolean active);
    Optional<Product> findBySlugIgnoreCase(String slug);
    long countByCategory_CategoryId(Integer categoryId);
    long countBySubcategory_SubcategoryId(Integer subcategoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchByTitleOrBrand(@Param("query") String query);

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL")
    List<String> findDistinctBrands();
}