package com.example.project.customer.service;

import com.example.project.customer.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB
    private static final List<String> SUPPORTED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException("File size exceeds the maximum limit of 15MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            String filename = file.getOriginalFilename();
            if (filename == null || !hasSupportedExtension(filename)) {
                throw new DocumentProcessingException("Unsupported file format: " + contentType + ". Supported formats: PDF, JPG, JPEG, PNG");
            }
        }
    }

    @Override
    public boolean isPdf(MultipartFile file) {
        if (file == null) return false;
        String contentType = file.getContentType();
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return true;
        }
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractTextFromPdf(MultipartFile file) {
        validateFile(file);
        if (!isPdf(file)) {
            throw new DocumentProcessingException("File is not a valid PDF document");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new DocumentProcessingException("Encrypted or password-protected PDF files are not supported");
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            String text = textStripper.getText(document);

            if (text == null || text.isBlank()) {
                log.info("PDF contains no extractable selectable text (likely a scanned document): {}", file.getOriginalFilename());
                return "";
            }

            log.info("Extracted {} characters from PDF: {}", text.length(), file.getOriginalFilename());
            return text.trim();
        } catch (IOException e) {
            log.error("Failed to parse PDF file: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Corrupted or unreadable PDF document: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] renderFirstPageToImage(MultipartFile file) {
        validateFile(file);
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render PDF page to image: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Failed to render PDF page for OCR/vision: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getFileBytes(MultipartFile file) {
        validateFile(file);
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read file bytes: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Failed to read file bytes: " + e.getMessage(), e);
        }
    }

    private boolean hasSupportedExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }
}
