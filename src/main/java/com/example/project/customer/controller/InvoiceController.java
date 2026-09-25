package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.InvoiceSummaryResponse;
import com.example.project.customer.entity.Order;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InvoiceController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserContextUtil userContextUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>> getInvoices(
            @RequestParam(value = "financialYear", required = false) String financialYear,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        Integer userId = userContextUtil.getCurrentUserId();
        log.info("Fetching tax invoices for customer #{}, fy={}, start={}, end={}", userId, financialYear, startDate, endDate);

        List<Order> orders = orderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(userId);

        List<InvoiceSummaryResponse> invoices = orders.stream()
                .map(order -> {
                    String invNum = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                            ? order.getStoreInvoiceNumber()
                            : "INV-" + String.format("%06d", order.getOrderId());

                    String dateStr = order.getCreatedAt() != null
                            ? order.getCreatedAt().format(DATE_FORMATTER)
                            : "";

                    String status = order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID";
                    if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
                        status = "CANCELLED";
                    }

                    return InvoiceSummaryResponse.builder()
                            .invoiceNumber(invNum)
                            .orderId(order.getOrderId())
                            .orderNumber(order.getOrderNumber())
                            .date(dateStr)
                            .amount(order.getTotalAmount())
                            .downloadUrl("/api/invoices/" + order.getOrderId() + "/download-pdf")
                            .status(status)
                            .createdAt(order.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Tax invoices retrieved successfully", invoices));
    }

    @GetMapping("/{id}/download-pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Integer id) {
        log.info("Downloading PDF tax invoice for order #{}", id);
        byte[] pdfBytes = orderService.generateInvoicePdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
