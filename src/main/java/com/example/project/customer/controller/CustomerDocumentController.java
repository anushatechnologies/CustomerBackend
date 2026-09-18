package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.dto.RejectDocumentRequest;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.service.CustomerDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequiredArgsConstructor
public class CustomerDocumentController {

    private final CustomerDocumentService documentService;
    private final UserContextUtil userContextUtil;

    private void validateAdminOrSelf(Integer customerId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = userContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required: Please log in.");
        }
        if (!currentUserId.equals(customerId)) {
            throw new ForbiddenException("Access denied: You can only view or manage your own documents.");
        }
    }

    private void validateAdminOrDocumentOwner(CustomerDocumentResponse doc) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = userContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required: Please log in.");
        }
        if (doc.customerId() == null || !doc.customerId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied: You do not have permission to access this document.");
        }
    }

    @PostMapping("/customers/{customerId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerDocumentResponse> submitDocument(
            @PathVariable Integer customerId,
            @Valid @RequestBody CustomerDocumentRequest request) {
        validateAdminOrSelf(customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.submitDocument(customerId, request));
    }

    @GetMapping("/customers/{customerId}/documents")
    @PreAuthorize("isAuthenticated()")
    public List<CustomerDocumentResponse> getCustomerDocuments(
            @PathVariable Integer customerId,
            @RequestParam(required = false) VerificationStatus status) {
        validateAdminOrSelf(customerId);
        return documentService.getDocumentsByCustomerId(customerId, status);
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public CustomerDocumentResponse getDocumentById(@PathVariable Integer documentId) {
        CustomerDocumentResponse doc = documentService.getDocumentById(documentId);
        validateAdminOrDocumentOwner(doc);
        return doc;
    }

    @RequestMapping(value = "/documents/{documentId}/verify", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDocumentResponse verifyDocument(@PathVariable Integer documentId) {
        return documentService.verifyDocument(documentId);
    }

    @RequestMapping(value = "/documents/{documentId}/reject", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDocumentResponse rejectDocument(
            @PathVariable Integer documentId,
            @Valid @RequestBody RejectDocumentRequest request) {
        return documentService.rejectDocument(documentId, request.reason());
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable Integer documentId) {
        CustomerDocumentResponse doc = documentService.getDocumentById(documentId);
        validateAdminOrDocumentOwner(doc);
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
