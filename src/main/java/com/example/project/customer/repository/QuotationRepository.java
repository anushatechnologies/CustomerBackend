package com.example.project.customer.repository;

import com.example.project.customer.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Integer> {
    List<Quotation> findByRfq_RfqIdOrderByUnitPriceAsc(Integer rfqId);
    int countByRfq_RfqId(Integer rfqId);
}
