package com.example.project.customer.service;

import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfInvoiceGeneratorServiceTest {

    private final PdfInvoiceGeneratorService generator = new PdfInvoiceGeneratorService();

    @Test
    @DisplayName("generateInvoicePdf should produce valid PDF-1.4 binary bytes containing B2B tax details")
    void generateInvoicePdf_Success() {
        UserProfile user = UserProfile.builder()
                .id(101)
                .fullName("John Doe")
                .companyName("Skyline Infra Ventures Ltd")
                .gstNumber("36AAACT2727Q1ZW")
                .panNumber("AAACT2727Q")
                .build();

        OrderItem item1 = OrderItem.builder()
                .orderItemId(1)
                .title("Tata Tiscon 550D TMT Rebar 12mm")
                .quantity(100)
                .unit("Pieces")
                .unitPrice(new BigDecimal("550.00"))
                .lineTotal(new BigDecimal("55000.00"))
                .gstRate(new BigDecimal("18.00"))
                .lineGst(new BigDecimal("9900.00"))
                .build();

        Order order = Order.builder()
                .orderId(1)
                .orderNumber("ORD-20260904-001")
                .userId(101)
                .deliveryLocation("Sector 5, Financial District, Hyderabad")
                .subtotal(new BigDecimal("55000.00"))
                .taxableAmount(new BigDecimal("55000.00"))
                .cgst(new BigDecimal("4950.00"))
                .sgst(new BigDecimal("4950.00"))
                .igst(BigDecimal.ZERO)
                .totalGst(new BigDecimal("9900.00"))
                .freightCharge(new BigDecimal("1500.00"))
                .craneUnloadingCharge(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("67400.00"))
                .paymentMethod("ONLINE")
                .paymentStatus("PAID")
                .orderStatus("CONFIRMED")
                .poNumber("PO-2026-99")
                .createdAt(LocalDateTime.now())
                .items(List.of(item1))
                .build();

        byte[] pdfBytes = generator.generateInvoicePdf(order, user, "INV-2026-000001");

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        String pdfString = new String(pdfBytes, StandardCharsets.US_ASCII);
        // Standard PDF-1.4 header
        assertThat(pdfString).startsWith("%PDF-1.4");
        // Standard EOF trailer
        assertThat(pdfString).contains("%%EOF");
        // Check objects
        assertThat(pdfString).contains("/Type /Catalog");
        assertThat(pdfString).contains("/Type /Pages");
        assertThat(pdfString).contains("/Type /Page");
        // Content strings (with escaped formatting)
        assertThat(pdfString).contains("36AAACH2026Q1Z1"); // Seller GSTIN
        assertThat(pdfString).contains("36AAACT2727Q1ZW"); // Buyer GSTIN
        assertThat(pdfString).contains("INV-2026-000001"); // Invoice Number
        assertThat(pdfString).contains("ORD-20260904-001"); // Order Number
        assertThat(pdfString).contains("HINCH MART");
        assertThat(pdfString).contains("TAX INVOICE");
    }
}
