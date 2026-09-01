package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationEntity {

    @Id
    @Column(name = "quotation_id", length = 30)
    private String id;

    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "enquiry_id", length = 30)
    private String enquiryId;

    @Column(name = "buyer_name", nullable = false, length = 255)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false, length = 255)
    private String buyerEmail;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SENT, VIEWED, ACCEPTED, EXPIRED, CONVERTED_TO_ORDER

    @Column(name = "items", columnDefinition = "JSON", nullable = false)
    private String items; // JSON array of quotation items

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "calculation_details", columnDefinition = "JSON")
    private String calculationDetails; // JSON with subtotal, gst, freight, etc

    @Column(name = "freight_charges")
    private Long freightCharges;

    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "delivery_timeline", length = 100)
    private String deliveryTimeline;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
