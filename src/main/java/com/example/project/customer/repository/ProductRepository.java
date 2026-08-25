package com.example.project.customer.repository;

import com.example.project.customer.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndProductIdNot(String slug, Integer productId);

    int countBySubcategory_SubcategoryId(Integer subcategoryId);
    int countBySubcategory_Category_CategoryId(Integer categoryId);

    List<Product> findTop5ByTitleContainingIgnoreCaseOrBrandContainingIgnoreCase(String title, String brand);
}