package com.example.project.customer.repository;

import com.example.project.customer.entity.Rfq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Integer> {
    Optional<Rfq> findByRfqNumber(String rfqNumber);
    List<Rfq> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId);
    List<Rfq> findByCustomer_CustomerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String status);
    Page<Rfq> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM Rfq r WHERE r.customer.customerId = :userId")
    int countByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
