package com.example.project.customer.service;

import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1);
        category.setName("Electronics");
        category.setSlug("electronics");
        category.setImageUrl("img.jpg");
        category.setActive(true);
        category.setSortOrder(1);
    }

    @Test
    @DisplayName("Create category - Success")
    void create_Success() {
        CategoryRequest request = new CategoryRequest("Electronics", "electronics", "img.jpg", true, 1);

        when(categoryRepository.existsBySlugIgnoreCase("electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Create category - Conflict")
    void create_Conflict() {
        CategoryRequest request = new CategoryRequest("Electronics", "electronics", "img.jpg", true, 1);

        when(categoryRepository.existsBySlugIgnoreCase("electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("Get category by ID - Success")
    void getById_Success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get category by ID - Not Found")
    void getById_NotFound() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get all categories - Success")
    void getAll_Success() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.getAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Update category - Success")
    void update_Success() {
        CategoryRequest request = new CategoryRequest("Electronics Updated", "electronics-updated", "img.jpg", true, 1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugIgnoreCaseAndCategoryIdNot("electronics-updated", 1)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.update(1, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Delete category - Success")
    void delete_Success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        categoryService.delete(1);

        verify(categoryRepository).delete(category);
    }
}
