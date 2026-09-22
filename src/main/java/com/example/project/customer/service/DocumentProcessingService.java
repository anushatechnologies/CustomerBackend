package com.example.project.customer.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentProcessingService {

    void validateFile(MultipartFile file);

    boolean isPdf(MultipartFile file);

    String extractTextFromPdf(MultipartFile file);

    byte[] renderFirstPageToImage(MultipartFile file);

    byte[] getFileBytes(MultipartFile file);
}
