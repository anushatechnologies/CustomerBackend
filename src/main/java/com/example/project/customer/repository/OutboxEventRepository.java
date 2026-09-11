package com.example.project.customer.repository;

import com.example.project.customer.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByAggregateTypeAndAggregateIdAndEventType(String aggregateType, String aggregateId, String eventType);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
}
