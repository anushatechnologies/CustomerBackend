package com.example.project.customer.service;

import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.exception.DocumentProcessingException;
import com.example.project.customer.repository.CustomerRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationPdfServiceImpl implements QuotationPdfService {

    private final S3ImageService s3ImageService;
    private final CustomerRepository customerRepository;

    @Value("${hinchmart.estimation.quote-validity-days:15}")
    private int validityDays;

    private static final Color BRAND_PRIMARY = new Color(15, 23, 42);   // Slate 900
    private static final Color BRAND_ACCENT = new Color(37, 99, 235);   // Blue 600
    private static final Color BG_LIGHT = new Color(248, 250, 252);     // Slate 50
    private static final Color BG_HEADER = new Color(241, 245, 249);    // Slate 100
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // Slate 200
    private static final Color TEXT_MUTED = new Color(100, 116, 139);   // Slate 500

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_PRIMARY);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BRAND_PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.DARK_GRAY);
    private static final Font FONT_TOTAL_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_ACCENT);
    private static final Font FONT_TOTAL_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_ACCENT);

    @Override
    public byte[] generateQuotationPdf(Estimation estimation) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Company Header
            addHeaderBlock(document, estimation);

            // 2. Customer & Metadata Info
            addCustomerMetadataBlock(document, estimation);

            // 3. Itemized Products Table
            addItemsTable(document, estimation);

            // 4. Totals & Calculation Summary
            addTotalsSummary(document, estimation);

            // 5. Terms, Conditions & Notes
            addTermsAndConditions(document, estimation);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Quotation PDF for estimation: {}", estimation.getEstimationNumber(), e);
            throw new DocumentProcessingException("Failed to generate Quotation PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateAndUploadQuotationPdf(Estimation estimation) {
        byte[] pdfBytes = generateQuotationPdf(estimation);
        String s3Key = "estimations/" + estimation.getCustomerId() + "/" + estimation.getId() + "/quotation.pdf";
        ImageUploadResponse uploadResponse = s3ImageService.uploadBytesToKey(pdfBytes, s3Key, "application/pdf");

        estimation.setQuotationPdfKey(uploadResponse.getImageKey());
        estimation.setQuotationPdfUrl(uploadResponse.getFileUrl());
        return uploadResponse.getFileUrl();
    }

    private void addHeaderBlock(Document doc, Estimation estimation) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});
        header.setSpacingAfter(15);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("HINCHMART", FONT_TITLE));
        left.addElement(new Paragraph("B2B Construction & Building Materials Marketplace", FONT_SUBTITLE));
        left.addElement(new Paragraph("GSTIN: 36AAACH1234F1Z8 | support@hinchmart.com", FONT_SUBTITLE));
        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph("ESTIMATION QUOTATION", FONT_SECTION);
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);

        Paragraph estNo = new Paragraph("Ref: " + estimation.getEstimationNumber(), FONT_BODY_BOLD);
        estNo.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(estNo);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Paragraph date = new Paragraph("Date: " + LocalDateTime.now().format(dtf), FONT_BODY);
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(date);

        Paragraph valid = new Paragraph("Valid Until: " + LocalDateTime.now().plusDays(validityDays).format(dtf), FONT_SUBTITLE);
        valid.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(valid);

        header.addCell(right);
        doc.add(header);
    }

    private void addCustomerMetadataBlock(Document doc, Estimation estimation) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingAfter(15);

        String customerName = "Customer ID: " + estimation.getCustomerId();
        String email = "-";
        String phone = "-";

        Customer c = customerRepository.findById(estimation.getCustomerId()).orElse(null);
        if (c != null) {
            if (c.getName() != null) customerName = c.getName();
            if (c.getEmail() != null) email = c.getEmail();
            if (c.getPhone() != null) phone = c.getPhone();
        }

        PdfPCell customerCell = new PdfPCell();
        customerCell.setBackgroundColor(BG_LIGHT);
        customerCell.setBorderColor(BORDER_COLOR);
        customerCell.setPadding(8);
        customerCell.addElement(new Paragraph("CLIENT / BUYER DETAILS:", FONT_TH));
        customerCell.addElement(new Paragraph(customerName, FONT_BODY_BOLD));
        customerCell.addElement(new Paragraph("Phone: " + phone + " | Email: " + email, FONT_BODY));
        customerCell.addElement(new Paragraph("Account Type: B2B Verified Buyer", FONT_SUBTITLE));
        table.addCell(customerCell);

        PdfPCell docCell = new PdfPCell();
        docCell.setBackgroundColor(BG_LIGHT);
        docCell.setBorderColor(BORDER_COLOR);
        docCell.setPadding(8);
        docCell.addElement(new Paragraph("DOCUMENT REFERENCE:", FONT_TH));
        docCell.addElement(new Paragraph("Source File: " + (estimation.getOriginalFileName() != null ? estimation.getOriginalFileName() : "Requirement Upload"), FONT_BODY));
        docCell.addElement(new Paragraph("Status: " + estimation.getStatus().name(), FONT_BODY_BOLD));
        docCell.addElement(new Paragraph("Payment Terms: Standard Advance / Net 15 on Approved Credit", FONT_BODY));
        docCell.addElement(new Paragraph("Delivery: Direct Transit to Project Site (Heavy Vehicle Access Req.)", FONT_SUBTITLE));
        table.addCell(docCell);

        doc.add(table);
    }

    private void addItemsTable(Document doc, Estimation estimation) throws Exception {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 26, 20, 10, 11, 10, 8, 10});
        table.setSpacingAfter(15);

        String[] headers = {"#", "Requested Item & Specs", "Matched Catalog Product", "Qty", "Unit Price", "Discount/Tier", "GST", "Total"};
        for (String h : headers) {
            PdfPCell th = new PdfPCell(new Phrase(h, FONT_TH));
            th.setBackgroundColor(BG_HEADER);
            th.setBorderColor(BORDER_COLOR);
            th.setPadding(6);
            th.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(th);
        }

        int index = 1;
        if (estimation.getItems() != null) {
            for (EstimationItem item : estimation.getItems()) {
                Color rowBg = (index % 2 == 0) ? BG_LIGHT : Color.WHITE;

                // 1. #
                table.addCell(createCell(String.valueOf(index++), FONT_BODY, rowBg, Element.ALIGN_CENTER));

                // 2. Requested item & specs
                String reqDesc = item.getRawItemName();
                if (item.getSpecifications() != null && !item.getSpecifications().isBlank()) {
                    reqDesc += "\n[" + item.getSpecifications() + "]";
                }
                table.addCell(createCell(reqDesc, FONT_BODY, rowBg, Element.ALIGN_LEFT));

                // 3. Matched product
                String matchedDesc;
                if (item.getMatchedProduct() != null) {
                    matchedDesc = item.getMatchedProduct().getTitle();
                    if (item.getMatchedProduct().getSku() != null) {
                        matchedDesc += " (" + item.getMatchedProduct().getSku() + ")";
                    }
                } else if (item.getMatchStatus() == MatchStatus.NOT_FOUND) {
                    matchedDesc = "[NOT FOUND IN CATALOG]";
                } else {
                    matchedDesc = "[MULTIPLE MATCHES]";
                }
                table.addCell(createCell(matchedDesc, FONT_BODY_BOLD, rowBg, Element.ALIGN_LEFT));

                // 4. Quantity & Unit
                String qtyStr = (item.getRequestedQuantity() != null ? item.getRequestedQuantity().stripTrailingZeros().toPlainString() : "1")
                        + " " + (item.getRequestedUnit() != null ? item.getRequestedUnit() : "");
                table.addCell(createCell(qtyStr, FONT_BODY, rowBg, Element.ALIGN_CENTER));

                // 5. Unit Price
                String priceStr = item.getUnitPrice() != null ? formatMoney(item.getUnitPrice()) : "-";
                table.addCell(createCell(priceStr, FONT_BODY, rowBg, Element.ALIGN_RIGHT));

                // 6. Discount / Tier
                String tierStr = item.getAppliedTierDescription() != null ? item.getAppliedTierDescription() : "Standard";
                table.addCell(createCell(tierStr, FONT_SUBTITLE, rowBg, Element.ALIGN_CENTER));

                // 7. GST %
                String gstStr = item.getGstRate() != null ? item.getGstRate().stripTrailingZeros().toPlainString() + "%" : "18%";
                table.addCell(createCell(gstStr, FONT_BODY, rowBg, Element.ALIGN_CENTER));

                // 8. Line Total
                String totalStr = item.getLineTotal() != null ? formatMoney(item.getLineTotal()) : "-";
                table.addCell(createCell(totalStr, FONT_BODY_BOLD, rowBg, Element.ALIGN_RIGHT));
            }
        }

        doc.add(table);
    }

    private void addTotalsSummary(Document doc, Estimation estimation) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 40});
        table.setSpacingAfter(15);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("Pricing & Volume Details:", FONT_TH));
        left.addElement(new Paragraph("• Wholesale bulk pricing tiers automatically applied for eligible quantities.", FONT_SUBTITLE));
        left.addElement(new Paragraph("• Taxes calculated according to applicable GST schedules.", FONT_SUBTITLE));
        left.addElement(new Paragraph("• Standard transit delivery times: 24 to 72 hours upon order confirmation.", FONT_SUBTITLE));
        table.addCell(left);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{60, 40});

        addTotalRow(totalsTable, "Subtotal (Excl. Tax):", formatMoney(estimation.getSubtotal()));
        addTotalRow(totalsTable, "Estimated GST Tax:", formatMoney(estimation.getTaxAmount()));

        if (estimation.getDiscountAmount() != null && estimation.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Special Discount:", "-" + formatMoney(estimation.getDiscountAmount()));
        }

        PdfPCell finalLbl = new PdfPCell(new Phrase("FINAL ESTIMATED TOTAL:", FONT_TOTAL_TITLE));
        finalLbl.setBackgroundColor(BG_HEADER);
        finalLbl.setBorderColor(BORDER_COLOR);
        finalLbl.setPadding(8);
        totalsTable.addCell(finalLbl);

        PdfPCell finalVal = new PdfPCell(new Phrase(formatMoney(estimation.getGrandTotal()), FONT_TOTAL_VAL));
        finalVal.setBackgroundColor(BG_HEADER);
        finalVal.setBorderColor(BORDER_COLOR);
        finalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        finalVal.setPadding(8);
        totalsTable.addCell(finalVal);

        PdfPCell rightCell = new PdfPCell(totalsTable);
        rightCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(rightCell);

        doc.add(table);
    }

    private void addTermsAndConditions(Document doc, Estimation estimation) throws Exception {
        Paragraph p = new Paragraph();
        p.add(new Paragraph("TERMS & CONDITIONS", FONT_TH));
        p.add(new Paragraph("1. This quotation is an estimate based on the extracted requirements and active database prices.", FONT_SUBTITLE));
        p.add(new Paragraph("2. Prices and inventory availability are subject to re-verification at the time of order placement.", FONT_SUBTITLE));
        p.add(new Paragraph("3. Delivery site must ensure unhindered heavy vehicle and transit crane access.", FONT_SUBTITLE));
        p.add(new Paragraph("4. Material Test Certificates (MTC) and quality reports will accompany dispatch for structural steel and cement items.", FONT_SUBTITLE));
        p.setSpacingBefore(10);
        doc.add(p);
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_BODY));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, FONT_BODY_BOLD));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private PdfPCell createCell(String text, Font font, Color bgColor, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        return cell;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + String.format("%,.2f", amount);
    }
}
