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

import java.time.LocalDateTime;

@Entity
@Table(name = "buyer_enquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enquiry {

    @Id
    @Column(name = "enquiry_id", length = 30)
    private String id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "buyer_name", nullable = false, length = 255)
    private String buyerName;

    @Column(name = "project_name", length = 255)
    private String projectName;

    @Column(name = "buyer_city", length = 50)
    private String buyerCity;

    @Column(name = "buyer_state", length = 50)
    private String buyerState;

    @Column(name = "requested_items", columnDefinition = "JSON")
    private String requestedItems; // JSON array of items

    @Column(name = "status", nullable = false, length = 20)
    private String status; // NEW, QUOTED, ACCEPTED, REJECTED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
