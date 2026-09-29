package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryVerificationResponse {

    private boolean success;
    private String message;
    private Integer orderId;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime deliveredAt;
}
