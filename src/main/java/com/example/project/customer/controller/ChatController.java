package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ChatMessageRequest;
import com.example.project.customer.dto.ChatMessageResponse;
import com.example.project.customer.dto.ConversationResponse;
import com.example.project.customer.dto.StartConversationRequest;
import com.example.project.customer.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserContextUtil userContextUtil;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        Integer userId = userContextUtil.getCurrentUserId();
        List<ConversationResponse> list = chatService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.ok("Conversations retrieved successfully", list));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @Valid @RequestBody StartConversationRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        ConversationResponse conv = chatService.startConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Conversation initiated successfully", conv));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        ApiResponse<List<ChatMessageResponse>> response = chatService.getMessages(id, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Integer id,
            @Valid @RequestBody ChatMessageRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        ChatMessageResponse message = chatService.sendMessage(id, userId, "BUYER", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", message));
    }
}
