package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ChatMessageRequest;
import com.example.project.customer.dto.ChatMessageResponse;
import com.example.project.customer.dto.ConversationResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.StartConversationRequest;
import com.example.project.customer.entity.ChatMessage;
import com.example.project.customer.entity.Conversation;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ChatMessageRepository;
import com.example.project.customer.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Integer userId) {
        int uid = userId != null ? userId : 101;
        List<Conversation> list = conversationRepository.findByUserOrSeller(uid);
        return list.stream().map(c -> mapToConversationResponse(c, uid)).toList();
    }

    @Override
    public ConversationResponse startConversation(Integer userId, StartConversationRequest request) {
        int buyerId = userId != null ? userId : 101;
        String topic = request.getTopic() != null ? request.getTopic() : "GENERAL";
        String refId = request.getReferenceId();

        Conversation conversation = null;
        if (refId != null && !refId.isBlank()) {
            conversation = conversationRepository.findByBuyerIdAndSellerIdAndTopicAndReferenceId(
                    buyerId, request.getSellerId(), topic, refId).orElse(null);
        }

        if (conversation == null) {
            String title = request.getTitle() != null && !request.getTitle().isBlank()
                    ? request.getTitle() : "Inquiry with Seller #" + request.getSellerId();

            conversation = Conversation.builder()
                    .buyerId(buyerId)
                    .sellerId(request.getSellerId())
                    .topic(topic)
                    .referenceId(refId)
                    .title(title)
                    .unreadBuyer(0)
                    .unreadSeller(1)
                    .build();
            conversation = conversationRepository.save(conversation);
        }

        // Save initial message
        ChatMessage msg = ChatMessage.builder()
                .conversation(conversation)
                .senderId(buyerId)
                .senderRole("BUYER")
                .content(request.getInitialMessage())
                .messageType("TEXT")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(msg);

        conversation.setLastMessageText(request.getInitialMessage());
        conversation.setLastMessageTimestamp(LocalDateTime.now());
        conversation.setUnreadSeller(conversation.getUnreadSeller() + 1);
        conversation = conversationRepository.save(conversation);

        return mapToConversationResponse(conversation, buyerId);
    }

    @Override
    public ApiResponse<List<ChatMessageResponse>> getMessages(Integer conversationId, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 50;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Conversation conv = findConversation(conversationId);
        Page<ChatMessage> pageResult = messageRepository.findByConversation_ConversationIdOrderByTimestampAsc(conversationId, pageable);

        // Mark unread messages as read
        for (ChatMessage m : pageResult.getContent()) {
            if (Boolean.FALSE.equals(m.getIsRead())) {
                m.setIsRead(true);
            }
        }

        List<ChatMessageResponse> data = pageResult.getContent().stream()
                .map(this::mapToMessageResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Conversation messages retrieved successfully", data, meta);
    }

    @Override
    public ChatMessageResponse sendMessage(Integer conversationId, Integer senderId, String senderRole, ChatMessageRequest request) {
        Conversation conv = findConversation(conversationId);
        int sid = senderId != null ? senderId : 101;
        String role = senderRole != null ? senderRole : "BUYER";

        ChatMessage msg = ChatMessage.builder()
                .conversation(conv)
                .senderId(sid)
                .senderRole(role)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
        ChatMessage saved = messageRepository.save(msg);

        conv.setLastMessageText(request.getContent());
        conv.setLastMessageTimestamp(LocalDateTime.now());
        if ("BUYER".equalsIgnoreCase(role)) {
            conv.setUnreadSeller((conv.getUnreadSeller() != null ? conv.getUnreadSeller() : 0) + 1);
        } else {
            conv.setUnreadBuyer((conv.getUnreadBuyer() != null ? conv.getUnreadBuyer() : 0) + 1);
        }
        conversationRepository.save(conv);

        return mapToMessageResponse(saved);
    }

    private Conversation findConversation(Integer id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    private ConversationResponse mapToConversationResponse(Conversation c, int currentUserId) {
        int unread = currentUserId == c.getBuyerId() ? (c.getUnreadBuyer() != null ? c.getUnreadBuyer() : 0)
                : (c.getUnreadSeller() != null ? c.getUnreadSeller() : 0);

        return ConversationResponse.builder()
                .conversationId(c.getConversationId())
                .buyerId(c.getBuyerId())
                .sellerId(c.getSellerId())
                .topic(c.getTopic())
                .referenceId(c.getReferenceId())
                .title(c.getTitle())
                .lastMessageText(c.getLastMessageText())
                .lastMessageTimestamp(c.getLastMessageTimestamp())
                .unreadCount(unread)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse mapToMessageResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .messageId(m.getMessageId())
                .conversationId(m.getConversation().getConversationId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .messageType(m.getMessageType())
                .isRead(m.getIsRead())
                .timestamp(m.getTimestamp())
                .build();
    }
}
