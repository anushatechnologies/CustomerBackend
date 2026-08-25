package com.example.project.customer.repository;

import com.example.project.customer.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndCategoryIdNot(String slug, Integer categoryId);
    List<Category> findByActiveTrueOrderBySortOrderAsc();
    List<Category> findAllByOrderBySortOrderAsc();
}