package com.example.project.customer.seller.controller;

import com.example.project.customer.seller.dto.*;
import com.example.project.customer.seller.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/profile")
    public SellerProfileResponse getSellerProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return sellerService.getSellerProfile(resolveSellerId(authorization));
    }

    @PutMapping("/profile")
    public SellerProfileResponse updateSellerProfile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @Valid @RequestBody UpdateSellerProfileRequest request) {
        return sellerService.updateSellerProfile(resolveSellerId(authorization), request);
    }

    @GetMapping("/documents")
    public List<SellerDocumentResponse> getDocuments(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return sellerService.getDocuments(resolveSellerId(authorization));
    }

    @PostMapping("/documents")
    public ResponseEntity<SellerDocumentResponse> createDocument(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                @Valid @RequestBody CreateSellerDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.createDocument(resolveSellerId(authorization), request));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable Long id) {
        sellerService.deleteDocument(resolveSellerId(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verification/submit")
    public SubmitVerificationResponse submitForVerification(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return sellerService.submitForVerification(resolveSellerId(authorization));
    }

    @GetMapping("/verification/status")
    public SellerVerificationStatusResponse getVerificationStatus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return sellerService.getVerificationStatus(resolveSellerId(authorization));
    }

    private String resolveSellerId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("Authentication required");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw new SecurityException("Authentication required");
        }
        return token;
    }
}
