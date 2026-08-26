package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.entity.VerificationStatus;

import java.util.List;

public interface CustomerDocumentService {

    CustomerDocumentResponse submitDocument(Integer customerId, CustomerDocumentRequest request);

    List<CustomerDocumentResponse> getDocumentsByCustomerId(Integer customerId, VerificationStatus status);

    CustomerDocumentResponse getDocumentById(Integer documentId);

    CustomerDocumentResponse verifyDocument(Integer documentId);

    CustomerDocumentResponse rejectDocument(Integer documentId, String reason);

    void deleteDocument(Integer documentId);
}
