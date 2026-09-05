package com.example.project.customer.repository;

import com.example.project.customer.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Integer> {
    List<TicketMessage> findByTicket_TicketIdOrderByTimestampAsc(Integer ticketId);
}
