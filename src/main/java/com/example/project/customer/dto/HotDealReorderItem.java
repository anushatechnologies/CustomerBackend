package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotDealReorderItem {

    @NotNull(message = "Hot Deal ID is required")
    private Long id;

    @NotNull(message = "displayOrder is required")
    private Integer displayOrder;
}
