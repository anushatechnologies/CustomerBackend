package com.example.project.customer.repository;

import com.example.project.customer.entity.CustomerDocument;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Integer> {

    List<CustomerDocument> findByCustomer_CustomerIdOrderByUploadedAtDesc(Integer customerId);

    List<CustomerDocument> findByCustomer_CustomerIdAndStatusOrderByUploadedAtDesc(Integer customerId, VerificationStatus status);

    Optional<CustomerDocument> findByCustomer_CustomerIdAndDocumentType(Integer customerId, DocumentType documentType);

    boolean existsByCustomer_CustomerIdAndDocumentType(Integer customerId, DocumentType documentType);
}
