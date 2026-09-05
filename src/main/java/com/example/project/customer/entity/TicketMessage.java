package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "sender_role", nullable = false)
    private String senderRole; // USER, SUPPORT_AGENT

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
