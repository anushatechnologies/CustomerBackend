package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
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
class SubcategoryServiceTest {

    @Mock
    private SubcategoryRepository repository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

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
        when(repository.existsBySlugIgnoreCase("tmt-bars")).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenReturn(subcategory);
        when(productRepository.countByBrand_Subcategory_SubcategoryId(10)).thenReturn(5);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("TMT Bars");
        verify(repository).save(any(Subcategory.class));
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
        when(repository.existsBySlugIgnoreCaseAndSubcategoryIdNot("tmt-bars-updated", 10)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcategoryResponse response = subcategoryService.update(10, request);

        assertThat(response).isNotNull();
        verify(s3ImageService).deleteImage("https://storage/subcategories/tmt.jpg");
        verify(repository).save(subcategory);
    }

    @Test
    @DisplayName("delete - should delete subcategory and clean up S3 image")
    void delete_CleansUpS3Image() {
        when(repository.findById(10)).thenReturn(Optional.of(subcategory));

        subcategoryService.delete(10);

        verify(repository).delete(subcategory);
        verify(s3ImageService).deleteImage("https://storage/subcategories/tmt.jpg");
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

        verify(repository, org.mockito.Mockito.never()).save(any(Subcategory.class));
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

        verify(repository, org.mockito.Mockito.never()).save(any(Subcategory.class));
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
        when(repository.existsBySlugIgnoreCase("tiles")).thenReturn(false);
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
        when(repository.existsBySlugIgnoreCase("bricks")).thenReturn(false);
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
        when(repository.existsBySlugIgnoreCase("gravel")).thenReturn(false);
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

        verify(repository, org.mockito.Mockito.never()).save(any(Subcategory.class));
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(any());
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
        when(repository.existsBySlugIgnoreCaseAndSubcategoryIdNot("old-sub", 14)).thenReturn(false);
        when(repository.save(any(Subcategory.class))).thenAnswer(inv -> inv.getArgument(0));

        subcategoryService.update(14, request);

        // Crucial: S3 delete must NOT be called for the old blob URL!
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(org.mockito.ArgumentMatchers.contains("blob:"));
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
        verify(s3ImageService, org.mockito.Mockito.never()).deleteImage(any());
    }
}
