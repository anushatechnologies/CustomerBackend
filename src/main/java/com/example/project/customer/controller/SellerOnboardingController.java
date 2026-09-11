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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Seller> submitPersonalKycJson(@Valid @RequestBody PersonalKycRequest request) {
        Seller saved = onboardingService.savePersonalKyc(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping(value = "/step1-personal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<Seller> submitBusinessTax(
            @PathVariable Integer sellerId,
            @Valid @RequestBody BusinessTaxRequest request) {
        Seller saved = onboardingService.saveBusinessTax(sellerId, request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{sellerId}/step3-bank")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<Seller> submitBankDetails(
            @PathVariable Integer sellerId,
            @Valid @RequestBody BankDetailsRequest request) {
        Seller saved = onboardingService.saveBankDetails(sellerId, request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/{sellerId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<SellerDocument> uploadDocument(
            @PathVariable Integer sellerId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        SellerDocument doc = onboardingService.uploadDocument(sellerId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/{sellerId}/documents")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<List<SellerDocument>> getDocuments(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getDocumentsBySellerId(sellerId));
    }

    @GetMapping("/{sellerId}/documents/{documentType}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<SellerDocument> getDocumentByType(
            @PathVariable Integer sellerId,
            @PathVariable DocumentType documentType) {
        return ResponseEntity.ok(onboardingService.getDocumentBySellerIdAndType(sellerId, documentType));
    }

    @GetMapping("/{sellerId}/summary")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<SellerOnboardingSummaryResponse> getSummary(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getSummary(sellerId));
    }

    @GetMapping({
            "/{sellerId}/vault",
            "/{sellerId}/document-vault",
            "/{sellerId}/compliance"
    })
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<SellerDocumentVaultResponse> getDocumentVault(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getDocumentVault(sellerId));
    }

    @PostMapping("/{sellerId}/final-submit")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<Seller> finalSubmit(@PathVariable Integer sellerId) {
        Seller saved = onboardingService.finalSubmit(sellerId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = {"/{sellerId}/admin/approve", "/{sellerId}/approve", "/{sellerId}/verify"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Seller> approveSellerByAdmin(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        String finalRemarks = remarks;
        boolean isApproved = true;
        if (body != null) {
            if (body.get("remarks") != null) {
                finalRemarks = String.valueOf(body.get("remarks"));
            } else if (body.get("reason") != null) {
                finalRemarks = String.valueOf(body.get("reason"));
            }
            if (body.containsKey("verified")) {
                isApproved = Boolean.parseBoolean(String.valueOf(body.get("verified")));
            } else if (body.containsKey("approved")) {
                isApproved = Boolean.parseBoolean(String.valueOf(body.get("approved")));
            }
        }
        Seller result = onboardingService.verifySellerByAdmin(sellerId, isApproved, finalRemarks);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = {"/{sellerId}/admin/reject", "/{sellerId}/reject"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Seller> rejectSellerByAdmin(
            @PathVariable Integer sellerId,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        String finalRemarks = remarks;
        if (body != null) {
            if (body.get("remarks") != null) {
                finalRemarks = String.valueOf(body.get("remarks"));
            } else if (body.get("reason") != null) {
                finalRemarks = String.valueOf(body.get("reason"));
            }
        }
        Seller rejected = onboardingService.verifySellerByAdmin(sellerId, false, finalRemarks);
        return ResponseEntity.ok(rejected);
    }

    @PutMapping("/{sellerId}/documents/{documentType}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDocument> verifyDocument(
            @PathVariable Integer sellerId,
            @PathVariable DocumentType documentType,
            @RequestParam(value = "status", defaultValue = "VERIFIED") VerificationStatus status,
            @RequestParam(value = "remarks", required = false) String remarks) {
        SellerDocument doc = onboardingService.verifyDocumentByAdmin(sellerId, documentType, status, remarks);
        return ResponseEntity.ok(doc);
    }

    @GetMapping({"/all", "/list"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Seller>> getAllSellers(
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(onboardingService.getAllSellersForAdmin(status, search));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Seller>> getPendingSellers(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(onboardingService.getAllSellersForAdmin(VerificationStatus.PENDING, search));
    }

    @GetMapping("/{sellerId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentSeller(#sellerId)")
    public ResponseEntity<Seller> getSellerById(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(onboardingService.getSellerByIdForAdmin(sellerId));
    }
}
