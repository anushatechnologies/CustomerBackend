package com.example.project.customer.dto;

import com.example.project.customer.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Integer productId;
    private String productTitle;
    private Integer customerId;
    private String customerName;
    private Integer orderId;
    private Integer orderItemId;
    private int rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private int helpfulCount;
    private boolean verifiedPurchase;
    private List<String> imageUrls;
    private Instant createdAt;
    private Instant updatedAt;
}
