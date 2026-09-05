package com.example.project.customer.repository;

import com.example.project.customer.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndCategoryIdNot(String slug, Integer categoryId);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Integer categoryId);
    List<Category> findByActiveTrueOrderBySortOrderAsc();
    List<Category> findAllByOrderBySortOrderAsc();
    Page<Category> findByActiveTrueOrderBySortOrderAsc(Pageable pageable);
    Page<Category> findAllByOrderBySortOrderAsc(Pageable pageable);
}