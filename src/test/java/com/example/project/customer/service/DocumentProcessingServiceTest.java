package com.example.project.customer.service;

import com.example.project.customer.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentProcessingServiceTest {

    private DocumentProcessingServiceImpl documentProcessingService;

    @BeforeEach
    void setUp() {
        documentProcessingService = new DocumentProcessingServiceImpl();
    }

    @Test
    @DisplayName("Should extract text from valid PDF document")
    void testExtractTextFromPdf() throws IOException {
        byte[] pdfBytes = createTestPdf("OPC 53 Grade Cement - 100 bags\nTMT Steel 12mm - 500 kg");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "requirement.pdf",
                "application/pdf",
                pdfBytes
        );

        assertTrue(documentProcessingService.isPdf(file));
        String text = documentProcessingService.extractTextFromPdf(file);
        assertNotNull(text);
        assertTrue(text.contains("OPC 53 Grade Cement"));
        assertTrue(text.contains("TMT Steel 12mm"));
    }

    @Test
    @DisplayName("Should reject empty file with DocumentProcessingException")
    void testValidateEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(DocumentProcessingException.class, () -> documentProcessingService.validateFile(emptyFile));
    }

    @Test
    @DisplayName("Should reject file exceeding 15MB limit")
    void testValidateFileTooLarge() {
        byte[] largeBytes = new byte[16 * 1024 * 1024]; // 16MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);
        assertThrows(DocumentProcessingException.class, () -> documentProcessingService.validateFile(largeFile));
    }

    @Test
    @DisplayName("Should reject unsupported file formats like executable or text")
    void testValidateUnsupportedFileFormat() {
        MockMultipartFile badFile = new MockMultipartFile("file", "script.sh", "application/x-sh", "echo hello".getBytes());
        assertThrows(DocumentProcessingException.class, () -> documentProcessingService.validateFile(badFile));
    }

    @Test
    @DisplayName("Should accept valid JPG, JPEG, and PNG images")
    void testValidateValidImages() {
        MockMultipartFile jpg = new MockMultipartFile("file", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile png = new MockMultipartFile("file", "doc.png", "image/png", new byte[]{1, 2, 3});

        documentProcessingService.validateFile(jpg);
        documentProcessingService.validateFile(png);
        assertFalse(documentProcessingService.isPdf(jpg));
    }

    private byte[] createTestPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -15);
                }
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
