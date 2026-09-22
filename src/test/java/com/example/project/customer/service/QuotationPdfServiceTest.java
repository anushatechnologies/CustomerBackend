package com.example.project.customer.service;

import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.EstimationStatus;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationPdfServiceTest {

    @Mock
    private S3ImageService s3ImageService;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private QuotationPdfServiceImpl quotationPdfService;

    private Estimation estimation;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .customerId(101)
                .name("Rajesh Sharma")
                .email("rajesh@apexbldrs.com")
                .phone("9876543210")
                .build();

        Product product = Product.builder()
                .productId(1)
                .title("Ultratech OPC 53 Grade Cement")
                .sku("CEMT-ULT-53")
                .price(BigDecimal.valueOf(400.0))
                .unit("Bags")
                .build();

        EstimationItem item = EstimationItem.builder()
                .id(1L)
                .rawItemName("OPC Cement 53 Grade")
                .requestedQuantity(BigDecimal.valueOf(100))
                .requestedUnit("Bags")
                .matchedProduct(product)
                .unitPrice(BigDecimal.valueOf(400.0))
                .gstRate(BigDecimal.valueOf(18.0))
                .lineSubtotal(new BigDecimal("40000.00"))
                .lineTax(new BigDecimal("7200.00"))
                .lineTotal(new BigDecimal("47200.00"))
                .matchStatus(MatchStatus.MATCHED)
                .build();

        estimation = Estimation.builder()
                .id(50L)
                .estimationNumber("EST-20260922-A1B2C3")
                .customerId(101)
                .originalFileName("site-bill.pdf")
                .subtotal(new BigDecimal("40000.00"))
                .taxAmount(new BigDecimal("7200.00"))
                .discountAmount(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("47200.00"))
                .status(EstimationStatus.RESOLVED)
                .build();

        estimation.addItem(item);

        when(customerRepository.findById(101)).thenReturn(Optional.of(customer));
    }

    @Test
    @DisplayName("Should generate valid Quotation PDF document bytes")
    void testGenerateQuotationPdf() {
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(estimation);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        // Standard PDF magic byte header check: %PDF
        String header = new String(pdfBytes, 0, 4);
        assertTrue(header.startsWith("%PDF"), "Output must start with PDF header signature %PDF");
    }

    @Test
    @DisplayName("Should generate and upload Quotation PDF to S3")
    void testGenerateAndUploadQuotationPdf() {
        ImageUploadResponse mockResponse = ImageUploadResponse.builder()
                .imageKey("estimations/101/50/quotation.pdf")
                .fileUrl("https://s3.ap-south-2.amazonaws.com/hinchmart/estimations/101/50/quotation.pdf")
                .build();

        when(s3ImageService.uploadBytesToKey(any(byte[].class), anyString(), anyString())).thenReturn(mockResponse);

        String pdfUrl = quotationPdfService.generateAndUploadQuotationPdf(estimation);

        assertNotNull(pdfUrl);
        assertTrue(pdfUrl.contains("quotation.pdf"));
        assertNotNull(estimation.getQuotationPdfKey());
        assertTrue(estimation.getQuotationPdfKey().contains("estimations/101/50/quotation.pdf"));
    }
}
