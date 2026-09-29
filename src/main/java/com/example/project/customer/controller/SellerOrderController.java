package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.SellerOrderAcceptRequest;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import com.example.project.customer.service.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getSellerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) Integer sellerId,
            @RequestHeader(value = "X-Seller-Id", required = false) Integer headerSellerId
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId, headerSellerId);
        Page<OrderResponse> orders = sellerOrderService.getSellerOrders(effectiveSellerId, status, page, limit);

        ApiResponse<List<OrderResponse>> response = ApiResponse.ok("Seller orders retrieved successfully", orders.getContent());
        response.setPagination(com.example.project.customer.dto.PaginationMeta.of(page, limit, orders.getTotalElements()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getSellerOrderById(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer sellerId,
            @RequestHeader(value = "X-Seller-Id", required = false) Integer headerSellerId
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId, headerSellerId);
        OrderResponse order = sellerOrderService.getSellerOrderById(effectiveSellerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Seller order retrieved successfully", order));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) SellerOrderAcceptRequest request,
            @RequestParam(required = false) Integer sellerId,
            @RequestHeader(value = "X-Seller-Id", required = false) Integer headerSellerId
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId, headerSellerId);
        OrderResponse order = sellerOrderService.acceptOrder(effectiveSellerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Order accepted successfully. Customer Delivery OTP generated and rider dispatch initiated.", order));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable Integer id,
            @Valid @RequestBody SellerOrderRejectRequest request,
            @RequestParam(required = false) Integer sellerId,
            @RequestHeader(value = "X-Seller-Id", required = false) Integer headerSellerId
    ) {
        Integer effectiveSellerId = resolveEffectiveSellerId(sellerId, headerSellerId);
        OrderResponse order = sellerOrderService.rejectOrder(effectiveSellerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Order rejected successfully", order));
    }

    private Integer resolveEffectiveSellerId(Integer requestedSellerId, Integer headerSellerId) {
        if (com.example.project.customer.security.SecurityUtils.isAdmin() && requestedSellerId != null) {
            return requestedSellerId;
        }
        if (requestedSellerId != null) {
            return requestedSellerId;
        }
        if (headerSellerId != null) {
            return headerSellerId;
        }
        try {
            return sellerContextUtil.getCurrentSellerId();
        } catch (Exception e) {
            return 1; // Fallback default store/seller
        }
    }
}
