package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcategoryServiceTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SubcategoryServiceImpl subcategoryService;

    private Category category;
    private Subcategory subcategory;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1);
        category.setName("Electronics");

        subcategory = new Subcategory();
        subcategory.setSubcategoryId(1);
        subcategory.setCategory(category);
        subcategory.setName("Laptops");
        subcategory.setSlug("laptops");
        subcategory.setImageUrl("laptop.png");
        subcategory.setActive(true);
        subcategory.setSortOrder(1);
    }

    @Test
    @DisplayName("Create subcategory - Success")
    void create_Success() {
        SubcategoryRequest request = new SubcategoryRequest(1, "Laptops", "laptops", "laptop.png", true, 1);

        when(subcategoryRepository.existsBySlugIgnoreCase("laptops")).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(subcategoryRepository.save(any(Subcategory.class))).thenReturn(subcategory);

        SubcategoryResponse response = subcategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Laptops");
    }

    @Test
    @DisplayName("Create subcategory - Conflict")
    void create_Conflict() {
        SubcategoryRequest request = new SubcategoryRequest(1, "Laptops", "laptops", "laptop.png", true, 1);

        when(subcategoryRepository.existsBySlugIgnoreCase("laptops")).thenReturn(true);

        assertThatThrownBy(() -> subcategoryService.create(request))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("Get subcategory by ID - Success")
    void getById_Success() {
        when(subcategoryRepository.findById(1)).thenReturn(Optional.of(subcategory));

        SubcategoryResponse response = subcategoryService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.subcategoryId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get subcategory by ID - Not Found")
    void getById_NotFound() {
        when(subcategoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subcategoryService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get all subcategories - Success")
    void getAll_Success() {
        when(subcategoryRepository.findAll()).thenReturn(List.of(subcategory));

        List<SubcategoryResponse> responses = subcategoryService.getAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Update subcategory - Success")
    void update_Success() {
        SubcategoryRequest request = new SubcategoryRequest(1, "Laptops Pro", "laptops-pro", "laptop.png", true, 1);

        when(subcategoryRepository.findById(1)).thenReturn(Optional.of(subcategory));
        when(subcategoryRepository.existsBySlugIgnoreCaseAndSubcategoryIdNot("laptops-pro", 1)).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(subcategoryRepository.save(any(Subcategory.class))).thenReturn(subcategory);

        SubcategoryResponse response = subcategoryService.update(1, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Delete subcategory - Success")
    void delete_Success() {
        when(subcategoryRepository.findById(1)).thenReturn(Optional.of(subcategory));

        subcategoryService.delete(1);

        verify(subcategoryRepository).delete(subcategory);
    }
}
