package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ChatMessageRequest;
import com.example.project.customer.dto.ChatMessageResponse;
import com.example.project.customer.dto.ConversationResponse;
import com.example.project.customer.dto.StartConversationRequest;

import java.util.List;

public interface ChatService {
    List<ConversationResponse> getConversations(Integer userId);
    ConversationResponse startConversation(Integer userId, StartConversationRequest request);
    ApiResponse<List<ChatMessageResponse>> getMessages(Integer conversationId, int page, int limit);
    ChatMessageResponse sendMessage(Integer conversationId, Integer senderId, String senderRole, ChatMessageRequest request);
}
