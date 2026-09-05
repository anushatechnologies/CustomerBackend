package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String attachmentUrl;

    private String messageType; // TEXT, IMAGE, DOCUMENT
}
