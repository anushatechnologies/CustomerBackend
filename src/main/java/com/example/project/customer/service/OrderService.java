package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.InvoiceResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.OrderTrackingResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Integer userId, OrderCreateRequest request);
    ApiResponse<List<OrderSummaryResponse>> getOrders(Integer userId, String status, int page, int limit);
    OrderResponse getOrderById(Integer id);
    OrderTrackingResponse getOrderTracking(Integer id);
    InvoiceResponse getOrderInvoice(Integer id);
    OrderResponse updateOrderStatus(Integer id, String status, String location, String description);
}
