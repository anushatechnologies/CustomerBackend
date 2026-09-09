package com.example.project.customer.dto;

import com.example.project.customer.entity.StoreStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusUpdateRequest {

    @NotNull(message = "Store status is required")
    private StoreStatus status;

    private String remarks;
}
