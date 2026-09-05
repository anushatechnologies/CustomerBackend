package com.example.project.customer.repository;

import com.example.project.customer.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    @Query("SELECT c FROM Conversation c WHERE c.buyerId = :userId OR c.sellerId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByUserOrSeller(@Param("userId") Integer userId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndTopicAndReferenceId(
            Integer buyerId, Integer sellerId, String topic, String referenceId);
}
