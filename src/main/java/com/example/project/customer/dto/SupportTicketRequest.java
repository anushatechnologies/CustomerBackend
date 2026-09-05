package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String category; // DELIVERY, PAYMENT, QUALITY, RFQ, TECHNICAL, GENERAL

    private String priority; // LOW, MEDIUM, HIGH, URGENT

    private Integer orderId;

    @NotBlank(message = "Initial message is required")
    private String message;
}
