package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SupportTicketResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.TicketStatusUpdateRequest;
import com.example.project.customer.service.SupportTicketService;
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
@RequestMapping({"/api/admin/help/tickets", "/api/admin/customer/help/tickets", "/api/admin/customers/tickets", "/api/admin/tickets"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportTicketController {

    private final SupportTicketService ticketService;
    private final UserContextUtil userContextUtil;

    /**
     * Admin: List all customer support tickets with full contact details and optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketResponse>>> getTickets(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        ApiResponse<List<SupportTicketResponse>> response = ticketService.getTicketsForAdmin(userId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: View details of a specific customer support ticket.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketById(@PathVariable Integer id) {
        SupportTicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.ok("Customer support ticket details retrieved successfully", ticket));
    }

    /**
     * Admin: Reply to a customer support ticket as support agent.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Integer id,
            @Valid @RequestBody TicketMessageRequest request) {
        Integer adminUserId = userContextUtil.getOptionalCurrentUserId();
        int senderId = (adminUserId != null) ? adminUserId : 1;

        TicketMessageResponse message = ticketService.addMessage(id, senderId, "SUPPORT_AGENT", "HinchMart Support", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reply added to customer support ticket", message));
    }

    /**
     * Admin: Update status and/or priority of a customer support ticket (PATCH & PUT supported).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateTicketStatus(
            @PathVariable Integer id,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        SupportTicketResponse ticket = ticketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Customer support ticket status updated successfully", ticket));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> putTicketStatus(
            @PathVariable Integer id,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        return updateTicketStatus(id, request);
    }
}
