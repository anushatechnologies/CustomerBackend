package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.CustomerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CustomerDocumentRepository;
import com.example.project.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@SuppressWarnings("null")
public class CustomerDocumentServiceImpl implements CustomerDocumentService {

    private final CustomerDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;
    private final S3ImageService s3ImageService;

    public CustomerDocumentServiceImpl(CustomerDocumentRepository documentRepository,
                                       CustomerRepository customerRepository,
                                       S3ImageService s3ImageService) {
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
        this.s3ImageService = s3ImageService;
    }

    @Override
    public CustomerDocumentResponse submitDocument(Integer customerId, CustomerDocumentRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));

        // If the customer already submitted a document for this type, update/resubmit it
        CustomerDocument document = documentRepository
                .findByCustomer_CustomerIdAndDocumentType(customerId, request.documentType())
                .orElseGet(() -> {
                    CustomerDocument newDoc = new CustomerDocument();
                    newDoc.setCustomer(customer);
                    newDoc.setDocumentType(request.documentType());
                    return newDoc;
                });

        String oldFileUrl = document.getFileUrl();

        document.setTitle(request.title());
        document.setDocumentNumber(request.documentNumber());
        document.setFileName(request.fileName());
        document.setFileUrl(request.fileUrl());
        document.setFileSize(request.fileSize());
        document.setExpiresOn(request.expiresOn());
        document.setStatus(VerificationStatus.PENDING);
        document.setRejectionReason(null);
        document.setVerifiedAt(null);
        document.setUploadedAt(LocalDateTime.now());

        CustomerDocument saved = documentRepository.save(document);

        if (oldFileUrl != null && !oldFileUrl.isBlank() && !oldFileUrl.equals(request.fileUrl())) {
            s3ImageService.deleteImage(oldFileUrl);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDocumentResponse> getDocumentsByCustomerId(Integer customerId, VerificationStatus status) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found with id: " + customerId);
        }

        List<CustomerDocument> docs;
        if (status != null) {
            docs = documentRepository.findByCustomer_CustomerIdAndStatusOrderByUploadedAtDesc(customerId, status);
        } else {
            docs = documentRepository.findByCustomer_CustomerIdOrderByUploadedAtDesc(customerId);
        }

        return docs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDocumentResponse getDocumentById(Integer documentId) {
        return toResponse(findDocument(documentId));
    }

    @Override
    public CustomerDocumentResponse verifyDocument(Integer documentId) {
        CustomerDocument document = findDocument(documentId);
        document.setStatus(VerificationStatus.VERIFIED);
        document.setVerifiedAt(LocalDateTime.now());
        document.setRejectionReason(null);
        return toResponse(documentRepository.save(document));
    }

    @Override
    public CustomerDocumentResponse rejectDocument(Integer documentId, String reason) {
        CustomerDocument document = findDocument(documentId);
        document.setStatus(VerificationStatus.REJECTED);
        document.setRejectionReason(reason);
        document.setVerifiedAt(null);
        return toResponse(documentRepository.save(document));
    }

    @Override
    public void deleteDocument(Integer documentId) {
        CustomerDocument doc = findDocument(documentId);
        String fileUrl = doc.getFileUrl();
        documentRepository.delete(doc);

        if (fileUrl != null && !fileUrl.isBlank()) {
            s3ImageService.deleteImage(fileUrl);
        }
    }

    private CustomerDocument findDocument(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
    }

    private CustomerDocumentResponse toResponse(CustomerDocument doc) {
        return new CustomerDocumentResponse(
                doc.getDocumentId(),
                doc.getCustomer().getCustomerId(),
                doc.getDocumentType(),
                doc.getTitle(),
                doc.getDocumentNumber(),
                doc.getFileName(),
                doc.getFileUrl(),
                doc.getFileSize(),
                doc.getStatus(),
                doc.getRejectionReason(),
                doc.getExpiresOn(),
                doc.getUploadedAt(),
                doc.getVerifiedAt()
        );
    }
}
