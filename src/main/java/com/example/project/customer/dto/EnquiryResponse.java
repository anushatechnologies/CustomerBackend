package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryResponse {
    private String id;
    private String buyerName;
    private String projectName;
    private String buyerCity;
    private String buyerState;
    private List<Map<String, Object>> requestedItems;
    private String status;
    private LocalDateTime createdAt;
}
