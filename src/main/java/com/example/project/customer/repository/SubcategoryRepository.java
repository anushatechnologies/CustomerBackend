package com.example.project.customer.repository;

import com.example.project.customer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndSubcategoryIdNot(String slug, Integer id);
    List<Subcategory> findByCategory_CategoryId(Integer categoryId);
    List<Subcategory> findByCategory_CategoryIdAndActive(Integer categoryId, boolean active);
    List<Subcategory> findByActiveOrderBySortOrderAsc(boolean active);
    List<Subcategory> findAllByOrderBySortOrderAsc();
    Optional<Subcategory> findBySlugIgnoreCase(String slug);
    long countByCategory_CategoryId(Integer categoryId);
}