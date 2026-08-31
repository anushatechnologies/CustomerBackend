package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryResponse {
    private Integer productId;
    private Double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingBreakdown; // 5 -> count, 4 -> count, etc.
}
