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
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "category")
    @Builder.Default
    private String category = "GENERAL"; // DELIVERY, PAYMENT, QUALITY, RFQ, TECHNICAL, GENERAL

    @Column(name = "priority")
    @Builder.Default
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "status")
    @Builder.Default
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @Column(name = "order_id")
    private Integer orderId;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

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
        if (this.ticketNumber == null) {
            this.ticketNumber = "TKT-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
