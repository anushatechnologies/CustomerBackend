package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.seller.SellerSupportTicketRequest;
import com.example.project.customer.dto.seller.SellerSupportTicketResponse;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerSupportTicket;
import com.example.project.customer.entity.SellerTicketMessage;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.SellerSupportTicketRepository;
import com.example.project.customer.repository.SellerTicketMessageRepository;
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
public class SellerSupportTicketServiceImpl implements SellerSupportTicketService {

    private final SellerSupportTicketRepository ticketRepository;
    private final SellerTicketMessageRepository messageRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SellerSupportTicketResponse>> getTickets(Integer sellerId, String status, int page, int limit) {
        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required: Seller ID must not be null.");
        }
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<SellerSupportTicket> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = ticketRepository.findBySeller_SellerIdAndStatusOrderByCreatedAtDesc(sellerId, status.trim().toUpperCase(), pageable);
        } else {
            pageResult = ticketRepository.findBySeller_SellerIdOrderByCreatedAtDesc(sellerId, pageable);
        }

        List<SellerSupportTicketResponse> data = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Seller support tickets retrieved successfully", data, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerSupportTicketResponse getTicketById(Integer ticketId, Integer sellerId) {
        SellerSupportTicket ticket = findTicket(ticketId);
        // Ownership check: sellers can only view their own tickets
        if (!ticket.getSeller().getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Access Denied: This ticket does not belong to the authenticated seller.");
        }
        return mapToResponse(ticket);
    }

    @Override
    public SellerSupportTicketResponse createTicket(Integer sellerId, SellerSupportTicketRequest request) {
        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required: Seller ID must not be null.");
        }

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        String sellerName = (seller.getName() != null && !seller.getName().isBlank())
                ? seller.getName()
                : (seller.getCompanyName() != null && !seller.getCompanyName().isBlank()
                        ? seller.getCompanyName()
                        : "Seller");

        String tktNum = "STKT-" + System.currentTimeMillis();

        SellerSupportTicket ticket = SellerSupportTicket.builder()
                .ticketNumber(tktNum)
                .seller(seller)
                .subject(request.getSubject())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status("OPEN")
                .orderId(request.getOrderId())
                .messages(new ArrayList<>())
                .build();

        SellerSupportTicket saved = ticketRepository.save(ticket);

        SellerTicketMessage initialMsg = SellerTicketMessage.builder()
                .ticket(saved)
                .senderId(sellerId)
                .senderRole("SELLER")
                .senderName(sellerName)
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(initialMsg);
        saved.getMessages().add(initialMsg);

        log.info("Seller #{} created support ticket {} (category: {}, priority: {})",
                sellerId, tktNum, ticket.getCategory(), ticket.getPriority());

        return mapToResponse(saved);
    }

    @Override
    public TicketMessageResponse addMessage(Integer ticketId, Integer senderId, String senderRole, String senderName, TicketMessageRequest request) {
        if (senderId == null) {
            throw new UnauthorizedException("Authentication required: Sender ID must not be null.");
        }
        SellerSupportTicket ticket = findTicket(ticketId);
        String role = senderRole != null ? senderRole : "SELLER";

        String resolvedName = senderName;
        if ("SELLER".equalsIgnoreCase(role) && (resolvedName == null || resolvedName.isBlank())) {
            Seller seller = sellerRepository.findById(senderId).orElse(null);
            if (seller != null && seller.getName() != null && !seller.getName().isBlank()) {
                resolvedName = seller.getName();
            } else if (seller != null && seller.getCompanyName() != null && !seller.getCompanyName().isBlank()) {
                resolvedName = seller.getCompanyName();
            } else {
                resolvedName = "Seller";
            }
        } else if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = "SELLER".equals(role) ? "Seller" : "HinchMart Support";
        }

        SellerTicketMessage msg = SellerTicketMessage.builder()
                .ticket(ticket)
                .senderId(senderId)
                .senderRole(role)
                .senderName(resolvedName)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .timestamp(LocalDateTime.now())
                .build();

        SellerTicketMessage saved = messageRepository.save(msg);

        // Re-open ticket if closed and seller replies
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus()) && "SELLER".equalsIgnoreCase(role)) {
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);
        }

        return mapToMessageResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SellerSupportTicketResponse>> getTicketsForAdmin(Integer sellerId, String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<SellerSupportTicket> pageResult;
        boolean hasSeller = sellerId != null;
        boolean hasStatus = status != null && !status.isBlank();

        if (hasSeller && hasStatus) {
            pageResult = ticketRepository.findBySeller_SellerIdAndStatusOrderByCreatedAtDesc(sellerId, status.trim().toUpperCase(), pageable);
        } else if (hasSeller) {
            pageResult = ticketRepository.findBySeller_SellerIdOrderByCreatedAtDesc(sellerId, pageable);
        } else if (hasStatus) {
            pageResult = ticketRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable);
        } else {
            pageResult = ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<SellerSupportTicketResponse> data = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Seller support tickets retrieved successfully", data, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerSupportTicketResponse getTicketByIdForAdmin(Integer ticketId) {
        SellerSupportTicket ticket = findTicket(ticketId);
        return mapToResponse(ticket);
    }

    @Override
    public SellerSupportTicketResponse updateTicketStatus(Integer ticketId, com.example.project.customer.dto.seller.SellerTicketStatusUpdateRequest request) {
        SellerSupportTicket ticket = findTicket(ticketId);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            ticket.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            ticket.setPriority(request.getPriority().trim().toUpperCase());
        }
        SellerSupportTicket saved = ticketRepository.save(ticket);
        log.info("Admin updated seller ticket #{} status to {} (priority: {})",
                ticketId, saved.getStatus(), saved.getPriority());
        return mapToResponse(saved);
    }

    private SellerSupportTicket findTicket(Integer id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller support ticket not found with id: " + id));
    }

    private SellerSupportTicketResponse mapToResponse(SellerSupportTicket t) {
        List<TicketMessageResponse> msgs = t.getMessages() != null
                ? t.getMessages().stream().map(this::mapToMessageResponse).toList()
                : List.of();

        return SellerSupportTicketResponse.builder()
                .ticketId(t.getTicketId())
                .ticketNumber(t.getTicketNumber())
                .sellerId(t.getSeller() != null ? t.getSeller().getSellerId() : null)
                .sellerName(t.getSeller() != null ? t.getSeller().getName() : null)
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

    private TicketMessageResponse mapToMessageResponse(SellerTicketMessage m) {
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
