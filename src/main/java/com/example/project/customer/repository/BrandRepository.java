package com.example.project.customer.repository;

import com.example.project.customer.entity.Brand;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer>, JpaSpecificationExecutor<Brand> {

    @Override
    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    Optional<Brand> findById(Integer id);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findBySubcategory_SubcategoryIdOrderBySortOrderAsc(Integer subcategoryId);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findBySubcategory_SubcategoryIdAndActiveOrderBySortOrderAsc(Integer subcategoryId, Boolean active);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findBySubcategory_Category_CategoryIdOrderBySortOrderAsc(Integer categoryId);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findBySubcategory_Category_CategoryIdAndActiveOrderBySortOrderAsc(Integer categoryId, Boolean active);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findByActiveTrueOrderBySortOrderAsc();

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Brand> findAllByOrderBySortOrderAsc();

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndBrandIdNot(String slug, Integer brandId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndBrandIdNot(String name, Integer brandId);

    boolean existsByNameIgnoreCaseAndSubcategory_SubcategoryId(String name, Integer subcategoryId);

    boolean existsByNameIgnoreCaseAndSubcategory_SubcategoryIdAndBrandIdNot(String name, Integer subcategoryId, Integer brandId);

    int countByBrandId(Integer brandId);

    int countBySubcategory_SubcategoryId(Integer subcategoryId);

    int countBySubcategory_Category_CategoryId(Integer categoryId);

    Optional<Brand> findByNameIgnoreCase(String name);

    Optional<Brand> findByNameIgnoreCaseAndSubcategory_SubcategoryId(String name, Integer subcategoryId);
}
