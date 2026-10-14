package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.seller.SellerSupportTicketRequest;
import com.example.project.customer.dto.seller.SellerSupportTicketResponse;
import com.example.project.customer.service.SellerSupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/help")
@RequiredArgsConstructor
public class SellerSupportTicketController {

    private final SellerSupportTicketService ticketService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SellerSupportTicketResponse>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        ApiResponse<List<SellerSupportTicketResponse>> response = ticketService.getTickets(sellerId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SellerSupportTicketResponse>> createTicket(
            @Valid @RequestBody SellerSupportTicketRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        SellerSupportTicketResponse ticket = ticketService.createTicket(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Seller support ticket created successfully", ticket));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SellerSupportTicketResponse>> getTicketById(@PathVariable Integer id) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        SellerSupportTicketResponse ticket = ticketService.getTicketById(id, sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller support ticket details retrieved successfully", ticket));
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Integer id,
            @Valid @RequestBody TicketMessageRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        TicketMessageResponse message = ticketService.addMessage(id, sellerId, "SELLER", null, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message added to seller support ticket", message));
    }
}
