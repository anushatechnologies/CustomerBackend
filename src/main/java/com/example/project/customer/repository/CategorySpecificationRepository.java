package com.example.project.customer.repository;

import com.example.project.customer.entity.CategorySpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySpecificationRepository extends JpaRepository<CategorySpecification, Integer> {

    List<CategorySpecification> findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(Integer categoryId);

    List<CategorySpecification> findByCategory_CategoryIdOrderByDisplayOrderAsc(Integer categoryId);

    Optional<CategorySpecification> findByCategory_CategoryIdAndSpecification_SpecificationId(Integer categoryId, Integer specificationId);

    boolean existsByCategory_CategoryIdAndSpecification_SpecificationId(Integer categoryId, Integer specificationId);

    void deleteByCategory_CategoryIdAndSpecification_SpecificationId(Integer categoryId, Integer specificationId);
}
