package com.example.project.customer.service;

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
        when(productRepository.countBySubcategory_Category_CategoryId(1)).thenReturn(0);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Steel & Rebars");
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("getById - should return category when found")
    void getById_Success() {
        when(repository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.countBySubcategory_Category_CategoryId(1)).thenReturn(10);

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
}
