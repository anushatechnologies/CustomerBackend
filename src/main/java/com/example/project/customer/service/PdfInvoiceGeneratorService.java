package com.example.project.customer.service;

import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfInvoiceGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateInvoicePdf(Order order, UserProfile userProfile, String invoiceNumber) {
        SimplePdfWriter pdf = new SimplePdfWriter();

        // Colors
        float[] navy = {0.043f, 0.12f, 0.25f};
        float[] lightGray = {0.95f, 0.96f, 0.98f};
        float[] borderGray = {0.82f, 0.84f, 0.88f};
        float[] textDark = {0.1f, 0.1f, 0.1f};
        float[] textMuted = {0.4f, 0.45f, 0.5f};

        // Header Background Banner
        pdf.fillRect(36, 765, 523, 45, navy[0], navy[1], navy[2]);
        pdf.drawText(46, 792, "/F2", 15, 1f, 1f, 1f, "HINCH MART - B2B INDUSTRIAL & CONSTRUCTION SUPPLIES");
        pdf.drawText(46, 775, "/F1", 9, 0.85f, 0.88f, 0.95f, "TAX INVOICE / BILL OF SUPPLY  |  ORIGINAL FOR RECIPIENT  |  GST COMPLIANT");

        // Top metadata card
        pdf.drawRect(36, 690, 523, 65, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.fillRect(37, 691, 521, 63, lightGray[0], lightGray[1], lightGray[2]);

        String invDate = (order.getCreatedAt() != null ? order.getCreatedAt() : java.time.LocalDateTime.now()).format(DATE_FORMATTER);
        pdf.drawText(46, 736, "/F2", 9, textDark[0], textDark[1], textDark[2], "Invoice No: " + escape(invoiceNumber));
        pdf.drawText(46, 722, "/F1", 9, textDark[0], textDark[1], textDark[2], "Invoice Date: " + invDate);
        pdf.drawText(46, 708, "/F1", 9, textDark[0], textDark[1], textDark[2], "Payment Mode: " + escape(order.getPaymentMethod() != null ? order.getPaymentMethod() : "ONLINE"));

        pdf.drawText(220, 736, "/F2", 9, textDark[0], textDark[1], textDark[2], "Order No: " + escape(order.getOrderNumber()));
        pdf.drawText(220, 722, "/F1", 9, textDark[0], textDark[1], textDark[2], "Order Status: " + escape(order.getOrderStatus()));
        pdf.drawText(220, 708, "/F1", 9, textDark[0], textDark[1], textDark[2], "PO Number: " + escape(order.getPoNumber() != null ? order.getPoNumber() : "N/A"));

        pdf.drawText(400, 736, "/F2", 9, textDark[0], textDark[1], textDark[2], "Place of Supply: Telangana (36)");
        pdf.drawText(400, 722, "/F1", 9, textDark[0], textDark[1], textDark[2], "State Code: 36");
        pdf.drawText(400, 708, "/F1", 9, textDark[0], textDark[1], textDark[2], "Reverse Charge: NO");

        // Supplier & Buyer details boxes
        // Left: Supplier Details
        pdf.drawRect(36, 595, 256, 85, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.fillRect(37, 661, 254, 18, 0.90f, 0.93f, 0.97f);
        pdf.drawText(44, 666, "/F2", 9, navy[0], navy[1], navy[2], "SUPPLIER / SELLER DETAILS");
        pdf.drawText(44, 647, "/F2", 9, textDark[0], textDark[1], textDark[2], "HinchMart B2B Commerce Pvt Ltd");
        pdf.drawText(44, 634, "/F1", 8.5f, textDark[0], textDark[1], textDark[2], "GSTIN: 36AAACH2026Q1Z1  |  PAN: AAACH2026Q");
        pdf.drawText(44, 621, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Knowledge City, HITEC City, Hyderabad");
        pdf.drawText(44, 608, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Telangana, India - 500081 | help@hinchmart.com");

        // Right: Buyer Details
        String buyerLegalName = userProfile != null && userProfile.getCompanyName() != null && !userProfile.getCompanyName().isBlank()
                ? userProfile.getCompanyName() : "Apex Infra Projects Pvt Ltd";
        String buyerGstin = userProfile != null && userProfile.getGstNumber() != null && !userProfile.getGstNumber().isBlank()
                ? userProfile.getGstNumber() : "36AAACT2727Q1ZW";
        String buyerAddress = order.getDeliveryLocation() != null && !order.getDeliveryLocation().isBlank()
                ? order.getDeliveryLocation() : "Site #12, Financial District, Nanakramguda, Hyderabad, TS - 500032";

        pdf.drawRect(303, 595, 256, 85, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.fillRect(304, 661, 254, 18, 0.90f, 0.93f, 0.97f);
        pdf.drawText(311, 666, "/F2", 9, navy[0], navy[1], navy[2], "BILLED TO / SHIPPED TO (BUYER)");
        pdf.drawText(311, 647, "/F2", 9, textDark[0], textDark[1], textDark[2], truncate(buyerLegalName, 34));
        pdf.drawText(311, 634, "/F1", 8.5f, textDark[0], textDark[1], textDark[2], "GSTIN: " + escape(buyerGstin));
        pdf.drawText(311, 621, "/F1", 8f, textDark[0], textDark[1], textDark[2], truncate(buyerAddress, 42));
        pdf.drawText(311, 608, "/F1", 8f, textDark[0], textDark[1], textDark[2], "State: Telangana (36) | Buyer Type: Enterprise B2B");

        // Table Header
        float tableTop = 575;
        pdf.fillRect(36, tableTop, 523, 18, navy[0], navy[1], navy[2]);
        pdf.drawText(42, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "S.N");
        pdf.drawText(65, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "ITEM DESCRIPTION");
        pdf.drawText(260, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "HSN");
        pdf.drawText(305, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "QTY");
        pdf.drawText(340, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "RATE (INR)");
        pdf.drawText(410, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "TAXABLE");
        pdf.drawText(465, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "GST");
        pdf.drawText(505, tableTop + 5, "/F2", 8f, 1f, 1f, 1f, "TOTAL (INR)");

        // Table Rows
        float y = tableTop - 16;
        List<OrderItem> items = order.getItems() != null ? order.getItems() : List.of();
        int sNo = 1;
        for (OrderItem item : items) {
            if (y < 280) break; // keep within page
            if (sNo % 2 == 0) {
                pdf.fillRect(36, y - 2, 523, 15, lightGray[0], lightGray[1], lightGray[2]);
            }
            pdf.drawLine(36, y - 2, 559, y - 2, borderGray[0], borderGray[1], borderGray[2], 0.5f);

            pdf.drawText(42, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], String.valueOf(sNo));
            pdf.drawText(65, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], truncate(item.getTitle(), 36));
            pdf.drawText(260, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], "721420");
            pdf.drawText(305, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], item.getQuantity() + " " + (item.getUnit() != null ? item.getUnit() : ""));
            pdf.drawText(340, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], formatCurrency(item.getUnitPrice()));
            pdf.drawText(410, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], formatCurrency(item.getLineTotal()));
            pdf.drawText(465, y + 2, "/F1", 7.5f, textDark[0], textDark[1], textDark[2], (item.getGstRate() != null ? item.getGstRate().intValue() : 18) + "%");
            BigDecimal lineWithGst = item.getLineTotal().add(item.getLineGst() != null ? item.getLineGst() : BigDecimal.ZERO);
            pdf.drawText(505, y + 2, "/F2", 7.5f, textDark[0], textDark[1], textDark[2], formatCurrency(lineWithGst));

            y -= 15;
            sNo++;
        }

        // Outer border for item table
        float tableBottom = Math.min(y, tableTop - 60);
        pdf.drawRect(36, tableBottom, 523, tableTop + 18 - tableBottom, borderGray[0], borderGray[1], borderGray[2], 0.8f);

        // Additional Charges & Tax summary block
        float summaryTop = tableBottom - 15;
        // Left Box: Bank Details & Terms
        pdf.drawRect(36, summaryTop - 110, 310, 110, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.fillRect(37, summaryTop - 16, 308, 15, 0.90f, 0.93f, 0.97f);
        pdf.drawText(44, summaryTop - 12, "/F2", 8f, navy[0], navy[1], navy[2], "BANK DETAILS FOR DIRECT NEFT / RTGS REMITTANCE");
        pdf.drawText(44, summaryTop - 30, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Bank Name: HDFC Bank Ltd   |   Branch: HITEC City Hyderabad");
        pdf.drawText(44, summaryTop - 42, "/F1", 8f, textDark[0], textDark[1], textDark[2], "A/C Name: HinchMart B2B Commerce Pvt Ltd");
        pdf.drawText(44, summaryTop - 54, "/F2", 8f, textDark[0], textDark[1], textDark[2], "A/C No: 50200088991122   |   IFSC Code: HDFC0001234");
        pdf.drawText(44, summaryTop - 70, "/F2", 8f, navy[0], navy[1], navy[2], "TERMS & CONDITIONS:");
        pdf.drawText(44, summaryTop - 82, "/F1", 7f, textMuted[0], textMuted[1], textMuted[2], "1. Certified test reports (MTC) supplied with industrial materials.");
        pdf.drawText(44, summaryTop - 92, "/F1", 7f, textMuted[0], textMuted[1], textMuted[2], "2. E-way bill applicable for consignments exceeding INR 50,000.");
        pdf.drawText(44, summaryTop - 102, "/F1", 7f, textMuted[0], textMuted[1], textMuted[2], "3. Interest @ 18% p.a. will be levied on overdue enterprise accounts.");

        // Right Box: Split Tax Calculation & Grand Total
        pdf.drawRect(355, summaryTop - 110, 204, 110, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.drawText(365, summaryTop - 12, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Taxable Amount:");
        pdf.drawText(480, summaryTop - 12, "/F2", 8f, textDark[0], textDark[1], textDark[2], formatCurrency(order.getTaxableAmount()));

        pdf.drawText(365, summaryTop - 25, "/F1", 8f, textDark[0], textDark[1], textDark[2], "CGST (9%):");
        pdf.drawText(480, summaryTop - 25, "/F1", 8f, textDark[0], textDark[1], textDark[2], formatCurrency(order.getCgst()));

        pdf.drawText(365, summaryTop - 38, "/F1", 8f, textDark[0], textDark[1], textDark[2], "SGST (9%):");
        pdf.drawText(480, summaryTop - 38, "/F1", 8f, textDark[0], textDark[1], textDark[2], formatCurrency(order.getSgst()));

        if (order.getFreightCharge() != null && order.getFreightCharge().compareTo(BigDecimal.ZERO) > 0) {
            pdf.drawText(365, summaryTop - 51, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Freight Charges:");
            pdf.drawText(480, summaryTop - 51, "/F1", 8f, textDark[0], textDark[1], textDark[2], formatCurrency(order.getFreightCharge()));
        }

        if (order.getCraneUnloadingCharge() != null && order.getCraneUnloadingCharge().compareTo(BigDecimal.ZERO) > 0) {
            pdf.drawText(365, summaryTop - 64, "/F1", 8f, textDark[0], textDark[1], textDark[2], "Crane Unloading:");
            pdf.drawText(480, summaryTop - 64, "/F1", 8f, textDark[0], textDark[1], textDark[2], formatCurrency(order.getCraneUnloadingCharge()));
        }

        // Grand Total Bar
        pdf.fillRect(356, summaryTop - 108, 202, 22, navy[0], navy[1], navy[2]);
        pdf.drawText(365, summaryTop - 95, "/F2", 9.5f, 1f, 1f, 1f, "GRAND TOTAL (INR):");
        pdf.drawText(470, summaryTop - 95, "/F2", 9.5f, 1f, 1f, 1f, formatCurrency(order.getTotalAmount()));

        // Signatory box
        float signTop = summaryTop - 125;
        pdf.drawText(36, signTop - 10, "/F2", 8f, textDark[0], textDark[1], textDark[2], "Amount in Words: INR " + formatCurrency(order.getTotalAmount()) + " Only");
        pdf.drawLine(360, signTop - 25, 550, signTop - 25, borderGray[0], borderGray[1], borderGray[2], 0.8f);
        pdf.drawText(375, signTop - 36, "/F2", 8f, navy[0], navy[1], navy[2], "For HinchMart B2B Commerce Pvt Ltd");
        pdf.drawText(405, signTop - 47, "/F1", 7.5f, textMuted[0], textMuted[1], textMuted[2], "Authorized Signatory (Digital)");

        // Bottom Footer
        pdf.fillRect(36, 40, 523, 18, lightGray[0], lightGray[1], lightGray[2]);
        pdf.drawText(130, 46, "/F1", 7f, textMuted[0], textMuted[1], textMuted[2],
                "This is a digitally generated legal Tax Invoice under Section 31 of CGST Act, 2017. No physical signature required.");

        return pdf.build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format(java.util.Locale.US, "%,.2f", amount);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        String clean = text.replace("\n", " ").replace("\r", " ").trim();
        return clean.length() > maxLen ? clean.substring(0, maxLen - 3) + "..." : clean;
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /**
     * Minimal pure-Java PDF-1.4 builder class creating conforming PDF files.
     */
    static class SimplePdfWriter {
        private final StringBuilder streamContent = new StringBuilder();

        public void drawText(float x, float y, String font, float size, float r, float g, float b, String text) {
            streamContent.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b));
            streamContent.append("BT\n");
            streamContent.append(font).append(" ").append(String.format(java.util.Locale.US, "%.1f", size)).append(" Tf\n");
            streamContent.append(String.format(java.util.Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x, y));
            streamContent.append("(").append(escapePdfText(text)).append(") Tj\n");
            streamContent.append("ET\n");
        }

        public void fillRect(float x, float y, float w, float h, float r, float g, float b) {
            streamContent.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b));
            streamContent.append(String.format(java.util.Locale.US, "%.2f %.2f %.2f %.2f re f\n", x, y, w, h));
        }

        public void drawRect(float x, float y, float w, float h, float r, float g, float b, float lineWidth) {
            streamContent.append(String.format(java.util.Locale.US, "%.2f w\n", lineWidth));
            streamContent.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f RG\n", r, g, b));
            streamContent.append(String.format(java.util.Locale.US, "%.2f %.2f %.2f %.2f re s\n", x, y, w, h));
        }

        public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float lineWidth) {
            streamContent.append(String.format(java.util.Locale.US, "%.2f w\n", lineWidth));
            streamContent.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f RG\n", r, g, b));
            streamContent.append(String.format(java.util.Locale.US, "%.2f %.2f m %.2f %.2f l S\n", x1, y1, x2, y2));
        }

        private String escapePdfText(String text) {
            if (text == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == '(' || c == ')' || c == '\\') {
                    sb.append('\\').append(c);
                } else if (c >= 32 && c <= 126) {
                    sb.append(c);
                } else {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }

        public byte[] build() {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                List<Integer> offsets = new ArrayList<>();

                // PDF Header
                write(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

                // Obj 1: Catalog
                offsets.add(out.size());
                write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

                // Obj 2: Pages
                offsets.add(out.size());
                write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

                // Obj 3: Page (A4: 595.28 x 841.89 pt)
                offsets.add(out.size());
                write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

                // Obj 4: Contents
                byte[] contentBytes = streamContent.toString().getBytes(StandardCharsets.US_ASCII);
                offsets.add(out.size());
                write(out, "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
                out.write(contentBytes);
                write(out, "\nendstream\nendobj\n");

                // Obj 5: Font F1 (Helvetica)
                offsets.add(out.size());
                write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

                // Obj 6: Font F2 (Helvetica-Bold)
                offsets.add(out.size());
                write(out, "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

                // Cross-Reference Table
                int startXref = out.size();
                write(out, "xref\n0 " + (offsets.size() + 1) + "\n");
                write(out, "0000000000 65535 f \n");
                for (int offset : offsets) {
                    write(out, String.format(java.util.Locale.US, "%010d 00000 n \n", offset));
                }

                // Trailer
                write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n");
                write(out, "startxref\n" + startXref + "\n%%EOF\n");

                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to render PDF invoice", e);
            }
        }

        private void write(ByteArrayOutputStream out, String s) throws IOException {
            out.write(s.getBytes(StandardCharsets.US_ASCII));
        }
    }
}
