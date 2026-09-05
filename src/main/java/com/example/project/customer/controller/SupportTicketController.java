package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SupportTicketRequest;
import com.example.project.customer.dto.SupportTicketResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.service.SupportTicketService;
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
@RequestMapping("/api/help")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final UserContextUtil userContextUtil;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicketResponse>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        Integer userId = userContextUtil.getCurrentUserId();
        ApiResponse<List<SupportTicketResponse>> response = ticketService.getTickets(userId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @Valid @RequestBody SupportTicketRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        SupportTicketResponse ticket = ticketService.createTicket(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support ticket created successfully", ticket));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketById(@PathVariable Integer id) {
        SupportTicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.ok("Support ticket details retrieved successfully", ticket));
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Integer id,
            @Valid @RequestBody TicketMessageRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        TicketMessageResponse message = ticketService.addMessage(id, userId, "USER", "Customer", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message added to support ticket", message));
    }
}
