package com.example.project.customer.dto.estimation;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveProductRequest {

    @NotNull(message = "Product ID is required to resolve item match")
    @JsonAlias({"selectedProductId", "productId"})
    private Integer productId;
}
