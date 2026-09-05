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
public class ChatMessageResponse {
    private Integer messageId;
    private Integer conversationId;
    private Integer senderId;
    private String senderRole;
    private String content;
    private String attachmentUrl;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime timestamp;
}
