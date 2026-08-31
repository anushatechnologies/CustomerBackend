package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.InvoiceResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.OrderTrackingResponse;
import com.example.project.customer.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse order = orderService.createOrder(101, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Order placed successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        ApiResponse<List<OrderSummaryResponse>> response = orderService.getOrders(101, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Integer id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", order));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOrderTracking(@PathVariable Integer id) {
        OrderTrackingResponse tracking = orderService.getOrderTracking(id);
        return ResponseEntity.ok(ApiResponse.ok("Order tracking checkpoints retrieved successfully", tracking));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getOrderInvoice(@PathVariable Integer id) {
        InvoiceResponse invoice = orderService.getOrderInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok("Tax invoice retrieved successfully", invoice));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "IN_TRANSIT");
        String location = body.get("location");
        String description = body.get("description");
        OrderResponse updated = orderService.updateOrderStatus(id, status, location, description);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", updated));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, String> request = body != null ? body : Map.of();
        OrderResponse cancelled = orderService.cancelOrder(
                id,
                request.get("location"),
                request.get("description")
        );

        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", cancelled));
    }
}
