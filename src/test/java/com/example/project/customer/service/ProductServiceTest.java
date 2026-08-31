package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.StockQuantityUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private S3ImageService s3ImageService;

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

        request = ProductRequest.builder()
                .subcategoryId(5)
                .title("Steel Rebar")
                .description("Construction steel")
                .price(BigDecimal.TEN)
                .stockQty(10)
                .unit("piece")
                .imageUrl(null)
                .active(true)
                .build();
    }

    @Test
        void createAlwaysStartsPendingAndInactive() {
        Subcategory subcategory = product.getSubcategory();

        when(subcategoryRepository.findById(5))
                .thenReturn(Optional.of(subcategory));

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Product> savedCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(repository).save(savedCaptor.capture());

        Product saved = savedCaptor.getValue();

        assertEquals(
                ApprovalStatus.PENDING,
                saved.getApprovalStatus()
        );

        assertFalse(saved.isActive());
    }

    @Test
    void customerListQueriesOnlyApprovedActiveProducts() {
        product.setApprovalStatus(ApprovalStatus.APPROVED);
        product.setActive(true);
        Page<Product> page = new PageImpl<>(List.of(product));

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        var response = service.getAll(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "newest",
                1,
                20
        );

        verify(repository).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("Steel Rebar", response.getData().get(0).getTitle());
    }

    @Test
    void activationRequiresApproval() {
        when(repository.findById(1))
                .thenReturn(Optional.of(product));

        assertThrows(
                ResourceConflictException.class,
                () -> service.activate(1)
        );
    }

    @Test
    void updateStockQuantityAddsRequestedQuantityToCurrentStock() {
        StockQuantityUpdateRequest stockRequest = new StockQuantityUpdateRequest();
        stockRequest.setStockQty(25);

        when(repository.findByIdForStockUpdate(1)).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        var response = service.updateStockQuantity(1, stockRequest);

        assertEquals(35, product.getStockQty());
        assertEquals(35, response.getStockQty());
        verify(repository).save(product);
    }

    @Test
    void rejectPersistsReasonAndDisablesProduct() {

        product.setApprovalStatus(ApprovalStatus.APPROVED);
        product.setActive(true);

        when(repository.findById(1))
                .thenReturn(Optional.of(product));

        when(repository.save(product))
                .thenReturn(product);

        service.reject(
                1,
                new ProductRejectionRequest(
                        "Incomplete specifications"
                )
        );

        assertEquals(
                ApprovalStatus.REJECTED,
                product.getApprovalStatus()
        );

        assertEquals(
                "Incomplete specifications",
                product.getRejectionReason()
        );

        assertFalse(product.isActive());
    }

    @Test
    void deleteCleansUpMainAndGalleryS3Images() {
        product.setImageUrl("https://s3/products/main.jpg");
        product.setImages(List.of("https://s3/products/main.jpg", "https://s3/products/g1.jpg", "https://s3/products/g2.jpg"));

        when(repository.findById(1)).thenReturn(Optional.of(product));

        service.delete(1);

        verify(repository).delete(product);
        verify(s3ImageService).deleteImage("https://s3/products/main.jpg");
        verify(s3ImageService).deleteImage("https://s3/products/g1.jpg");
        verify(s3ImageService).deleteImage("https://s3/products/g2.jpg");
    }

    @Test
    void updateCleansUpRemovedS3Images() {
        product.setImageUrl("https://s3/products/old-main.jpg");
        product.setImages(List.of("https://s3/products/g1.jpg", "https://s3/products/g2-removed.jpg"));
        product.setApprovalStatus(ApprovalStatus.APPROVED);

        request.setImageUrl("https://s3/products/new-main.jpg");
        request.setImages(List.of("https://s3/products/g1.jpg", "https://s3/products/g3-new.jpg"));

        when(repository.findById(1)).thenReturn(Optional.of(product));
        when(subcategoryRepository.findById(5)).thenReturn(Optional.of(product.getSubcategory()));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(1, request);

        // old-main.jpg was replaced and not in new gallery, so it should be deleted
        verify(s3ImageService).deleteImage("https://s3/products/old-main.jpg");
        // g2-removed.jpg was in old gallery but not in new gallery, so it should be deleted
        verify(s3ImageService).deleteImage("https://s3/products/g2-removed.jpg");
    }
}
