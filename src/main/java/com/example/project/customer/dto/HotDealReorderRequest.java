package com.example.project.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotDealReorderRequest {

    @NotEmpty(message = "Reorder list cannot be empty")
    @Valid
    private List<HotDealReorderItem> items;
}
