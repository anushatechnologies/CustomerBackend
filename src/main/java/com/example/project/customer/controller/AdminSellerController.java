package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SellerDocumentVaultResponse;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.service.SellerOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/sellers", "/api/admin/seller", "/api/sellers"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {

    private final SellerOnboardingService onboardingService;

    /**
     * List all registered sellers with optional status filter (e.g., PENDING, VERIFIED, REJECTED) and search.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Seller>>> getAllSellers(
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) String search
    ) {
        List<Seller> sellers = onboardingService.getAllSellersForAdmin(status, search);
        return ResponseEntity.ok(ApiResponse.ok("Sellers retrieved successfully", sellers));
    }

    /**
     * List pending sellers awaiting review.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Seller>>> getPendingSellers(
            @RequestParam(required = false) String search
    ) {
        List<Seller> sellers = onboardingService.getAllSellersForAdmin(VerificationStatus.PENDING, search);
        return ResponseEntity.ok(ApiResponse.ok("Pending sellers retrieved successfully", sellers));
    }

    /**
     * Fetch a specific seller's basic profile details.
     */
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponse<Seller>> getSellerById(@PathVariable Integer sellerId) {
        Seller seller = onboardingService.getSellerByIdForAdmin(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller details retrieved successfully", seller));
    }

    /**
     * Fetch a specific seller's complete onboarding summary (Personal, Business, Bank, Docs).
     */
    @GetMapping("/{sellerId}/summary")
    public ResponseEntity<ApiResponse<SellerOnboardingSummaryResponse>> getSellerSummary(@PathVariable Integer sellerId) {
        SellerOnboardingSummaryResponse summary = onboardingService.getSummary(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller onboarding summary retrieved successfully", summary));
    }

    /**
     * Fetch a specific seller's Document Vault and compliance status.
     */
    @GetMapping("/{sellerId}/vault")
    public ResponseEntity<ApiResponse<SellerDocumentVaultResponse>> getSellerVault(@PathVariable Integer sellerId) {
        SellerDocumentVaultResponse vault = onboardingService.getDocumentVault(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller document vault retrieved successfully", vault));
    }

    /**
     * List all documents uploaded by a specific seller.
     */
    @GetMapping("/{sellerId}/documents")
    public ResponseEntity<ApiResponse<List<SellerDocument>>> getSellerDocuments(@PathVariable Integer sellerId) {
        List<SellerDocument> docs = onboardingService.getDocumentsBySellerId(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller documents retrieved successfully", docs));
    }

    /**
     * Admin approve a seller application.
     */
    @PostMapping("/{sellerId}/approve")
    public ResponseEntity<ApiResponse<Seller>> approveSeller(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        Seller approved = onboardingService.verifySellerByAdmin(sellerId, true, remarks);
        return ResponseEntity.ok(ApiResponse.ok("Seller approved successfully", approved));
    }

    /**
     * Admin reject a seller application.
     */
    @PostMapping("/{sellerId}/reject")
    public ResponseEntity<ApiResponse<Seller>> rejectSeller(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        Seller rejected = onboardingService.verifySellerByAdmin(sellerId, false, remarks);
        return ResponseEntity.ok(ApiResponse.ok("Seller rejected successfully", rejected));
    }

    /**
     * Admin verify/reject an individual KYC document for a seller.
     */
    @PutMapping("/{sellerId}/documents/{documentType}/verify")
    public ResponseEntity<ApiResponse<SellerDocument>> verifyDocument(
            @PathVariable Integer sellerId,
            @PathVariable DocumentType documentType,
            @RequestParam(value = "status", defaultValue = "VERIFIED") VerificationStatus status,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        SellerDocument doc = onboardingService.verifyDocumentByAdmin(sellerId, documentType, status, remarks);
        return ResponseEntity.ok(ApiResponse.ok("Document verification status updated successfully", doc));
    }
}
