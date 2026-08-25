package com.example.project.customer.repository;

import com.example.project.customer.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndCategoryIdNot(String slug, Integer id);
    List<Category> findByActiveOrderBySortOrderAsc(boolean active);
    List<Category> findAllByOrderBySortOrderAsc();
    Optional<Category> findBySlugIgnoreCase(String slug);
}