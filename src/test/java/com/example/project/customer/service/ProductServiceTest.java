package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @InjectMocks
    private ProductServiceImpl service;

    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        Subcategory subcategory = new Subcategory();
        subcategory.setSubcategoryId(5);
        product = new Product();
        product.setProductId(1);
        product.setSubcategory(subcategory);
        product.setTitle("Steel Rebar");
        product.setPrice(BigDecimal.TEN);
        product.setStockQty(10);
        product.setUnit("piece");
        request = new ProductRequest(5, "Steel Rebar", "Construction steel", BigDecimal.TEN,
                10, "piece", null, true);
    }

    @Test
    void createAlwaysStartsPendingAndInactive() {
        Subcategory subcategory = product.getSubcategory();
        when(subcategoryRepository.findById(5)).thenReturn(Optional.of(subcategory));
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(savedCaptor.capture());
        Product saved = savedCaptor.getValue();
        assertEquals(ApprovalStatus.PENDING, saved.getApprovalStatus());
        assertFalse(saved.isActive());
    }

    @Test
    void customerListQueriesOnlyApprovedActiveProducts() {
        when(repository.findByApprovalStatusAndActive(ApprovalStatus.APPROVED, true)).thenReturn(List.of(product));

        service.getAll();

        verify(repository).findByApprovalStatusAndActive(ApprovalStatus.APPROVED, true);
    }

    @Test
    void activationRequiresApproval() {
        when(repository.findById(1)).thenReturn(Optional.of(product));

        assertThrows(ResourceConflictException.class, () -> service.activate(1));
    }

    @Test
    void rejectPersistsReasonAndDisablesProduct() {
        product.setApprovalStatus(ApprovalStatus.APPROVED);
        product.setActive(true);
        when(repository.findById(1)).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.reject(1, new ProductRejectionRequest("Incomplete specifications"));

        assertEquals(ApprovalStatus.REJECTED, product.getApprovalStatus());
        assertEquals("Incomplete specifications", product.getRejectionReason());
        assertFalse(product.isActive());
    }
}