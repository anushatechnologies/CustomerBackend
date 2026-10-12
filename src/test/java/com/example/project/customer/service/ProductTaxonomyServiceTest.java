package com.example.project.customer.service;

import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductTaxonomyServiceTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductTaxonomyService taxonomyService;

    private Category categoryTools;
    private Category categoryHardware;
    private Subcategory subcategoryHandTools;
    private Subcategory subcategoryPowerTools;
    private Brand brandBosch;

    @BeforeEach
    void setUp() {
        categoryTools = Category.builder().categoryId(10).name("Tools").build();
        categoryHardware = Category.builder().categoryId(20).name("Hardware").build();

        subcategoryHandTools = Subcategory.builder()
                .subcategoryId(101)
                .name("Hand Tools")
                .category(categoryTools)
                .build();

        subcategoryPowerTools = Subcategory.builder()
                .subcategoryId(102)
                .name("Power Tools")
                .category(categoryTools)
                .build();

        brandBosch = Brand.builder()
                .brandId(500)
                .name("Bosch")
                .subcategory(subcategoryPowerTools)
                .build();
    }

    @Test
    @DisplayName("Explicit subcategory resolved successfully when matching category is provided")
    void resolveSubcategory_ValidExplicitSubcategoryWithMatchingCategory() {
        when(subcategoryRepository.findById(101)).thenReturn(Optional.of(subcategoryHandTools));

        Subcategory result = taxonomyService.resolveSubcategoryForProduct(101, 10, null, brandBosch);

        assertThat(result).isNotNull();
        assertThat(result.getSubcategoryId()).isEqualTo(101);
        assertThat(result.getName()).isEqualTo("Hand Tools");
    }

    @Test
    @DisplayName("Explicit subcategory throws IllegalArgumentException when category hierarchy does not match")
    void resolveSubcategory_ThrowsOnCategoryMismatch() {
        when(subcategoryRepository.findById(101)).thenReturn(Optional.of(subcategoryHandTools));

        assertThatThrownBy(() -> taxonomyService.resolveSubcategoryForProduct(101, 20, null, brandBosch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to Category ID: 20");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when explicit subcategory ID does not exist")
    void resolveSubcategory_ThrowsWhenSubcategoryNotFound() {
        when(subcategoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxonomyService.resolveSubcategoryForProduct(999, null, null, brandBosch))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subcategory not found with id: 999");
    }

    @Test
    @DisplayName("Update operation preserves existing product subcategory when subcategoryId is omitted")
    void resolveSubcategory_PreservesExistingProductSubcategoryOnUpdate() {
        Product existingProduct = Product.builder()
                .productId(1)
                .subcategory(subcategoryHandTools)
                .build();

        Subcategory result = taxonomyService.resolveSubcategoryForProduct(null, null, existingProduct, brandBosch);

        assertThat(result).isNotNull();
        assertThat(result.getSubcategoryId()).isEqualTo(101);
    }

    @Test
    @DisplayName("Unclassified creation without subcategory does NOT blindly copy brand subcategory")
    void resolveSubcategory_DoesNotBlindlyCopyBrandSubcategory() {
        Subcategory result = taxonomyService.resolveSubcategoryForProduct(null, null, null, brandBosch);

        assertThat(result).isNull();
    }
}
