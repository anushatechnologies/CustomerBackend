package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OrderDisputeResponse {

    @Builder.Default
    @JsonProperty("success")
    private boolean success = true;

    @JsonProperty("disputeId")
    private String disputeId;

    @JsonProperty("orderId")
    private Integer orderId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
