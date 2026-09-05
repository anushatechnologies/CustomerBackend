package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PurchaseOrderRequest;
import com.example.project.customer.dto.PurchaseOrderResponse;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(Integer userId, PurchaseOrderRequest request);
    ApiResponse<List<PurchaseOrderResponse>> getPurchaseOrders(Integer userId, String status, int page, int limit);
    PurchaseOrderResponse getPurchaseOrderById(Integer poId);
    PurchaseOrderResponse approvePurchaseOrder(Integer poId, String approvedBy);
    PurchaseOrderResponse rejectPurchaseOrder(Integer poId, String reason);
}
