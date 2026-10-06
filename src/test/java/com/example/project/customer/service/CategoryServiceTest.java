package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.exception.ResourceNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .name("Steel & Rebars")
                .slug("steel-rebars")
                .imageUrl("https://storage/categories/steel.jpg")
                .active(true)
                .sortOrder(1)
                .productCount(10)
                .build();
    }

    @Test
    @DisplayName("create - should save new category")
    void create_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Steel & Rebars")
                .slug("steel-rebars")
                .imageUrl("https://storage/categories/steel.jpg")
                .active(true)
                .sortOrder(1)
                .build();

        when(repository.existsBySlugIgnoreCase("steel-rebars")).thenReturn(false);
        when(repository.save(any(Category.class))).thenReturn(category);
        when(productRepository.countByBrand_Subcategory_Category_CategoryId(1)).thenReturn(0);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Steel & Rebars");
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("getById - should return category when found")
    void getById_Success() {
        when(repository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.countByBrand_Subcategory_Category_CategoryId(1)).thenReturn(10);

        CategoryResponse response = categoryService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(1);
    }

    @Test
    @DisplayName("update - should update category and delete old S3 image when replaced")
    void update_ReplacesOldS3Image() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Steel & Rebars Updated")
                .slug("steel-rebars-updated")
                .imageUrl("https://storage/categories/new-steel.jpg")
                .active(true)
                .sortOrder(2)
                .build();

        when(repository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsBySlugIgnoreCaseAndCategoryIdNot("steel-rebars-updated", 1)).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.update(1, request);

        assertThat(response).isNotNull();
        verify(s3ImageService).deleteImage("https://storage/categories/steel.jpg");
        verify(repository).save(category);
    }

    @Test
    @DisplayName("delete - should delete category and clean up S3 image")
    void delete_CleansUpS3Image() {
        when(repository.findById(1)).thenReturn(Optional.of(category));

        categoryService.delete(1);

        verify(repository).delete(category);
        verify(s3ImageService).deleteImage("https://storage/categories/steel.jpg");
    }

    @Test
    @DisplayName("delete - should throw ResourceNotFoundException when category not found")
    void delete_NotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAll paginated - should return paginated category response")
    void getAll_Paginated_Success() {
        org.springframework.data.domain.Page<Category> page = new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(category),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1
        );
        when(repository.findAllByOrderBySortOrderAsc(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(productRepository.countByBrand_Subcategory_Category_CategoryId(1)).thenReturn(5);

        ApiResponse<java.util.List<CategoryResponse>> response = categoryService.getAll(false, false, 1, 10);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getPagination()).isNotNull();
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getPagination().getPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("create - should throw InvalidImageException and NOT save when imageUrl is a blob URL")
    void create_BlobUrl_ThrowsInvalidImageException_DoesNotSave() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Plumbing")
                .imageUrl("blob:http://localhost:5173/58fca786-fa31-4b60-bb91-e03efe75662c")
                .build();

        when(repository.existsByNameIgnoreCase("Plumbing")).thenReturn(false);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Blob URLs");

        verify(repository, org.mockito.Mockito.never()).save(any(Category.class));
    }

    @Test
    @DisplayName("create - should throw InvalidImageException and NOT save when imageUrl is a base64 data URL")
    void create_DataUri_ThrowsInvalidImageException_DoesNotSave() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electrical")
                .imageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .build();

        when(repository.existsByNameIgnoreCase("Electrical")).thenReturn(false);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Base64 data URLs");

        verify(repository, org.mockito.Mockito.never()).save(any(Category.class));
    }

    @Test
    @DisplayName("create - should handle null imageUrl correctly and persist null")
    void create_NullImageUrl_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Roofing")
                .imageUrl(null)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(2)
                .name("Roofing")
                .slug("roofing")
                .imageUrl(null)
                .active(true)
                .build();

        when(repository.existsByNameIgnoreCase("Roofing")).thenReturn(false);
        when(repository.existsBySlugIgnoreCase("roofing")).thenReturn(false);
        when(repository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isNull();
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("create - should normalize empty string imageUrl to null")
    void create_EmptyImageUrl_NormalizesToNull() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Paints")
                .imageUrl("   ")
                .build();

        Category savedCategory = Category.builder()
                .categoryId(3)
                .name("Paints")
                .slug("paints")
                .imageUrl(null)
                .active(true)
                .build();

        when(repository.existsByNameIgnoreCase("Paints")).thenReturn(false);
        when(repository.existsBySlugIgnoreCase("paints")).thenReturn(false);
        when(repository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isNull();
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("create - should persist valid S3 URL")
    void create_ValidS3Url_Persisted() {
        String s3Url = "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/categories/uuid-123.jpg";
        CategoryRequest request = CategoryRequest.builder()
                .name("Wood & Timber")
                .imageUrl(s3Url)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(4)
                .name("Wood & Timber")
                .slug("wood-timber")
                .imageUrl(s3Url)
                .active(true)
                .build();

        when(repository.existsByNameIgnoreCase("Wood & Timber")).thenReturn(false);
        when(repository.existsBySlugIgnoreCase("wood-timber")).thenReturn(false);
        when(repository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo(s3Url);
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("update - should throw InvalidImageException and NOT save when updated imageUrl is a blob URL")
    void update_BlobUrl_ThrowsInvalidImageException_DoesNotSave() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Steel Updated")
                .imageUrl("blob:http://localhost:5173/abcdef")
                .build();

        when(repository.findById(1)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.update(1, request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Blob URLs");

        verify(repository, org.mockito.Mockito.never()).save(any(Category.class));
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(any());
    }

    @Test
    @DisplayName("update - should not call s3 delete if old imageUrl in database was a blob URL")
    void update_WhenOldImageIsBlobUrl_DoesNotCallS3Delete() {
        Category categoryWithBlob = Category.builder()
                .categoryId(5)
                .name("Old Category")
                .slug("old-category")
                .imageUrl("blob:http://localhost:5173/old-blob-id")
                .build();

        CategoryRequest request = CategoryRequest.builder()
                .name("Old Category")
                .imageUrl("https://storage/categories/new-image.jpg")
                .build();

        when(repository.findById(5)).thenReturn(Optional.of(categoryWithBlob));
        when(repository.existsBySlugIgnoreCaseAndCategoryIdNot("old-category", 5)).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.update(5, request);

        // Crucial: S3 delete must NOT be called for the old blob URL!
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(org.mockito.ArgumentMatchers.contains("blob:"));
    }

    @Test
    @DisplayName("delete - should not call s3 delete if imageUrl in database is a blob URL")
    void delete_WhenImageIsBlobUrl_DoesNotCallS3Delete() {
        Category categoryWithBlob = Category.builder()
                .categoryId(6)
                .name("Blob Category")
                .slug("blob-category")
                .imageUrl("blob:http://localhost:5173/old-blob-id")
                .build();

        when(repository.findById(6)).thenReturn(Optional.of(categoryWithBlob));

        categoryService.delete(6);

        verify(repository).delete(categoryWithBlob);
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(any());
    }
}
