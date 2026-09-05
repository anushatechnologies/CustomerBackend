package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SupportTicketRequest;
import com.example.project.customer.dto.SupportTicketResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;

import java.util.List;

public interface SupportTicketService {
    ApiResponse<List<SupportTicketResponse>> getTickets(Integer userId, String status, int page, int limit);
    SupportTicketResponse getTicketById(Integer ticketId);
    SupportTicketResponse createTicket(Integer userId, SupportTicketRequest request);
    TicketMessageResponse addMessage(Integer ticketId, Integer senderId, String senderRole, String senderName, TicketMessageRequest request);
}
