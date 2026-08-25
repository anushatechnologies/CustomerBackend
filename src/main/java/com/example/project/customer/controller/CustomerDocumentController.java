package com.example.project.customer.controller;

import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.dto.RejectDocumentRequest;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.service.CustomerDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CustomerDocumentController {

    private final CustomerDocumentService documentService;

    public CustomerDocumentController(CustomerDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/customers/{customerId}/documents")
    public ResponseEntity<CustomerDocumentResponse> submitDocument(
            @PathVariable Integer customerId,
            @Valid @RequestBody CustomerDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.submitDocument(customerId, request));
    }

    @GetMapping("/customers/{customerId}/documents")
    public List<CustomerDocumentResponse> getCustomerDocuments(
            @PathVariable Integer customerId,
            @RequestParam(required = false) VerificationStatus status) {
        return documentService.getDocumentsByCustomerId(customerId, status);
    }

    @GetMapping("/documents/{documentId}")
    public CustomerDocumentResponse getDocumentById(@PathVariable Integer documentId) {
        return documentService.getDocumentById(documentId);
    }

    @RequestMapping(value = "/documents/{documentId}/verify", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public CustomerDocumentResponse verifyDocument(@PathVariable Integer documentId) {
        return documentService.verifyDocument(documentId);
    }

    @RequestMapping(value = "/documents/{documentId}/reject", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public CustomerDocumentResponse rejectDocument(
            @PathVariable Integer documentId,
            @Valid @RequestBody RejectDocumentRequest request) {
        return documentService.rejectDocument(documentId, request.reason());
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Integer documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
