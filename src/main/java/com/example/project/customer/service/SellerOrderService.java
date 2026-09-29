package com.example.project.customer.service;

import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import org.springframework.data.domain.Page;

public interface SellerOrderService {

    Page<OrderResponse> getSellerOrders(Integer sellerId, String status, int page, int limit);

    OrderResponse getSellerOrderById(Integer sellerId, Integer orderId);

    OrderResponse acceptOrder(Integer sellerId, Integer orderId, com.example.project.customer.dto.SellerOrderAcceptRequest request);

    default OrderResponse acceptOrder(Integer sellerId, Integer orderId) {
        return acceptOrder(sellerId, orderId, null);
    }

    OrderResponse rejectOrder(Integer sellerId, Integer orderId, SellerOrderRejectRequest request);
}
