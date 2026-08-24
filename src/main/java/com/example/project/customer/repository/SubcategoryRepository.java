package com.example.project.customer.repository;

import com.example.project.customer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndSubcategoryIdNot(String slug, Integer subcategoryId);
}