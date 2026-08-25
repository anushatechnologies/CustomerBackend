package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {

    private String imageKey;
    private String imageUrl;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
}
