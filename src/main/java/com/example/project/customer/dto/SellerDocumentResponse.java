package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDocumentResponse {
    private String id;
    private String documentType;
    private String name;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String status;
    private LocalDateTime uploadedAt;
}
