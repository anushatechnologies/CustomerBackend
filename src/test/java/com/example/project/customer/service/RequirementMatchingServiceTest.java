package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.CandidateProductSummary;
import com.example.project.customer.dto.estimation.ExtractedRequirementItem;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirementMatchingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RequirementMatchingServiceImpl matchingService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        Brand brand = Brand.builder().name("Ultratech").build();

        product1 = Product.builder()
                .productId(101)
                .title("Ultratech OPC 53 Grade Cement")
                .price(BigDecimal.valueOf(420.0))
                .unit("Bags")
                .stockQty(5000)
                .active(true)
                .brand(brand)
                .build();

        product2 = Product.builder()
                .productId(102)
                .title("Ultratech PPC Weather Plus Cement")
                .price(BigDecimal.valueOf(390.0))
                .unit("Bags")
                .stockQty(2500)
                .active(true)
                .brand(brand)
                .build();
    }

    @Test
    @DisplayName("Should match exactly when one high confidence product is found")
    void testExactMatch() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(product1));

        EstimationItem item = new EstimationItem();
        ExtractedRequirementItem req = ExtractedRequirementItem.builder()
                .name("Ultratech OPC 53 Grade Cement")
                .quantity(BigDecimal.valueOf(100))
                .unit("Bags")
                .brand("Ultratech")
                .build();

        matchingService.matchRequirementItem(item, req);

        assertEquals(MatchStatus.MATCHED, item.getMatchStatus());
        assertNotNull(item.getMatchedProduct());
        assertEquals(101, item.getMatchedProduct().getProductId());
        assertEquals(BigDecimal.valueOf(420.0), item.getUnitPrice());
        assertTrue(item.getIsAvailable());
    }

    @Test
    @DisplayName("Should flag MULTIPLE_MATCHES when multiple products closely match")
    void testMultipleMatches() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(product1, product2));

        EstimationItem item = new EstimationItem();
        ExtractedRequirementItem req = ExtractedRequirementItem.builder()
                .name("Ultratech Cement")
                .quantity(BigDecimal.valueOf(200))
                .unit("Bags")
                .brand("Ultratech")
                .build();

        matchingService.matchRequirementItem(item, req);

        assertEquals(MatchStatus.MULTIPLE_MATCHES, item.getMatchStatus());
        assertNotNull(item.getCandidateProductIds());
        assertTrue(item.getCandidateProductIds().contains("101") || item.getCandidateProductIds().contains("102"));
    }

    @Test
    @DisplayName("Should set NOT_FOUND when no catalog products match")
    void testProductNotFound() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        EstimationItem item = new EstimationItem();
        ExtractedRequirementItem req = ExtractedRequirementItem.builder()
                .name("Hyper-Quantum Nanomaterial 9000")
                .quantity(BigDecimal.valueOf(5))
                .unit("Pieces")
                .build();

        matchingService.matchRequirementItem(item, req);

        assertEquals(MatchStatus.NOT_FOUND, item.getMatchStatus());
        assertFalse(item.getIsAvailable());
    }

    @Test
    @DisplayName("Should convert product to CandidateProductSummary correctly")
    void testToCandidateSummary() {
        CandidateProductSummary summary = matchingService.toCandidateSummary(product1);

        assertNotNull(summary);
        assertEquals(101, summary.getProductId());
        assertEquals("Ultratech OPC 53 Grade Cement", summary.getTitle());
        assertEquals("Ultratech", summary.getBrandName());
        assertEquals(BigDecimal.valueOf(420.0), summary.getPrice());
        assertTrue(summary.getIsAvailable());
    }
}
