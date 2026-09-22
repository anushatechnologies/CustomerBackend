package com.example.project.customer.service;

import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.EstimationStatus;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.UnauthorizedAccessException;
import com.example.project.customer.repository.EstimationItemRepository;
import com.example.project.customer.repository.EstimationRepository;
import com.example.project.customer.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimationServiceOwnershipTest {

    @Mock
    private DocumentProcessingService documentProcessingService;

    @Mock
    private AiRequirementService aiRequirementService;

    @Mock
    private RequirementMatchingService requirementMatchingService;

    @Mock
    private EstimationCalculationService estimationCalculationService;

    @Mock
    private QuotationPdfService quotationPdfService;

    @Mock
    private S3ImageService s3ImageService;

    @Mock
    private EstimationRepository estimationRepository;

    @Mock
    private EstimationItemRepository estimationItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private EstimationServiceImpl estimationService;

    private Estimation estimation;

    @BeforeEach
    void setUp() {
        estimation = Estimation.builder()
                .id(10L)
                .estimationNumber("EST-2026-0010")
                .customerId(101) // Owned by Customer 101
                .status(EstimationStatus.REQUIREMENTS_EXTRACTED)
                .build();
    }

    @Test
    @DisplayName("Should prevent Customer 102 from accessing Customer 101's estimation")
    void testOwnershipAccessDenied() {
        when(estimationRepository.findById(10L)).thenReturn(Optional.of(estimation));

        // Customer 102 attempts to access Customer 101's estimation
        assertThrows(UnauthorizedAccessException.class, () -> estimationService.getEstimation(102, 10L));
    }

    @Test
    @DisplayName("Should allow Customer 101 to access their own estimation")
    void testOwnershipAccessGranted() {
        when(estimationRepository.findById(10L)).thenReturn(Optional.of(estimation));

        var response = estimationService.getEstimation(101, 10L);
        assertEquals(10L, response.getEstimationId());
        assertEquals(101, response.getCustomerId());
    }

    @Test
    @DisplayName("Should resolve ambiguous item match and update estimation status")
    void testResolveItemMatch() {
        EstimationItem item = EstimationItem.builder()
                .id(1L)
                .estimation(estimation)
                .rawItemName("Cement")
                .matchStatus(MatchStatus.MULTIPLE_MATCHES)
                .build();
        estimation.addItem(item);

        Product selectedProduct = Product.builder()
                .productId(201)
                .title("Ultratech Cement")
                .price(BigDecimal.valueOf(410.0))
                .build();

        when(estimationRepository.findById(10L)).thenReturn(Optional.of(estimation));
        when(estimationItemRepository.findByIdAndEstimation_Id(1L, 10L)).thenReturn(Optional.of(item));
        when(productRepository.findById(201)).thenReturn(Optional.of(selectedProduct));
        when(estimationRepository.save(any(Estimation.class))).thenReturn(estimation);

        var response = estimationService.resolveItemMatch(101, 10L, 1L, 201);

        verify(requirementMatchingService).applySelectedProduct(item, selectedProduct);
        verify(estimationCalculationService).calculateItemPricing(item);
        verify(estimationCalculationService).recalculateEstimationTotals(estimation);
        assertEquals(MatchStatus.RESOLVED, item.getMatchStatus());
    }

    @Test
    @DisplayName("Should generate quotation and update status to QUOTATION_GENERATED")
    void testGenerateQuotation() {
        when(estimationRepository.findById(10L)).thenReturn(Optional.of(estimation));
        when(quotationPdfService.generateAndUploadQuotationPdf(estimation)).thenReturn("https://s3.hinchmart/quotation.pdf");
        when(estimationRepository.save(any(Estimation.class))).thenReturn(estimation);

        var response = estimationService.generateQuotation(101, 10L);

        assertEquals(EstimationStatus.QUOTATION_GENERATED, estimation.getStatus());
        verify(quotationPdfService).generateAndUploadQuotationPdf(estimation);
    }
}
