package com.example.project.customer.dto;

import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerDocumentResponse(
        Integer documentId,
        Integer customerId,
        DocumentType documentType,
        String title,
        String documentNumber,
        String fileName,
        String fileUrl,
        String fileSize,
        VerificationStatus status,
        String rejectionReason,
        LocalDate expiresOn,
        LocalDateTime uploadedAt,
        LocalDateTime verifiedAt
) {
}
