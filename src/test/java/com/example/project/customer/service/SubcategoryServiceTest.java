package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BrandRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubcategoryServiceTest {

    @Mock
    private SubcategoryRepository repository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private SubcategoryServiceImpl subcategoryService;

    private Category category;
    private Subcategory subcategory;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .name("Steel")
                .slug("steel")
                .build();

        subcategory = Subcategory.builder()
                .subcategoryId(10)
                .category(category)
                .name("TMT Bars")
                .slug("tmt-bars")
                .imageUrl("https://storage/subcategories/tmt.jpg")
                .active(true)
                .sortOrder(1)
                .productCount(5)
                .build();
    }

    @Test
    @DisplayName("create - should save new subcategory")
    void create_Success() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("TMT Bars")
                .slug("tmt-bars")
                .imageUrl("https://storage/subcategories/tmt.jpg")
                .active(true)
                .sortOrder(1)
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("tmt-bars", 1)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(subcategory);
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(5);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("TMT Bars");
        verify(repository).save(any(Subcategory.class));
        verify(repository).flush();
    }

    @Test
    @DisplayName("create - rejects duplicate name with case-insensitivity under same category")
    void create_DuplicateName_CaseInsensitive_ThrowsResourceConflict() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("glass hardware")
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("glass hardware", 1)).thenReturn(true);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Subcategory already exists with name: 'glass hardware'");

        verify(repository, never()).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - trims whitespace and rejects whitespace variant duplicates")
    void create_DuplicateName_WhitespaceVariant_ThrowsResourceConflict() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("   Glass    Hardware   ")
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Glass Hardware", 1)).thenReturn(true);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Glass Hardware");

        verify(repository, never()).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - rejects duplicate slug under same category")
    void create_DuplicateSlug_UnderSameCategory_ThrowsResourceConflict() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Unique Name")
                .slug("existing-slug")
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Unique Name", 1)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("existing-slug", 1)).thenReturn(true);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Subcategory already exists with slug: 'existing-slug'");

        verify(repository, never()).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - allows same subcategory name under a different category")
    void create_SameNameDifferentCategory_Success() {
        Category otherCategory = Category.builder().categoryId(2).name("Hardware").slug("hardware").build();
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(2)
                .name("Standard Screws")
                .build();

        Subcategory savedSub = Subcategory.builder()
                .subcategoryId(25)
                .category(otherCategory)
                .name("Standard Screws")
                .slug("standard-screws")
                .active(true)
                .build();

        when(categoryRepository.findById(2)).thenReturn(Optional.of(otherCategory));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Standard Screws", 2)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("standard-screws", 2)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(savedSub);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("Standard Screws");
    }

    @Test
    @DisplayName("create - handles concurrent race conditions gracefully by catching DataIntegrityViolationException")
    void create_ConcurrentRaceCondition_ThrowsResourceConflict() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Glass Hardware")
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Glass Hardware", 1)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("glass-hardware", 1)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenThrow(new DataIntegrityViolationException("Duplicate entry '1-glass-hardware' for key 'uk_subcategories_category_slug'"));

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Subcategory already exists with name: 'Glass Hardware' or slug: 'glass-hardware'");
    }

    @Test
    @DisplayName("update - should update subcategory and delete old S3 image when replaced")
    void update_ReplacesOldS3Image() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("TMT Bars Updated")
                .slug("tmt-bars-updated")
                .imageUrl("https://storage/subcategories/new-tmt.jpg")
                .active(true)
                .sortOrder(2)
                .build();

        when(repository.findById(10)).thenReturn(Optional.of(subcategory));
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryIdAndSubcategoryIdNot("tmt-bars-updated", 1, 10)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcategoryResponse response = subcategoryService.update(10, request);

        assertThat(response).isNotNull();
        verify(s3ImageService).deleteImage("https://storage/subcategories/tmt.jpg");
        verify(repository).save(subcategory);
        verify(repository).flush();
    }

    @Test
    @DisplayName("delete - hard-deletes subcategory when no products or brands exist")
    void delete_NoDependencies_HardDeletes() {
        when(repository.findById(10)).thenReturn(Optional.of(subcategory));
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(0);
        when(brandRepository.countBySubcategory_SubcategoryId(10)).thenReturn(0);

        subcategoryService.delete(10);

        verify(repository).delete(subcategory);
        verify(s3ImageService).deleteImage("https://storage/subcategories/tmt.jpg");
    }

    @Test
    @DisplayName("delete - soft-deletes and deactivates subcategory when linked products exist")
    void delete_LinkedProducts_SoftDeletes() {
        when(repository.findById(10)).thenReturn(Optional.of(subcategory));
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(3);

        subcategoryService.delete(10);

        assertThat(subcategory.isActive()).isFalse();
        assertThat(subcategory.isVisibleOnWebsite()).isFalse();
        verify(repository).save(subcategory);
        verify(repository, never()).delete(any(Subcategory.class));
        verify(s3ImageService, never()).deleteImage(any());
    }

    @Test
    @DisplayName("delete - soft-deletes subcategory and cascades deactivation to brands when linked brands exist")
    void delete_LinkedBrands_SoftDeletesAndDeactivatesBrands() {
        Brand linkedBrand = Brand.builder().brandId(50).subcategory(subcategory).name("Tata Tiscon").active(true).build();

        when(repository.findById(10)).thenReturn(Optional.of(subcategory));
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(0);
        when(brandRepository.countBySubcategory_SubcategoryId(10)).thenReturn(1);
        when(brandRepository.findBySubcategory_SubcategoryIdOrderBySortOrderAsc(10)).thenReturn(List.of(linkedBrand));

        subcategoryService.delete(10);

        assertThat(subcategory.isActive()).isFalse();
        assertThat(subcategory.isVisibleOnWebsite()).isFalse();
        assertThat(linkedBrand.isActive()).isFalse();
        verify(repository).save(subcategory);
        verify(brandRepository).saveAll(List.of(linkedBrand));
        verify(repository, never()).delete(any(Subcategory.class));
    }

    @Test
    @DisplayName("delete - should throw ResourceNotFoundException when not found")
    void delete_NotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subcategoryService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create - should throw InvalidImageException and NOT save when imageUrl is a blob URL")
    void create_BlobUrl_ThrowsInvalidImageException_DoesNotSave() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Pipes")
                .imageUrl("blob:http://localhost:5173/58fca786-fa31-4b60-bb91-e03efe75662c")
                .build();

        Category category = Category.builder().categoryId(1).name("Civil").build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Pipes", 1)).thenReturn(false);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Blob URLs");

        verify(repository, never()).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - should throw InvalidImageException and NOT save when imageUrl is a base64 data URL")
    void create_DataUri_ThrowsInvalidImageException_DoesNotSave() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Wires")
                .imageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .build();

        Category category = Category.builder().categoryId(1).name("Civil").build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Wires", 1)).thenReturn(false);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Base64 data URLs");

        verify(repository, never()).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - should handle null imageUrl correctly and persist null")
    void create_NullImageUrl_Success() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Tiles")
                .imageUrl(null)
                .build();

        Category category = Category.builder().categoryId(1).name("Civil").build();
        Subcategory savedSub = Subcategory.builder()
                .subcategoryId(11)
                .category(category)
                .name("Tiles")
                .slug("tiles")
                .imageUrl(null)
                .active(true)
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Tiles", 1)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("tiles", 1)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(savedSub);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isNull();
        verify(repository).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - should normalize empty string imageUrl to null")
    void create_EmptyImageUrl_NormalizesToNull() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Bricks")
                .imageUrl("   ")
                .build();

        Category category = Category.builder().categoryId(1).name("Civil").build();
        Subcategory savedSub = Subcategory.builder()
                .subcategoryId(12)
                .category(category)
                .name("Bricks")
                .slug("bricks")
                .imageUrl(null)
                .active(true)
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Bricks", 1)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("bricks", 1)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(savedSub);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isNull();
        verify(repository).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("create - should persist valid S3 URL")
    void create_ValidS3Url_Persisted() {
        String s3Url = "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/subcategories/uuid-456.jpg";
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Gravel")
                .imageUrl(s3Url)
                .build();

        Category category = Category.builder().categoryId(1).name("Civil").build();
        Subcategory savedSub = Subcategory.builder()
                .subcategoryId(13)
                .category(category)
                .name("Gravel")
                .slug("gravel")
                .imageUrl(s3Url)
                .active(true)
                .build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndCategory_CategoryId("Gravel", 1)).thenReturn(false);
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryId("gravel", 1)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(savedSub);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo(s3Url);
        verify(repository).save(any(Subcategory.class));
    }

    @Test
    @DisplayName("update - should throw InvalidImageException and NOT save when updated imageUrl is a blob URL")
    void update_BlobUrl_ThrowsInvalidImageException_DoesNotSave() {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("TMT Updated")
                .imageUrl("blob:http://localhost:5173/bad-blob")
                .build();

        when(repository.findById(10)).thenReturn(Optional.of(subcategory));

        assertThatThrownBy(() -> subcategoryService.update(10, request))
                .isInstanceOf(com.example.project.customer.exception.InvalidImageException.class)
                .hasMessageContaining("Blob URLs");

        verify(repository, never()).save(any(Subcategory.class));
        verify(s3ImageService, never()).deleteImage(any());
    }

    @Test
    @DisplayName("update - should not call s3 delete if old imageUrl in database was a blob URL")
    void update_WhenOldImageIsBlobUrl_DoesNotCallS3Delete() {
        Category cat = Category.builder().categoryId(1).name("Civil").build();
        Subcategory subWithBlob = Subcategory.builder()
                .subcategoryId(14)
                .category(cat)
                .name("Old Sub")
                .slug("old-sub")
                .imageUrl("blob:http://localhost:5173/old-blob-id")
                .build();

        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Old Sub")
                .imageUrl("https://storage/subcategories/new-sub.jpg")
                .build();

        when(repository.findById(14)).thenReturn(Optional.of(subWithBlob));
        when(repository.existsBySlugIgnoreCaseAndCategory_CategoryIdAndSubcategoryIdNot("old-sub", 1, 14)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenAnswer(inv -> inv.getArgument(0));

        subcategoryService.update(14, request);

        // Crucial: S3 delete must NOT be called for the old blob URL!
        verify(s3ImageService, never()).deleteImage(org.mockito.ArgumentMatchers.contains("blob:"));
    }

    @Test
    @DisplayName("delete - should not call s3 delete if imageUrl in database is a blob URL")
    void delete_WhenImageIsBlobUrl_DoesNotCallS3Delete() {
        Category cat = Category.builder().categoryId(1).name("Civil").build();
        Subcategory subWithBlob = Subcategory.builder()
                .subcategoryId(15)
                .category(cat)
                .name("Blob Sub")
                .slug("blob-sub")
                .imageUrl("blob:http://localhost:5173/old-blob-id")
                .build();

        when(repository.findById(15)).thenReturn(Optional.of(subWithBlob));

        subcategoryService.delete(15);

        verify(repository).delete(subWithBlob);
        verify(s3ImageService, never()).deleteImage(any());
    }
}
