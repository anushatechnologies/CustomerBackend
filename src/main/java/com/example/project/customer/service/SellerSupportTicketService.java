package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.seller.SellerSupportTicketRequest;
import com.example.project.customer.dto.seller.SellerSupportTicketResponse;

import com.example.project.customer.dto.seller.SellerTicketStatusUpdateRequest;

import java.util.List;

public interface SellerSupportTicketService {
    ApiResponse<List<SellerSupportTicketResponse>> getTickets(Integer sellerId, String status, int page, int limit);
    SellerSupportTicketResponse getTicketById(Integer ticketId, Integer sellerId);
    SellerSupportTicketResponse createTicket(Integer sellerId, SellerSupportTicketRequest request);
    TicketMessageResponse addMessage(Integer ticketId, Integer senderId, String senderRole, String senderName, TicketMessageRequest request);

    ApiResponse<List<SellerSupportTicketResponse>> getTicketsForAdmin(Integer sellerId, String status, int page, int limit);
    SellerSupportTicketResponse getTicketByIdForAdmin(Integer ticketId);
    SellerSupportTicketResponse updateTicketStatus(Integer ticketId, SellerTicketStatusUpdateRequest request);
}
