package com.example.project.customer.repository;

import com.example.project.customer.entity.Estimation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstimationRepository extends JpaRepository<Estimation, Long> {

    Page<Estimation> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);

    Optional<Estimation> findByIdAndCustomerId(Long id, Integer customerId);

    Optional<Estimation> findByEstimationNumber(String estimationNumber);
}
