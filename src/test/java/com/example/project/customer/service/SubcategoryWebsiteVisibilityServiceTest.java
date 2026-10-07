package com.example.project.customer.service;

import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubcategoryWebsiteVisibilityServiceTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private SubcategoryServiceImpl subcategoryService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private Subcategory subcategoryActiveAndVisible;
    private Subcategory subcategoryActiveAndHidden;
    private Subcategory subcategoryInactiveAndVisible;
    private Subcategory subcategoryInactiveAndHidden;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .categoryId(10)
                .name("Home Decors")
                .slug("home-decors")
                .active(true)
                .build();

        // 1. isActive=true, visibleOnWebsite=true
        subcategoryActiveAndVisible = Subcategory.builder()
                .subcategoryId(101)
                .category(testCategory)
                .name("Wall Art & Paintings")
                .slug("wall-art")
                .active(true)
                .visibleOnWebsite(true)
                .build();

        // 2. isActive=true, visibleOnWebsite=false (THE MOST IMPORTANT CASE)
        subcategoryActiveAndHidden = Subcategory.builder()
                .subcategoryId(102)
                .category(testCategory)
                .name("Luxury Chandeliers")
                .slug("luxury-chandeliers")
                .active(true)
                .visibleOnWebsite(false)
                .build();

        // 3. isActive=false, visibleOnWebsite=true
        subcategoryInactiveAndVisible = Subcategory.builder()
                .subcategoryId(103)
                .category(testCategory)
                .name("Vintage Clocks")
                .slug("vintage-clocks")
                .active(false)
                .visibleOnWebsite(true)
                .build();

        // 4. isActive=false, visibleOnWebsite=false
        subcategoryInactiveAndHidden = Subcategory.builder()
                .subcategoryId(104)
                .category(testCategory)
                .name("Seasonal Wreaths")
                .slug("seasonal-wreaths")
                .active(false)
                .visibleOnWebsite(false)
                .build();
    }

    // =========================================================================
    // 4 COMBINATIONS MATRIX TESTS
    // =========================================================================

    @Test
    @DisplayName("Test 1: isActive=true, visibleOnWebsite=true -> Visible in Existing App and Visible on New Website")
    void test1_ActiveAndVisibleOnWebsite_VisibleInBoth() {
        // Given
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(10, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(10, true, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));

        // When - Existing App calls
        List<SubcategoryResponse> existingAppResults = subcategoryService.getAll(10, true);

        // When - New Website calls
        List<SubcategoryResponse> newWebsiteResults = subcategoryService.getAll(10, true, true);

        // Then
        assertThat(existingAppResults).hasSize(1);
        assertThat(existingAppResults.get(0).getName()).isEqualTo("Wall Art & Paintings");
        assertThat(existingAppResults.get(0).isActive()).isTrue();
        assertThat(existingAppResults.get(0).isVisibleOnWebsite()).isTrue();

        assertThat(newWebsiteResults).hasSize(1);
        assertThat(newWebsiteResults.get(0).getName()).isEqualTo("Wall Art & Paintings");
    }

    @Test
    @DisplayName("Test 2 (MOST IMPORTANT): isActive=true, visibleOnWebsite=false -> Visible in Existing App, Hidden from New Website")
    void test2_ActiveAndHiddenOnWebsite_VisibleInApp_HiddenOnWebsite() {
        // Given
        // Existing app gets all active subcategories (ignoring visibleOnWebsite)
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(10, true))
                .thenReturn(List.of(subcategoryActiveAndVisible, subcategoryActiveAndHidden));

        // New website only gets active + visibleOnWebsite
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(10, true, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));

        // When - Existing App calls
        List<SubcategoryResponse> existingAppResults = subcategoryService.getAll(10, true);

        // When - New Website calls via general getAll with visibleOnWebsite=true
        List<SubcategoryResponse> newWebsiteResults = subcategoryService.getAll(10, true, true);

        // When - New Website calls via dedicated endpoint
        List<SubcategoryResponse> dedicatedWebsiteResults = subcategoryService.getWebsiteSubcategories(10);

        // Then - Existing App STILL SHOWS both subcategories
        assertThat(existingAppResults).hasSize(2);
        assertThat(existingAppResults).extracting(SubcategoryResponse::getName)
                .containsExactly("Wall Art & Paintings", "Luxury Chandeliers");

        // Then - New Website ONLY SHOWS the website-visible subcategory (Hides subcategoryActiveAndHidden)
        assertThat(newWebsiteResults).hasSize(1);
        assertThat(newWebsiteResults.get(0).getName()).isEqualTo("Wall Art & Paintings");

        assertThat(dedicatedWebsiteResults).hasSize(1);
        assertThat(dedicatedWebsiteResults.get(0).getName()).isEqualTo("Wall Art & Paintings");
    }

    @Test
    @DisplayName("Test 3: isActive=false, visibleOnWebsite=true -> Hidden from Existing App and Hidden from New Website")
    void test3_InactiveAndVisibleOnWebsite_HiddenInBoth() {
        // Given
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(10, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(10, true, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));

        // When
        List<SubcategoryResponse> existingAppResults = subcategoryService.getAll(10, true);
        List<SubcategoryResponse> newWebsiteResults = subcategoryService.getAll(10, true, true);

        // Then - Inactive subcategory is absent in both
        assertThat(existingAppResults).extracting(SubcategoryResponse::getName)
                .doesNotContain("Vintage Clocks");
        assertThat(newWebsiteResults).extracting(SubcategoryResponse::getName)
                .doesNotContain("Vintage Clocks");
    }

    @Test
    @DisplayName("Test 4: isActive=false, visibleOnWebsite=false -> Hidden from Existing App and Hidden from New Website")
    void test4_InactiveAndHiddenOnWebsite_HiddenInBoth() {
        // Given
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(10, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));
        when(subcategoryRepository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(10, true, true))
                .thenReturn(List.of(subcategoryActiveAndVisible));

        // When
        List<SubcategoryResponse> existingAppResults = subcategoryService.getAll(10, true);
        List<SubcategoryResponse> newWebsiteResults = subcategoryService.getAll(10, true, true);

        // Then - Inactive + hidden subcategory is absent in both
        assertThat(existingAppResults).extracting(SubcategoryResponse::getName)
                .doesNotContain("Seasonal Wreaths");
        assertThat(newWebsiteResults).extracting(SubcategoryResponse::getName)
                .doesNotContain("Seasonal Wreaths");
    }

    // =========================================================================
    // ADMIN TOGGLE WORKFLOW TESTS: ON -> OFF -> ON
    // =========================================================================

    @Test
    @DisplayName("Admin Toggle Lifecycle: ON -> OFF -> ON independently updates visibleOnWebsite without altering active status")
    void adminToggle_Lifecycle_TogglesVisibleOnWebsiteIndependently() {
        Subcategory luxuryLighting = Subcategory.builder()
                .subcategoryId(15)
                .category(testCategory)
                .name("Luxury Lighting")
                .slug("luxury-lighting")
                .active(true)
                .visibleOnWebsite(true)
                .build();

        when(subcategoryRepository.findById(15)).thenReturn(Optional.of(luxuryLighting));
        when(subcategoryRepository.save(any(Subcategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Initial State: Active ON, Visible ON
        assertThat(luxuryLighting.isActive()).isTrue();
        assertThat(luxuryLighting.isVisibleOnWebsite()).isTrue();

        // 2. Admin turns Visible OFF
        SubcategoryResponse responseOff = subcategoryService.updateWebsiteVisibility(15, false);
        assertThat(responseOff.isActive()).isTrue(); // active MUST remain untouched!
        assertThat(responseOff.isVisibleOnWebsite()).isFalse();

        // 3. Admin turns Visible ON again
        SubcategoryResponse responseOn = subcategoryService.updateWebsiteVisibility(15, true);
        assertThat(responseOn.isActive()).isTrue(); // active MUST remain untouched!
        assertThat(responseOn.isVisibleOnWebsite()).isTrue();
    }

    @Test
    @DisplayName("Create Subcategory with default visibleOnWebsite=true")
    void createSubcategory_DefaultsToVisibleOnWebsiteTrue() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(10)
                .name("Modern Mirrors")
                .build();

        when(categoryRepository.findById(10)).thenReturn(Optional.of(testCategory));
        when(subcategoryRepository.save(any(Subcategory.class))).thenAnswer(inv -> {
            Subcategory s = inv.getArgument(0);
            s.setSubcategoryId(201);
            return s;
        });

        SubcategoryResponse created = subcategoryService.create(request);

        assertThat(created.isVisibleOnWebsite()).isTrue();
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Update Subcategory via PUT supports visibleOnWebsite")
    void updateSubcategory_SupportsVisibleOnWebsite() {
        Subcategory existing = Subcategory.builder()
                .subcategoryId(15)
                .category(testCategory)
                .name("Lighting")
                .slug("lighting")
                .active(true)
                .visibleOnWebsite(true)
                .build();

        when(subcategoryRepository.findById(15)).thenReturn(Optional.of(existing));
        when(subcategoryRepository.save(any(Subcategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcategoryRequest updateReq = SubcategoryRequest.builder()
                .categoryId(10)
                .name("Lighting Updated")
                .active(true)
                .visibleOnWebsite(false)
                .build();

        SubcategoryResponse updated = subcategoryService.update(15, updateReq);

        assertThat(updated.isActive()).isTrue();
        assertThat(updated.isVisibleOnWebsite()).isFalse();
    }
}
