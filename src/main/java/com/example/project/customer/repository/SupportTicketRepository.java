package com.example.project.customer.repository;

import com.example.project.customer.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<SupportTicket> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, String status, Pageable pageable);
}
