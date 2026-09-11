package com.hinchexpress.trip.repository;

import com.hinchexpress.trip.entity.DeliveryOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryOutboxEventRepository extends JpaRepository<DeliveryOutboxEvent, Long> {

    List<DeliveryOutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}