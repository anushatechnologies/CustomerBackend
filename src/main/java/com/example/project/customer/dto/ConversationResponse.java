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
public class ConversationResponse {
    private Integer conversationId;
    private Integer buyerId;
    private Integer sellerId;
    private String topic;
    private String referenceId;
    private String title;
    private String lastMessageText;
    private LocalDateTime lastMessageTimestamp;
    private Integer unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
