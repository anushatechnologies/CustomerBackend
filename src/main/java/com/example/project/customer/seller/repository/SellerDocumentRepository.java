package com.example.project.customer.seller.repository;

import com.example.project.customer.seller.entity.DocumentType;
import com.example.project.customer.seller.entity.SellerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerDocumentRepository extends JpaRepository<SellerDocument, Long> {

    List<SellerDocument> findBySellerId(String sellerId);

    Optional<SellerDocument> findBySellerIdAndDocumentType(String sellerId, String documentType);

    boolean existsBySellerIdAndDocumentType(String sellerId, String documentType);

    Optional<SellerDocument> findBySellerIdAndDocumentType(String sellerId, DocumentType documentType);

    boolean existsBySellerIdAndDocumentType(String sellerId, DocumentType documentType);
}
