package com.example.project.customer.repository;

import com.example.project.customer.entity.Rfq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Integer> {
    Page<Rfq> findByUserId(Integer userId, Pageable pageable);
    Page<Rfq> findByUserIdAndStatusIgnoreCase(Integer userId, String status, Pageable pageable);
    Optional<Rfq> findByRfqIdAndUserId(Integer rfqId, Integer userId);
    Optional<Rfq> findByRfqNumber(String rfqNumber);
    long countByUserId(Integer userId);
    long countByUserIdAndStatusIgnoreCase(Integer userId, String status);
}
