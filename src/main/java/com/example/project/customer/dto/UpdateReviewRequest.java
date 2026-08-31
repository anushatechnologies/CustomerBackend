package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest {
    private String title;
    private String comment;
    private Integer rating; // optional, if provided must be between 1 and 5
    private String status; // APPROVED, REJECTED, PENDING
}
