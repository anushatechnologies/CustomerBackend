package com.example.project.customer.repository;

import com.example.project.customer.entity.Enquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
    
    // Find all enquiries for a seller
    Page<Enquiry> findBySellerId(String sellerId, Pageable pageable);
    
    // Find enquiry by ID and seller ID (ownership check)
    Optional<Enquiry> findByIdAndSellerId(String id, String sellerId);
    
    // Find enquiries by status and seller
    Page<Enquiry> findBySellerIdAndStatus(String sellerId, String status, Pageable pageable);
}
