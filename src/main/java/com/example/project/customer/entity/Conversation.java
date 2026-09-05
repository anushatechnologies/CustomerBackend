package com.example.project.customer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Integer conversationId;

    @Column(name = "buyer_id", nullable = false)
    private Integer buyerId;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "topic")
    @Builder.Default
    private String topic = "GENERAL"; // PRODUCT, ORDER, RFQ, GENERAL

    @Column(name = "reference_id")
    private String referenceId; // orderId, rfqId, productId

    @Column(name = "title")
    private String title;

    @Column(name = "last_message_text", columnDefinition = "TEXT")
    private String lastMessageText;

    @Column(name = "last_message_timestamp")
    private LocalDateTime lastMessageTimestamp;

    @Column(name = "unread_buyer")
    @Builder.Default
    private Integer unreadBuyer = 0;

    @Column(name = "unread_seller")
    @Builder.Default
    private Integer unreadSeller = 0;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
