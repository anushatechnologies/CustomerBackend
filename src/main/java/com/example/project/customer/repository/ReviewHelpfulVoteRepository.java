package com.example.project.customer.repository;

import com.example.project.customer.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {
    boolean existsByReview_IdAndCustomer_CustomerId(Long reviewId, Integer customerId);
}
