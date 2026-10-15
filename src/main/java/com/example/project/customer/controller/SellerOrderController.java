package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SellerOrderDto;
import com.example.project.customer.dto.SellerOrderPageData;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
public class SellerOrderController {

    private final OrderService orderService;
    private final SellerContextUtil sellerContextUtil;

    /**
     * Retrieve orders containing items belonging to the authenticated seller.
     * Administrators may pass an optional ?sellerId parameter to inspect orders for a specific seller.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SellerOrderPageData>> getSellerOrders(
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer size
    ) {
        int effectiveLimit = limit != null ? limit : (size != null ? size : 20);
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId);
        SellerOrderPageData pageData = orderService.getSellerOrders(effectiveSellerId, status, page, effectiveLimit);
        return ResponseEntity.ok(ApiResponse.ok("Seller orders retrieved successfully", pageData));
    }

    /**
     * Allow sellers to view details of a single order containing their items.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerOrderDto>> getSellerOrderById(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer sellerId
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId);
        SellerOrderDto order = orderService.getSellerOrderById(id, effectiveSellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller order retrieved successfully", order));
    }

    /**
     * Allow sellers to update order status (e.g. PROCESSING, CONFIRMED, DISPATCHED, etc.)
     */
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<SellerOrderDto>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer sellerId,
            @RequestBody Map<String, String> body
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId);
        String status = body != null ? body.get("status") : null;
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Field 'status' is required.");
        }
        String notes = body.get("notes") != null ? body.get("notes") : body.get("description");
        String location = body.get("location");
        String carrierName = body.get("carrierName");
        String vehicleNumber = body.get("vehicleNumber");
        String driverName = body.get("driverName");
        String trackingNumber = body.get("trackingNumber");
        String estimatedDelivery = body.get("estimatedDelivery");

        orderService.updateOrderStatus(
                id, status, location, notes,
                carrierName, vehicleNumber, driverName, trackingNumber, estimatedDelivery
        );

        SellerOrderDto updated = orderService.getSellerOrderById(id, effectiveSellerId);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", updated));
    }

    private Integer resolveEffectiveSellerId(Integer requestedSellerId) {
        if (SecurityUtils.isAdmin() && requestedSellerId != null) {
            return requestedSellerId;
        }
        if (SecurityUtils.isAdmin()) {
            return requestedSellerId;
        }
        return SecurityUtils.getCurrentSellerId()
                .or(() -> {
                    try {
                        return java.util.Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                    } catch (Exception e) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse(null);
    }
}
