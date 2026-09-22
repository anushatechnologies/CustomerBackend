package com.example.project.customer.dto.estimation;

import com.example.project.customer.entity.EstimationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstimationResponse {
    private Long estimationId;
    private String estimationNumber;
    private Integer customerId;
    private String originalFileName;
    private String originalFileType;
    private Long originalFileSize;
    private String originalFileUrl;

    private EstimationStatus status;
    private String failureReason;

    private String quotationPdfUrl;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;

    private int totalItemsCount;
    private int matchedItemsCount;
    private int ambiguousItemsCount;
    private int unavailableItemsCount;

    private String notes;

    @Builder.Default
    private List<EstimationItemResponse> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
