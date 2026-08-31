package com.example.project.customer.controller;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerDocumentVaultResponse;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.service.SellerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({
        "/api/sellers/onboarding",
        "/api/seller/onboarding",
        "/v1/api/sellers/onboarding",
        "/api/v1/sellers/onboarding",
        "/v1/api/seller/onboarding"
})
public class SellerOnboardingController {

    private final SellerOnboardingService onboardingService;

    public SellerOnboardingController(SellerOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping(value = "/step1-personal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Seller> submitPersonalKycJson(@Valid @RequestBody PersonalKycRequest request) {
        Seller saved = onboardingService.savePersonalKyc(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping(value = "/step1-personal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Seller> submitPersonalKycMultipart(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("panNumber") String panNumber,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam("panCardFile") MultipartFile panCardFile) {

        if (panCardFile == null || panCardFile.isEmpty()) {
            throw new IllegalArgumentException("Upload PAN Card file is mandatory (JPG, PNG, or PDF)");
        }

        PersonalKycRequest request = new PersonalKycRequest(name, email, phone, panNumber, aadhaarNumber);
        Seller saved = onboardingService.savePersonalKyc(request, panCardFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{sellerId}/step2-business")
    public ResponseEntity<Seller> submitBusinessTax(
            @PathVariable Integer sellerId,
            @Valid @RequestBody BusinessTaxRequest request) {
        Seller saved = onboardingService.saveBusinessTax(sellerId, request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{sellerId}/step3-bank")
    public ResponseEntity<Seller> submitBankDetails(
            @PathVariable Integer sellerId,
            @Valid @RequestBody BankDetailsRequest request) {
        Seller saved = onboardingService.saveBankDetails(sellerId, request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/{sellerId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerDocument> uploadDocument(
            @PathVariable Integer sellerId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        SellerDocument doc = onboardingService.uploadDocument(sellerId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/{sellerId}/documents")
    public ResponseEntity<List<SellerDocument>> getDocuments(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getDocumentsBySellerId(sellerId));
    }

    @GetMapping("/{sellerId}/documents/{documentType}")
    public ResponseEntity<SellerDocument> getDocumentByType(
            @PathVariable Integer sellerId,
            @PathVariable DocumentType documentType) {
        return ResponseEntity.ok(onboardingService.getDocumentBySellerIdAndType(sellerId, documentType));
    }

    @GetMapping("/{sellerId}/summary")
    public ResponseEntity<SellerOnboardingSummaryResponse> getSummary(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getSummary(sellerId));
    }

    @GetMapping({
            "/{sellerId}/vault",
            "/{sellerId}/document-vault",
            "/{sellerId}/compliance"
    })
    public ResponseEntity<SellerDocumentVaultResponse> getDocumentVault(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getDocumentVault(sellerId));
    }

    @PostMapping("/{sellerId}/final-submit")
    public ResponseEntity<Seller> finalSubmit(@PathVariable Integer sellerId) {
        Seller saved = onboardingService.finalSubmit(sellerId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{sellerId}/admin/approve")
    public ResponseEntity<Seller> approveSellerByAdmin(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks) {
        Seller verified = onboardingService.verifySellerByAdmin(sellerId, true, remarks);
        return ResponseEntity.ok(verified);
    }

    @PostMapping("/{sellerId}/admin/reject")
    public ResponseEntity<Seller> rejectSellerByAdmin(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks) {
        Seller rejected = onboardingService.verifySellerByAdmin(sellerId, false, remarks);
        return ResponseEntity.ok(rejected);
    }

    @PutMapping("/{sellerId}/documents/{documentType}/verify")
    public ResponseEntity<SellerDocument> verifyDocument(
            @PathVariable Integer sellerId,
            @PathVariable DocumentType documentType,
            @RequestParam(value = "status", defaultValue = "VERIFIED") VerificationStatus status,
            @RequestParam(value = "remarks", required = false) String remarks) {
        SellerDocument doc = onboardingService.verifyDocumentByAdmin(sellerId, documentType, status, remarks);
        return ResponseEntity.ok(doc);
    }
}
