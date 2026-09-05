package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PurchaseOrderRequest;
import com.example.project.customer.dto.PurchaseOrderResponse;
import com.example.project.customer.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final UserContextUtil userContextUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        PurchaseOrderResponse created = purchaseOrderService.createPurchaseOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Purchase order created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getPurchaseOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        Integer userId = userContextUtil.getCurrentUserId();
        ApiResponse<List<PurchaseOrderResponse>> response = purchaseOrderService.getPurchaseOrders(userId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable Integer id) {
        PurchaseOrderResponse po = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase order retrieved successfully", po));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approvePurchaseOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        String approvedBy = body != null ? body.get("approvedBy") : null;
        PurchaseOrderResponse approved = purchaseOrderService.approvePurchaseOrder(id, approvedBy);
        return ResponseEntity.ok(ApiResponse.ok("Purchase order approved successfully", approved));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> rejectPurchaseOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        PurchaseOrderResponse rejected = purchaseOrderService.rejectPurchaseOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Purchase order rejected", rejected));
    }
}
