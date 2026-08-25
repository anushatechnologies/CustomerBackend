package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Subcategory subcategory;
    private Product product;

    @BeforeEach
    void setUp() {
        subcategory = new Subcategory();
        subcategory.setSubcategoryId(1);
        subcategory.setName("Smartphones");

        product = new Product();
        product.setProductId(1);
        product.setSubcategory(subcategory);
        product.setTitle("Phone X");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQty(10);
        product.setUnit("piece");
        product.setActive(true);
    }

    @Test
    @DisplayName("Create product - Success")
    void create_Success() {
        ProductRequest request = new ProductRequest(1, "Phone X", "Desc", new BigDecimal("999.99"), 10, "piece", "img.jpg", true);

        when(subcategoryRepository.findById(1)).thenReturn(Optional.of(subcategory));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Phone X");
    }

    @Test
    @DisplayName("Get product by ID - Success")
    void getById_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get product by ID - Not Found")
    void getById_NotFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get all products - Success")
    void getAll_Success() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Update product - Success")
    void update_Success() {
        ProductRequest request = new ProductRequest(1, "Phone X Updated", "Desc", new BigDecimal("899.99"), 15, "piece", "img.jpg", true);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(subcategoryRepository.findById(1)).thenReturn(Optional.of(subcategory));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.update(1, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Delete product - Success")
    void delete_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.delete(1);

        verify(productRepository).delete(product);
    }
}
