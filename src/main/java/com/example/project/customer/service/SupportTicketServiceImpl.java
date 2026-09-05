package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.SupportTicketRequest;
import com.example.project.customer.dto.SupportTicketResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.entity.SupportTicket;
import com.example.project.customer.entity.TicketMessage;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.SupportTicketRepository;
import com.example.project.customer.repository.TicketMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SupportTicketResponse>> getTickets(Integer userId, String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        int uid = userId != null ? userId : 101;

        Page<SupportTicket> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(uid, status.trim().toUpperCase(), pageable);
        } else {
            pageResult = ticketRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
        }

        List<SupportTicketResponse> data = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Support tickets retrieved successfully", data, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketById(Integer ticketId) {
        SupportTicket ticket = findTicket(ticketId);
        return mapToResponse(ticket);
    }

    @Override
    public SupportTicketResponse createTicket(Integer userId, SupportTicketRequest request) {
        int uid = userId != null ? userId : 101;
        String tktNum = "TKT-" + System.currentTimeMillis();

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(tktNum)
                .userId(uid)
                .subject(request.getSubject())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status("OPEN")
                .orderId(request.getOrderId())
                .messages(new ArrayList<>())
                .build();

        SupportTicket saved = ticketRepository.save(ticket);

        TicketMessage initialMsg = TicketMessage.builder()
                .ticket(saved)
                .senderId(uid)
                .senderRole("USER")
                .senderName("Customer")
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(initialMsg);
        saved.getMessages().add(initialMsg);

        return mapToResponse(saved);
    }

    @Override
    public TicketMessageResponse addMessage(Integer ticketId, Integer senderId, String senderRole, String senderName, TicketMessageRequest request) {
        SupportTicket ticket = findTicket(ticketId);
        int sid = senderId != null ? senderId : 101;
        String role = senderRole != null ? senderRole : "USER";

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .senderId(sid)
                .senderRole(role)
                .senderName(senderName != null ? senderName : ("USER".equals(role) ? "Customer" : "HinchMart Support"))
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .timestamp(LocalDateTime.now())
                .build();

        TicketMessage saved = messageRepository.save(msg);

        // Update status if closed or resolved
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus()) && "USER".equalsIgnoreCase(role)) {
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);
        }

        return mapToMessageResponse(saved);
    }

    private SupportTicket findTicket(Integer id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with id: " + id));
    }

    private SupportTicketResponse mapToResponse(SupportTicket t) {
        List<TicketMessageResponse> msgs = t.getMessages() != null
                ? t.getMessages().stream().map(this::mapToMessageResponse).toList()
                : List.of();

        return SupportTicketResponse.builder()
                .ticketId(t.getTicketId())
                .ticketNumber(t.getTicketNumber())
                .userId(t.getUserId())
                .subject(t.getSubject())
                .category(t.getCategory())
                .priority(t.getPriority())
                .status(t.getStatus())
                .orderId(t.getOrderId())
                .messages(msgs)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private TicketMessageResponse mapToMessageResponse(TicketMessage m) {
        return TicketMessageResponse.builder()
                .messageId(m.getMessageId())
                .ticketId(m.getTicket().getTicketId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .senderName(m.getSenderName())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .timestamp(m.getTimestamp())
                .build();
    }
}
