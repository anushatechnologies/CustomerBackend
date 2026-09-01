package com.example.project.customer.repository;

import com.example.project.customer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {
    Optional<Subcategory> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndSubcategoryIdNot(String slug, Integer subcategoryId);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndSubcategoryIdNot(String name, Integer subcategoryId);
    boolean existsByNameIgnoreCaseAndCategory_CategoryId(String name, Integer categoryId);
    boolean existsByNameIgnoreCaseAndCategory_CategoryIdAndSubcategoryIdNot(String name, Integer categoryId, Integer subcategoryId);

    // Filter by Category ID
    List<Subcategory> findByCategory_CategoryIdOrderBySortOrderAsc(Integer categoryId);
    List<Subcategory> findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(Integer categoryId, boolean active);

    List<Subcategory> findByActiveTrueOrderBySortOrderAsc();
    List<Subcategory> findAllByOrderBySortOrderAsc();
}