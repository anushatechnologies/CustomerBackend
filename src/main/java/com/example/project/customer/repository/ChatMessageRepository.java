package com.example.project.customer.repository;

import com.example.project.customer.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findByConversation_ConversationIdOrderByTimestampAsc(Integer conversationId);
    Page<ChatMessage> findByConversation_ConversationIdOrderByTimestampAsc(Integer conversationId, Pageable pageable);
}
