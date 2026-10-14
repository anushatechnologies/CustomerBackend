package com.example.project.customer.dto.seller;

import com.example.project.customer.dto.TicketMessageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerSupportTicketResponse {
    private Integer ticketId;
    private String ticketNumber;
    private Integer sellerId;
    private String sellerName;
    private String subject;
    private String category;
    private String priority;
    private String status;
    private Integer orderId;
    private List<TicketMessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
