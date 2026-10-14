package com.example.project.customer.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerSupportTicketRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String category; // PAYOUT, COMMISSION, INVENTORY, ORDER_DISPUTE, ONBOARDING, TECHNICAL, GENERAL

    private String priority; // LOW, MEDIUM, HIGH, URGENT

    private Integer orderId;

    @NotBlank(message = "Initial message is required")
    private String message;
}
