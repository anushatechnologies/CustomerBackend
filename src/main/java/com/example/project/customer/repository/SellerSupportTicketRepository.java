package com.example.project.customer.repository;

import com.example.project.customer.entity.SellerSupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerSupportTicketRepository extends JpaRepository<SellerSupportTicket, Integer> {
    Optional<SellerSupportTicket> findByTicketNumber(String ticketNumber);
    Page<SellerSupportTicket> findBySeller_SellerIdOrderByCreatedAtDesc(Integer sellerId, Pageable pageable);
    Page<SellerSupportTicket> findBySeller_SellerIdAndStatusOrderByCreatedAtDesc(Integer sellerId, String status, Pageable pageable);
    Page<SellerSupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<SellerSupportTicket> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
