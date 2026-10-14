package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.seller.SellerSupportTicketResponse;
import com.example.project.customer.dto.seller.SellerTicketStatusUpdateRequest;
import com.example.project.customer.service.SellerSupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping({"/api/admin/seller/help/tickets", "/api/admin/sellers/tickets"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerSupportTicketController {

    private final SellerSupportTicketService ticketService;
    private final UserContextUtil userContextUtil;

    /**
     * Admin: List all seller support tickets with optional filters by sellerId and status.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerSupportTicketResponse>>> getTickets(
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        ApiResponse<List<SellerSupportTicketResponse>> response = ticketService.getTicketsForAdmin(sellerId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: View details of a specific seller support ticket including all message history.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerSupportTicketResponse>> getTicketById(@PathVariable Integer id) {
        SellerSupportTicketResponse ticket = ticketService.getTicketByIdForAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("Seller support ticket details retrieved successfully", ticket));
    }

    /**
     * Admin: Reply to a seller support ticket as support agent.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Integer id,
            @Valid @RequestBody TicketMessageRequest request) {
        Integer adminUserId = userContextUtil.getOptionalCurrentUserId();
        int senderId = (adminUserId != null) ? adminUserId : 1;

        TicketMessageResponse message = ticketService.addMessage(id, senderId, "SUPPORT_AGENT", "HinchMart Support", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reply added to seller support ticket", message));
    }

    /**
     * Admin: Update status and/or priority of a seller support ticket (e.g. IN_PROGRESS, RESOLVED, CLOSED).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SellerSupportTicketResponse>> updateTicketStatus(
            @PathVariable Integer id,
            @Valid @RequestBody SellerTicketStatusUpdateRequest request) {
        SellerSupportTicketResponse ticket = ticketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller support ticket status updated successfully", ticket));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SellerSupportTicketResponse>> putTicketStatus(
            @PathVariable Integer id,
            @Valid @RequestBody SellerTicketStatusUpdateRequest request) {
        return updateTicketStatus(id, request);
    }
}
