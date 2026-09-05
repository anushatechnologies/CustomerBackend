package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequest {

    @NotNull(message = "Seller ID is required")
    private Integer sellerId;

    private String topic; // PRODUCT, ORDER, RFQ, GENERAL

    private String referenceId;

    private String title;

    @NotBlank(message = "Initial message is required")
    private String initialMessage;
}
