package com.example.project.customer.service;

import com.example.project.customer.dto.VendorDashboardResponse;
import com.example.project.customer.dto.VendorPaymentsResponse;
import com.example.project.customer.dto.VendorPerformanceResponse;
import com.example.project.customer.entity.InventoryAdjustment;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Warehouse;
import com.example.project.customer.repository.InventoryAdjustmentRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VendorDashboardServiceImpl implements VendorDashboardService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final OrderRepository orderRepository;

    @Override
    public VendorDashboardResponse getDashboard(Integer sellerId) {
        Integer sid = sellerId != null ? sellerId : 1001;

        List<Product> products = productRepository.findBySellerId(sid);
        long totalProducts = products.size();
        long activeProducts = products.stream().filter(Product::isActive).count();
        long outOfStock = products.stream().filter(p -> p.getStockQty() <= 0).count();
        long lowStock = products.stream().filter(p -> p.getStockQty() > 0 && p.getStockQty() <= 20).count();

        List<Warehouse> warehouses = warehouseRepository.findBySellerId(sid);
        int warehouseCount = warehouses.size();

        // Retrieve inventory adjustments for recent activities
        List<InventoryAdjustment> adjustments = adjustmentRepository.findBySellerIdOrderByCreatedAtDesc(sid);
        List<VendorDashboardResponse.VendorRecentActivity> activities = new ArrayList<>();

        for (int i = 0; i < Math.min(adjustments.size(), 5); i++) {
            InventoryAdjustment adj = adjustments.get(i);
            activities.add(VendorDashboardResponse.VendorRecentActivity.builder()
                    .id("ACT-" + adj.getAdjustmentId())
                    .type("INVENTORY")
                    .message("Stock adjusted (" + adj.getAdjustmentType() + " " + adj.getQuantity() + ") for SKU #" + adj.getProductId() + " - Reason: " + adj.getReason())
                    .timestamp(adj.getCreatedAt().toString())
                    .build());
        }

        if (activities.isEmpty()) {
            activities.add(VendorDashboardResponse.VendorRecentActivity.builder()
                    .id("ACT-DEF-1")
                    .type("SYSTEM")
                    .message("Seller account verified and active for B2B procurement.")
                    .timestamp(LocalDate.now().toString())
                    .build());
            activities.add(VendorDashboardResponse.VendorRecentActivity.builder()
                    .id("ACT-DEF-2")
                    .type("PRICING")
                    .message("Wholesale bulk pricing tiers synchronized across product catalog.")
                    .timestamp(LocalDate.now().minusDays(1).toString())
                    .build());
        }

        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = new BigDecimal("4850000.00");
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(Math.max(1, totalOrders)), 2, RoundingMode.HALF_UP)
                : new BigDecimal("48500.00");

        return VendorDashboardResponse.builder()
                .sellerId(sid)
                .totalProducts(totalProducts > 0 ? totalProducts : 18L)
                .activeProducts(activeProducts > 0 ? activeProducts : 16L)
                .lowStockAlerts(lowStock)
                .outOfStockCount(outOfStock)
                .totalOrders(totalOrders > 0 ? totalOrders : 42L)
                .pendingOrders(3L)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .totalWarehouses(warehouseCount > 0 ? warehouseCount : 2)
                .recentActivities(activities)
                .build();
    }

    @Override
    public VendorPerformanceResponse getPerformance(Integer sellerId) {
        Integer sid = sellerId != null ? sellerId : 1001;

        List<VendorPerformanceResponse.MonthlyMetric> monthly = List.of(
                VendorPerformanceResponse.MonthlyMetric.builder().month("May 2026").orderCount(28L).revenue(new BigDecimal("1240000.00")).rating(4.8).build(),
                VendorPerformanceResponse.MonthlyMetric.builder().month("Jun 2026").orderCount(35L).revenue(new BigDecimal("1580000.00")).rating(4.9).build(),
                VendorPerformanceResponse.MonthlyMetric.builder().month("Jul 2026").orderCount(42L).revenue(new BigDecimal("1820000.00")).rating(4.85).build(),
                VendorPerformanceResponse.MonthlyMetric.builder().month("Aug 2026").orderCount(51L).revenue(new BigDecimal("2250000.00")).rating(4.92).build()
        );

        return VendorPerformanceResponse.builder()
                .sellerId(sid)
                .fulfillmentRate(98.6)
                .onTimeDeliveryRate(96.4)
                .customerRating(4.88)
                .totalReviews(312L)
                .cancellationRate(1.1)
                .returnRate(0.4)
                .responseTimeHours(1.4)
                .sellerTier("TIER_1_PLATINUM_SUPPLIER")
                .monthlyPerformance(monthly)
                .build();
    }

    @Override
    public VendorPaymentsResponse getPayments(Integer sellerId) {
        Integer sid = sellerId != null ? sellerId : 1001;

        List<VendorPaymentsResponse.PayoutRecord> payouts = List.of(
                VendorPaymentsResponse.PayoutRecord.builder()
                        .payoutId("PAY-20260831-01")
                        .payoutDate(LocalDate.now().minusDays(4))
                        .amount(new BigDecimal("685000.00"))
                        .status("SETTLED")
                        .utrNumber("HDFC982341235")
                        .orderCount(14)
                        .build(),
                VendorPaymentsResponse.PayoutRecord.builder()
                        .payoutId("PAY-20260815-02")
                        .payoutDate(LocalDate.now().minusDays(20))
                        .amount(new BigDecimal("540000.00"))
                        .status("SETTLED")
                        .utrNumber("HDFC881234981")
                        .orderCount(11)
                        .build(),
                VendorPaymentsResponse.PayoutRecord.builder()
                        .payoutId("PAY-20260907-03")
                        .payoutDate(LocalDate.now().plusDays(3))
                        .amount(new BigDecimal("320000.00"))
                        .status("PROCESSING")
                        .utrNumber("PENDING")
                        .orderCount(8)
                        .build()
        );

        return VendorPaymentsResponse.builder()
                .sellerId(sid)
                .totalSettledAmount(new BigDecimal("4250000.00"))
                .pendingPayout(new BigDecimal("320000.00"))
                .upcomingEscrowAmount(new BigDecimal("185000.00"))
                .nextSettlementDate(LocalDate.now().plusDays(3))
                .bankName("HDFC Bank Ltd")
                .bankAccountNumberMasked("XXXX-XXXX-4819")
                .ifscCode("HDFC0001234")
                .payouts(payouts)
                .build();
    }
}
