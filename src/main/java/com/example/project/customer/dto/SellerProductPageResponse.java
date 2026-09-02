package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerProductPageResponse {

    @Builder.Default
    private boolean success = true;

    private long total;
    private int page;
    private int limit;

    @Builder.Default
    private List<ProductResponse> data = new ArrayList<>();
}
