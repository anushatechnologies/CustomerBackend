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
public class SellerTicketStatusUpdateRequest {

    @NotBlank(message = "Status is required (e.g. OPEN, IN_PROGRESS, RESOLVED, CLOSED)")
    private String status;

    private String priority;
}
