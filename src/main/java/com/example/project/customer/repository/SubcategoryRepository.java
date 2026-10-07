package com.example.project.customer.repository;

import com.example.project.customer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
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

    // New website queries (isActive = true AND visibleOnWebsite = true)
    List<Subcategory> findByCategory_CategoryIdAndActiveTrueAndVisibleOnWebsiteTrueOrderBySortOrderAsc(Integer categoryId);
    List<Subcategory> findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(Integer categoryId, boolean active, boolean visibleOnWebsite);
    List<Subcategory> findByCategory_CategoryIdAndVisibleOnWebsiteOrderBySortOrderAsc(Integer categoryId, boolean visibleOnWebsite);

    List<Subcategory> findByActiveTrueAndVisibleOnWebsiteTrueOrderBySortOrderAsc();
    List<Subcategory> findByActiveAndVisibleOnWebsiteOrderBySortOrderAsc(boolean active, boolean visibleOnWebsite);
    List<Subcategory> findByVisibleOnWebsiteOrderBySortOrderAsc(boolean visibleOnWebsite);

    List<Subcategory> findByActiveTrueOrderBySortOrderAsc();
    List<Subcategory> findAllByOrderBySortOrderAsc();
}