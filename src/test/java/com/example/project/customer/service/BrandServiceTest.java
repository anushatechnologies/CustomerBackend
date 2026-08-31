package com.example.project.customer.service;

import com.example.project.customer.dto.BrandRequest;
import com.example.project.customer.dto.BrandResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BrandRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository repository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Category category;
    private Subcategory subcategory;
    private Brand brand;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .name("Civil & Structural")
                .slug("civil-structural")
                .build();

        subcategory = Subcategory.builder()
                .subcategoryId(10)
                .category(category)
                .name("TMT Steel")
                .slug("tmt-steel")
                .build();

        brand = Brand.builder()
                .brandId(100)
                .subcategory(subcategory)
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .imageUrl("https://cdn.hinchmart.com/brands/tata_tiscon.png")
                .active(true)
                .sortOrder(1)
                .productCount(5)
                .build();
    }

    @Test
    @DisplayName("create - should save new brand")
    void create_Success() {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .imageUrl("https://cdn.hinchmart.com/brands/tata_tiscon.png")
                .active(true)
                .sortOrder(1)
                .build();

        when(subcategoryRepository.findById(10)).thenReturn(Optional.of(subcategory));
        when(repository.existsBySlugIgnoreCase("tata-tiscon")).thenReturn(false);
        when(repository.save(any(Brand.class))).thenReturn(brand);
        when(productRepository.countByBrand_BrandId(100)).thenReturn(5);

        BrandResponse response = brandService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Tata Tiscon");
        assertThat(response.getSubcategoryId()).isEqualTo(10);
        assertThat(response.getCategoryId()).isEqualTo(1);
        verify(repository).save(any(Brand.class));
    }

    @Test
    @DisplayName("create - should throw ResourceNotFoundException when subcategory not found")
    void create_SubcategoryNotFound() {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(99)
                .name("Tata Tiscon")
                .build();

        when(subcategoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create - should throw ResourceConflictException on duplicate slug")
    void create_DuplicateSlug() {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .build();

        when(subcategoryRepository.findById(10)).thenReturn(Optional.of(subcategory));
        when(repository.existsBySlugIgnoreCase("tata-tiscon")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(request))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("getById - should return brand when found")
    void getById_Success() {
        when(repository.findById(100)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrand_BrandId(100)).thenReturn(5);

        BrandResponse response = brandService.getById(100);

        assertThat(response).isNotNull();
        assertThat(response.getBrandId()).isEqualTo(100);
        assertThat(response.getName()).isEqualTo("Tata Tiscon");
    }

    @Test
    @DisplayName("getById - should throw ResourceNotFoundException when not found")
    void getById_NotFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAll - filter by subcategoryId and active")
    void getAll_FilterBySubcategoryAndActive() {
        when(repository.findBySubcategory_SubcategoryIdAndActiveOrderBySortOrderAsc(10, true))
                .thenReturn(List.of(brand));
        when(productRepository.countByBrand_BrandId(100)).thenReturn(5);

        List<BrandResponse> list = brandService.getAll(null, 10, true);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Tata Tiscon");
    }

    @Test
    @DisplayName("getAll - filter by categoryId")
    void getAll_FilterByCategoryId() {
        when(repository.findBySubcategory_Category_CategoryIdOrderBySortOrderAsc(1))
                .thenReturn(List.of(brand));
        when(productRepository.countByBrand_BrandId(100)).thenReturn(5);

        List<BrandResponse> list = brandService.getAll(1, null, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Tata Tiscon");
    }

    @Test
    @DisplayName("update - should update brand and delete old S3 image when replaced")
    void update_ReplacesOldS3Image() {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("Tata Tiscon Updated")
                .slug("tata-tiscon-updated")
                .imageUrl("https://cdn.hinchmart.com/brands/new_tata.png")
                .active(true)
                .sortOrder(2)
                .build();

        when(repository.findById(100)).thenReturn(Optional.of(brand));
        when(repository.existsBySlugIgnoreCaseAndBrandIdNot("tata-tiscon-updated", 100)).thenReturn(false);
        when(repository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.update(100, request);

        assertThat(response).isNotNull();
        verify(s3ImageService).deleteImage("https://cdn.hinchmart.com/brands/tata_tiscon.png");
        verify(repository).save(brand);
    }

    @Test
    @DisplayName("delete - should delete brand and clean up S3 image")
    void delete_CleansUpS3Image() {
        when(repository.findById(100)).thenReturn(Optional.of(brand));

        brandService.delete(100);

        verify(repository).delete(brand);
        verify(s3ImageService).deleteImage("https://cdn.hinchmart.com/brands/tata_tiscon.png");
    }

    @Test
    @DisplayName("delete - should throw ResourceNotFoundException when brand not found")
    void delete_NotFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.delete(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
