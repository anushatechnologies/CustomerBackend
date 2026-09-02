package com.example.project.customer.repository;

import com.example.project.customer.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Integer> {
    List<Quotation> findByRfq_RfqIdOrderByUnitPriceAsc(Integer rfqId);
    List<Quotation> findByVendorIdOrderByCreatedAtDesc(Integer vendorId);
    int countByRfq_RfqId(Integer rfqId);
}
