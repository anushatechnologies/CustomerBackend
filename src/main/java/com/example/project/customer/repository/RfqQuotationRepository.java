package com.example.project.customer.repository;

import com.example.project.customer.entity.RfqQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqQuotationRepository extends JpaRepository<RfqQuotation, Integer> {
    List<RfqQuotation> findByRfq_RfqId(Integer rfqId);
    Optional<RfqQuotation> findByQuoteIdAndRfq_RfqId(Integer quoteId, Integer rfqId);
}
