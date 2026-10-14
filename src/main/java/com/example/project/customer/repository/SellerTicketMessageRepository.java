package com.example.project.customer.repository;

import com.example.project.customer.entity.SellerTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerTicketMessageRepository extends JpaRepository<SellerTicketMessage, Integer> {
    List<SellerTicketMessage> findByTicket_TicketIdOrderByTimestampAsc(Integer ticketId);
}
