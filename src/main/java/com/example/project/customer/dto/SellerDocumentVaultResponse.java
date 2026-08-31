package com.example.project.customer.dto;

import java.util.List;

public record SellerDocumentVaultResponse(
        Integer sellerId,
        String overallStatus,
        int totalRequired,
        int submittedCount,
        int verifiedCount,
        String progressText,
        boolean isAllSubmitted,
        boolean isAllVerified,
        List<DocumentVaultItem> documents
) {
    public record DocumentVaultItem(
            Long documentId,
            String documentType,
            String title,
            String description,
            String status,
            String statusCode,
            boolean isUploaded,
            String fileName,
            String fileUrl,
            Long fileSize,
            String fileSizeFormatted,
            String fileType,
            String uploadedAt,
            String remarks
    ) {}
}
