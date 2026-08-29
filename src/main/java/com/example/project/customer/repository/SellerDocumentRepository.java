package com.example.project.customer.repository;

import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.SellerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerDocumentRepository extends JpaRepository<SellerDocument, Long> {

    List<SellerDocument> findBySellerId(Integer sellerId);

    Optional<SellerDocument> findBySellerIdAndDocumentType(Integer sellerId, DocumentType documentType);

    boolean existsBySellerIdAndDocumentType(Integer sellerId, DocumentType documentType);
}
