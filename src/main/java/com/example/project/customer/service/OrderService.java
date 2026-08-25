package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;

public interface OrderService {
    OrderResponse placeOrder(Integer userId, CreateOrderRequest request);
    PagedResponse<OrderSummaryDto> getOrders(Integer userId, String status, int page, int limit);
    OrderDetailResponse getOrderById(Integer userId, Integer orderId);
    OrderTrackingResponse getOrderTracking(Integer userId, Integer orderId);
    InvoiceResponse getOrderInvoice(Integer userId, Integer orderId);
}
