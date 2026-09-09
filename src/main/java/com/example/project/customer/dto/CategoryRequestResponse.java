package com.example.project.customer.dto;

import com.example.project.customer.entity.CategoryRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestResponse {

    private Integer id;
    private Integer sellerId;
    private String sellerName;
    private String name;
    private Integer parentCategoryId;
    private String description;
    private CategoryRequestStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
