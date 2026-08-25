package com.example.project.customer;

import com.example.project.customer.dto.*;
import com.example.project.customer.service.CategoryService;
import com.example.project.customer.service.ProductService;
import com.example.project.customer.service.SubcategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductAndSearchFlowTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SubcategoryService subcategoryService;

    @Autowired
    private ProductService productService;

    @Test
    void testCategoriesAndSubcategoryFiltering() {
        List<CategoryResponse> categories = categoryService.getAll(true, true);
        assertNotNull(categories);
        assertFalse(categories.isEmpty());

        Integer civilCatId = categories.stream()
                .filter(c -> "civil-structural".equals(c.getSlug()))
                .map(CategoryResponse::getCategoryId)
                .findFirst()
                .orElse(null);

        assertNotNull(civilCatId);

        // Subcategory filtering by categoryId (QA Bug 1 Fix test)
        List<SubcategoryResponse> subs = subcategoryService.getAll(civilCatId, true);
        assertNotNull(subs);
        assertFalse(subs.isEmpty());
        for (SubcategoryResponse s : subs) {
            assertEquals(civilCatId, s.getCategoryId(), "Subcategory must belong to requested categoryId");
        }
    }

    @Test
    void testProductFilteringAndBulkSlabs() {
        var paged = productService.getProducts(null, null, "Tata Tiscon", null, null, null, null, null, 1, 10);
        assertNotNull(paged);
        assertNotNull(paged.getData());
        assertFalse(paged.getData().isEmpty());

        ProductResponse prod = paged.getData().get(0);
        assertEquals("Tata Tiscon", prod.getBrand());

        // Test Product Detail with Bulk Pricing Slabs (QA Bug 5 Fix test)
        ProductResponse detail = productService.getById(prod.getProductId());
        assertNotNull(detail);
        assertNotNull(detail.getBulkPricingTiers());
        assertFalse(detail.getBulkPricingTiers().isEmpty());
        assertNotNull(detail.getSpecifications());
        assertNotNull(detail.getVendor());
    }

    @Test
    void testSearchSuggestions() {
        SearchSuggestionsResponse suggestions = productService.getSearchSuggestions("Tata");
        assertNotNull(suggestions);
        assertNotNull(suggestions.getProducts());
        assertNotNull(suggestions.getCategories());
        assertNotNull(suggestions.getPopularSearches());
        assertFalse(suggestions.getPopularSearches().isEmpty());
    }
}
