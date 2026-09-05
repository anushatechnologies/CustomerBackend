package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.PurchaseOrderItemRequest;
import com.example.project.customer.dto.PurchaseOrderItemResponse;
import com.example.project.customer.dto.PurchaseOrderRequest;
import com.example.project.customer.dto.PurchaseOrderResponse;
import com.example.project.customer.entity.PurchaseOrder;
import com.example.project.customer.entity.PurchaseOrderItem;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.PurchaseOrderItemRepository;
import com.example.project.customer.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;

    @Override
    public PurchaseOrderResponse createPurchaseOrder(Integer userId, PurchaseOrderRequest request) {
        int uid = userId != null ? userId : 101;
        String poNum = "PO-" + System.currentTimeMillis();

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNum)
                .userId(uid)
                .vendorId(request.getVendorId())
                .totalAmount(BigDecimal.ZERO)
                .status("PENDING_APPROVAL")
                .deliveryDate(request.getDeliveryDate())
                .billingAddress(request.getBillingAddress())
                .shippingAddress(request.getShippingAddress())
                .paymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : "NET_30")
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            BigDecimal taxRate = itemReq.getTaxRate() != null ? itemReq.getTaxRate() : new BigDecimal("18.00");
            BigDecimal baseTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal taxAmount = baseTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = baseTotal.add(taxAmount);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productId(itemReq.getProductId())
                    .productTitle(itemReq.getProductTitle())
                    .quantity(itemReq.getQuantity())
                    .unit(itemReq.getUnit())
                    .unitPrice(itemReq.getUnitPrice())
                    .taxRate(taxRate)
                    .lineTotal(lineTotal)
                    .build();

            po.getItems().add(item);
            calculatedTotal = calculatedTotal.add(lineTotal);
        }

        po.setTotalAmount(calculatedTotal);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PurchaseOrderResponse>> getPurchaseOrders(Integer userId, String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        int uid = userId != null ? userId : 101;

        Page<PurchaseOrder> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = purchaseOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(uid, status.trim().toUpperCase(), pageable);
        } else {
            pageResult = purchaseOrderRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
        }

        List<PurchaseOrderResponse> data = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Purchase orders retrieved successfully", data, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Integer poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));
        return mapToResponse(po);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrder(Integer poId, String approvedBy) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));

        po.setStatus("APPROVED");
        po.setApprovedAt(LocalDateTime.now());
        po.setApprovedBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy : "Procurement Manager");

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse rejectPurchaseOrder(Integer poId, String reason) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));

        po.setStatus("REJECTED");
        po.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Procurement requirements not met");

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return mapToResponse(saved);
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        List<PurchaseOrderItemResponse> items = po.getItems() != null
                ? po.getItems().stream().map(this::mapItemToResponse).toList()
                : List.of();

        return PurchaseOrderResponse.builder()
                .poId(po.getPoId())
                .poNumber(po.getPoNumber())
                .userId(po.getUserId())
                .vendorId(po.getVendorId())
                .totalAmount(po.getTotalAmount())
                .status(po.getStatus())
                .deliveryDate(po.getDeliveryDate())
                .billingAddress(po.getBillingAddress())
                .shippingAddress(po.getShippingAddress())
                .paymentTerms(po.getPaymentTerms())
                .notes(po.getNotes())
                .rejectionReason(po.getRejectionReason())
                .approvedAt(po.getApprovedAt())
                .approvedBy(po.getApprovedBy())
                .items(items)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private PurchaseOrderItemResponse mapItemToResponse(PurchaseOrderItem item) {
        return PurchaseOrderItemResponse.builder()
                .itemId(item.getItemId())
                .productId(item.getProductId())
                .productTitle(item.getProductTitle())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .taxRate(item.getTaxRate())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
