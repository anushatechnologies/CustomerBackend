package com.example.project.customer.repository;

import com.example.project.customer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndSubcategoryIdNot(String slug, Integer subcategoryId);

    // Filter by Category ID to fix Bug 1
    List<Subcategory> findByCategory_CategoryIdOrderBySortOrderAsc(Integer categoryId);
    List<Subcategory> findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(Integer categoryId, boolean active);

    List<Subcategory> findByActiveTrueOrderBySortOrderAsc();
    List<Subcategory> findAllByOrderBySortOrderAsc();
}