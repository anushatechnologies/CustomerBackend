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
public class VendorDashboardResponse {
    private Integer sellerId;
    private Long totalProducts;
    private Long activeProducts;
    private Long lowStockAlerts;
    private Long outOfStockCount;
    private Long totalOrders;
    private Long pendingOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Integer totalWarehouses;
    private List<VendorRecentActivity> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorRecentActivity {
        private String id;
        private String type; // ORDER, INVENTORY, PRICING, SYSTEM
        private String message;
        private String timestamp;
    }
}
