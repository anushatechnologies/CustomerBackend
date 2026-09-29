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
public class RiderOfferRespondRequest {

    @NotBlank(message = "Action must be ACCEPT or REJECT")
    private String action; // ACCEPT, REJECT

    private String reason;
}
