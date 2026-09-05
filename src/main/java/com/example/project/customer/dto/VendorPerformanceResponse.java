package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorPerformanceResponse {
    private Integer sellerId;
    private Double fulfillmentRate; // percentage (e.g. 98.5)
    private Double onTimeDeliveryRate; // percentage (e.g. 96.2)
    private Double customerRating; // 1-5 scale (e.g. 4.85)
    private Long totalReviews;
    private Double cancellationRate; // percentage (e.g. 1.2)
    private Double returnRate; // percentage (e.g. 0.5)
    private Double responseTimeHours; // hours (e.g. 1.8)
    private String sellerTier; // TIER_1_PLATINUM, TIER_2_GOLD, etc.
    private List<MonthlyMetric> monthlyPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyMetric {
        private String month;
        private Long orderCount;
        private BigDecimal revenue;
        private Double rating;
    }
}
