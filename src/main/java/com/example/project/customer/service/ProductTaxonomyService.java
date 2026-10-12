package com.example.project.customer.service;

import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Authoritative taxonomy service governing Product-Subcategory and Product-Category associations.
 * Enforces verified subcategory persistence, cross-category validation, and protects against
 * accidental erasure or blind copying from brand subcategories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductTaxonomyService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Resolves, validates, and returns the authoritative Subcategory for a product.
     *
     * @param requestedSubcategoryId The subcategoryId explicitly provided in request (may be null).
     * @param requestedCategoryId    Optional categoryId provided for cross-validation (may be null).
     * @param existingProduct        Existing product during update operations (null during creation).
     * @param resolvedBrand          Brand associated with the product (may be null).
     * @return The validated Subcategory entity to assign to the product, or null if unclassified.
     */
    public Subcategory resolveSubcategoryForProduct(
            Integer requestedSubcategoryId,
            Integer requestedCategoryId,
            Product existingProduct,
            Brand resolvedBrand
    ) {
        // 1. Explicit subcategory ID supplied: Primary source of truth
        if (requestedSubcategoryId != null) {
            Subcategory subcategory = subcategoryRepository.findById(requestedSubcategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Subcategory not found with id: " + requestedSubcategoryId));

            // Validate Category-Subcategory hierarchy if categoryId is also supplied
            if (requestedCategoryId != null && subcategory.getCategory() != null) {
                if (!subcategory.getCategory().getCategoryId().equals(requestedCategoryId)) {
                    throw new IllegalArgumentException(String.format(
                            "Subcategory '%s' (ID: %d) does not belong to Category ID: %d (actual Category ID: %d)",
                            subcategory.getName(),
                            requestedSubcategoryId,
                            requestedCategoryId,
                            subcategory.getCategory().getCategoryId()
                    ));
                }
            }

            if (resolvedBrand != null && resolvedBrand.getSubcategory() != null
                    && !resolvedBrand.getSubcategory().getSubcategoryId().equals(subcategory.getSubcategoryId())) {
                log.info("Product assigned to subcategory '{}' (ID: {}), which differs from brand '{}' subcategory ID: {}",
                        subcategory.getName(), subcategory.getSubcategoryId(),
                        resolvedBrand.getName(), resolvedBrand.getSubcategory().getSubcategoryId());
            }

            return subcategory;
        }

        // 2. Partial update preservation: If this is an update and subcategoryId was not supplied in request,
        // preserve the existing product's subcategory.
        if (existingProduct != null && existingProduct.getSubcategory() != null) {
            return existingProduct.getSubcategory();
        }

        // 3. Fallback / Unclassified: Do NOT blindly copy brand's subcategory into products.subcategory_id.
        // A product without an explicit subcategory remains unclassified at the product level.
        return null;
    }
}
