package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryResponse {
    private Integer orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private Integer itemCount;
    private String firstItemTitle;
    private String firstItemImage;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDelivery;
}
