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
    List<Rfq> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Rfq> findByUserIdAndStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String status);
    Page<Rfq> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    int countByUserId(Integer userId);
}
