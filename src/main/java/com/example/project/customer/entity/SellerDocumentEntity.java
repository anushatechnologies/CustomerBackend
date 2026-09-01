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

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDocumentEntity {

    @Id
    @Column(name = "document_id", length = 20)
    private String id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // GSTIN, PAN, INCORPORATION, MSME, TRADE_LICENSE

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", length = 2000)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;
}
