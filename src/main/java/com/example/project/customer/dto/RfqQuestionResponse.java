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
public class RfqQuestionResponse {
    private Integer questionId;
    private Integer rfqId;
    private String question;
    private String response;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
}
