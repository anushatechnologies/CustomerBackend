package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;
import com.example.project.customer.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Valid @RequestBody CreateOrderRequest request) {
        Integer uid = userId != null ? userId : 101;
        OrderResponse response = orderService.placeOrder(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Order placed successfully", response));
    }

    @GetMapping
    public PagedResponse<OrderSummaryDto> getOrders(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Integer uid = userId != null ? userId : 101;
        return orderService.getOrders(uid, status, page, limit);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> getOrderById(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(orderService.getOrderById(uid, id));
    }

    @GetMapping("/{id}/tracking")
    public ApiResponse<OrderTrackingResponse> getOrderTracking(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(orderService.getOrderTracking(uid, id));
    }

    @GetMapping("/{id}/invoice")
    public ApiResponse<InvoiceResponse> getOrderInvoice(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(orderService.getOrderInvoice(uid, id));
    }
}
