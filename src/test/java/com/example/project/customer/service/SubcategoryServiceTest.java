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
        when(productRepository.countBySubcategory_SubcategoryId(10)).thenReturn(5);

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
}
