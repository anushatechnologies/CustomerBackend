package com.example.project.customer.controller;

import com.example.project.customer.dto.*;
import com.example.project.customer.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/orders", "/api/admin/order"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Retrieve all orders placed by all buyers across the platform.
     * Supports optional status filtering, page, and limit.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        ApiResponse<List<OrderSummaryResponse>> response = orderService.getAllOrdersForAdmin(status, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve single order details for any platform order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Integer id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", order));
    }

    /**
     * Retrieve tracking checkpoints and genuine dispatch details for an order.
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOrderTracking(@PathVariable Integer id) {
        OrderTrackingResponse tracking = orderService.getOrderTracking(id);
        return ResponseEntity.ok(ApiResponse.ok("Order tracking checkpoints retrieved successfully", tracking));
    }

    /**
     * Retrieve tax invoice breakdown for an order.
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getOrderInvoice(@PathVariable Integer id) {
        InvoiceResponse invoice = orderService.getOrderInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok("Tax invoice retrieved successfully", invoice));
    }

    /**
     * Download generated PDF invoice for an order.
     */
    @GetMapping("/{id}/invoice/download")
    public ResponseEntity<byte[]> downloadOrderInvoice(@PathVariable Integer id) {
        byte[] pdfBytes = orderService.generateInvoicePdf(id);
        String filename = "INV-" + String.format("%06d", id) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    /**
     * Preview PDF invoice in browser for an order.
     */
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> previewOrderInvoice(@PathVariable Integer id) {
        byte[] pdfBytes = orderService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"INV-" + id + ".pdf\"")
                .body(pdfBytes);
    }

    /**
     * Update order status and assign genuine logistics dispatch details.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String location = body.get("location");
        String description = body.get("description");
        String carrierName = body.get("carrierName");
        String vehicleNumber = body.get("vehicleNumber");
        String driverName = body.get("driverName");
        String trackingNumber = body.get("trackingNumber");
        String estimatedDelivery = body.get("estimatedDelivery");

        OrderResponse updated = orderService.updateOrderStatus(
                id, status, location, description,
                carrierName, vehicleNumber, driverName, trackingNumber, estimatedDelivery);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", updated));
    }

    /**
     * Patch order status and assign genuine logistics dispatch details.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> patchOrderStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body
    ) {
        return updateOrderStatus(id, body);
    }

    /**
     * Cancel an order from admin desk.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String location = body != null ? body.get("location") : "Admin Panel";
        String description = body != null ? body.get("description") : "Cancelled by platform administrator";
        OrderResponse cancelled = orderService.cancelOrder(id, location, description);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", cancelled));
    }
}
