package com.example.project.customer.dto;

import com.example.project.customer.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CustomerDocumentRequest(
        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Title must not be blank")
        String title,

        String documentNumber,

        @NotBlank(message = "File name must not be blank")
        String fileName,

        @NotBlank(message = "File URL must not be blank")
        String fileUrl,

        String fileSize,

        LocalDate expiresOn
) {
}
