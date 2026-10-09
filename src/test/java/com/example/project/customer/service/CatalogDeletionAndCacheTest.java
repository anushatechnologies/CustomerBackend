package com.example.project.customer.service;

import com.example.project.customer.controller.AdminCacheController;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CatalogDeletionAndCacheTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private S3ImageService s3ImageService;

    @Mock
    private ProductSpecificationValidator validator;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @InjectMocks
    private SubcategoryServiceImpl subcategoryService;

    @InjectMocks
    private BrandServiceImpl brandService;

    @Test
    @DisplayName("Category delete: Soft-deletes when subcategories or products exist")
    void testCategorySoftDelete_WhenRelationsExist() {
        Category category = Category.builder().categoryId(1).name("Steel").active(true).build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.countByBrand_Subcategory_Category_CategoryId(1)).thenReturn(5);

        categoryService.delete(1);

        assertThat(category.getActive()).isFalse();
        verify(categoryRepository).save(category);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Subcategory delete: Soft-deletes when products exist")
    void testSubcategorySoftDelete_WhenProductsExist() {
        Subcategory subcategory = Subcategory.builder().subcategoryId(10).name("TMT Steel").active(true).visibleOnWebsite(true).build();
        when(subcategoryRepository.findById(10)).thenReturn(Optional.of(subcategory));
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(3);

        subcategoryService.delete(10);

        assertThat(subcategory.getActive()).isFalse();
        assertThat(subcategory.getVisibleOnWebsite()).isFalse();
        verify(subcategoryRepository).save(subcategory);
        verify(subcategoryRepository, never()).delete(any(Subcategory.class));
    }

    @Test
    @DisplayName("Brand delete: Soft-deletes when products exist")
    void testBrandSoftDelete_WhenProductsExist() {
        Brand brand = Brand.builder().brandId(20).name("Tata").active(true).build();
        when(brandRepository.findById(20)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrand_BrandId(20)).thenReturn(8);

        brandService.delete(20);

        assertThat(brand.getActive()).isFalse();
        verify(brandRepository).save(brand);
        verify(brandRepository, never()).delete(any(Brand.class));
    }

    @Test
    @DisplayName("Product delete: Soft-deletes when order items exist")
    void testProductSoftDelete_WhenOrderItemsExist() {
        ProductServiceImpl productService = new ProductServiceImpl(
                productRepository, brandRepository, subcategoryRepository, categoryRepository, storeRepository, s3ImageService, validator
        );
        ReflectionTestUtils.setField(productService, "orderItemRepository", orderItemRepository);
        ReflectionTestUtils.setField(productService, "cartItemRepository", cartItemRepository);

        Product product = Product.builder().productId(30).title("Cement Bag").active(true).build();
        when(productRepository.findById(30)).thenReturn(Optional.of(product));
        when(orderItemRepository.existsByProductId(30)).thenReturn(true);

        productService.delete(30);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("AdminCacheController: Clears all caches successfully")
    void testAdminCacheClear() {
        AdminCacheController controller = new AdminCacheController(cacheManager);
        when(cacheManager.getCacheNames()).thenReturn(Set.of("brands"));
        when(cacheManager.getCache("brands")).thenReturn(cache);

        ResponseEntity<ApiResponse<Void>> response = controller.clearAllCaches();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(cache).clear();
    }
}
