package com.example.project.customer.service;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerDocumentVaultResponse;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.OnboardingStatus;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.SellerDocumentRepository;
import com.example.project.customer.repository.SellerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@SuppressWarnings("null")
public class SellerOnboardingServiceImpl implements SellerOnboardingService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository documentRepository;
    private final S3ImageService s3ImageService;
    private final Validator validator;

    public SellerOnboardingServiceImpl(SellerRepository sellerRepository,
                                       SellerDocumentRepository documentRepository,
                                       @Autowired(required = false) S3ImageService s3ImageService,
                                       Validator validator) {
        this.sellerRepository = sellerRepository;
        this.documentRepository = documentRepository;
        this.s3ImageService = s3ImageService;
        this.validator = validator;
    }

    @Override
    public Seller savePersonalKyc(PersonalKycRequest request) {
        return savePersonalKyc(request, null);
    }

    @Override
    public Seller savePersonalKyc(PersonalKycRequest request, MultipartFile panCardFile) {
        validateRequest(request);

        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();
        String pan = request.panNumber().trim().toUpperCase();
        String aadhaar = request.aadhaarNumber().trim();

        Seller seller = sellerRepository.findFirstByEmailIgnoreCase(email)
                .orElseGet(() -> Seller.builder()
                        .email(email)
                        .build());

        // Uniqueness check for Mobile/Phone Number
        for (Seller existing : sellerRepository.findAllByPhone(phone)) {
            if (seller.getSellerId() == null || !seller.getSellerId().equals(existing.getSellerId())) {
                throw new ResourceConflictException("Mobile number '" + phone + "' is already registered with another seller");
            }
        }

        // Uniqueness check for PAN
        for (Seller existing : sellerRepository.findAllByPanNumber(pan)) {
            if (seller.getSellerId() == null || !seller.getSellerId().equals(existing.getSellerId())) {
                throw new ResourceConflictException("PAN number '" + pan + "' is already registered with another seller");
            }
        }

        // Uniqueness check for Aadhaar
        for (Seller existing : sellerRepository.findAllByAadhaarNumber(aadhaar)) {
            if (seller.getSellerId() == null || !seller.getSellerId().equals(existing.getSellerId())) {
                throw new ResourceConflictException("Aadhaar number '" + aadhaar + "' is already registered with another seller");
            }
        }

        seller.setName(request.name().trim());
        seller.setEmail(email);
        seller.setPhone(phone);
        seller.setPanNumber(pan);
        seller.setAadhaarNumber(aadhaar);

        if (seller.getOnboardingStatus() == null) {
            seller.setOnboardingStatus(OnboardingStatus.STEP_1);
        }

        Seller saved = sellerRepository.save(seller);

        if (panCardFile != null && !panCardFile.isEmpty()) {
            String fileUrl = handleFileUpload(saved.getSellerId(), DocumentType.PAN, panCardFile, "PAN Card");
            saved.setPanCardUrl(fileUrl);
            saved = sellerRepository.save(saved);
        }

        return saved;
    }

    @Override
    public Seller saveBusinessTax(Integer sellerId, BusinessTaxRequest request) {
        validateRequest(request);

        Seller seller = findSeller(sellerId);
        String gstin = request.gstin().trim().toUpperCase();

        // Uniqueness check for GSTIN
        for (Seller existing : sellerRepository.findAllByGstin(gstin)) {
            if (!sellerId.equals(existing.getSellerId())) {
                throw new ResourceConflictException("GSTIN '" + gstin + "' is already registered with another seller");
            }
        }

        seller.setCompanyName(request.companyName().trim());
        seller.setBusinessType(request.businessType());
        seller.setGstin(gstin);
        seller.setBusinessAddress(request.businessAddress().trim());
        seller.setState(request.state().trim());
        seller.setCity(request.city().trim());
        seller.setPincode(request.pincode().trim());

        if (seller.getOnboardingStatus() == null || seller.getOnboardingStatus() == OnboardingStatus.STEP_1) {
            seller.setOnboardingStatus(OnboardingStatus.STEP_2);
        }

        return sellerRepository.save(seller);
    }

    @Override
    public Seller saveBankDetails(Integer sellerId, BankDetailsRequest request) {
        validateRequest(request);

        Seller seller = findSeller(sellerId);
        String accountNumber = request.accountNumber().trim();

        if (!accountNumber.equals(request.confirmAccountNumber().trim())) {
            throw new ResourceConflictException("Account number and Confirm Account Number do not match");
        }

        // Uniqueness check for Account Number
        for (Seller existing : sellerRepository.findAllByAccountNumber(accountNumber)) {
            if (!sellerId.equals(existing.getSellerId())) {
                throw new ResourceConflictException("Bank account number '" + accountNumber + "' is already registered with another seller");
            }
        }

        seller.setBankName(request.bankName().trim());
        seller.setAccountHolderName(request.accountHolderName().trim());
        seller.setAccountNumber(accountNumber);
        seller.setIfscCode(request.ifscCode().trim().toUpperCase());
        seller.setAccountType(request.accountType().trim().toUpperCase());

        if (seller.getOnboardingStatus() == null
                || seller.getOnboardingStatus() == OnboardingStatus.STEP_1
                || seller.getOnboardingStatus() == OnboardingStatus.STEP_2) {
            seller.setOnboardingStatus(OnboardingStatus.STEP_3);
        }

        return sellerRepository.save(seller);
    }

    @Override
    @Transactional
    public SellerDocument uploadDocument(Integer sellerId, DocumentType documentType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("Document type is required (e.g. GST, AADHAAR, PAN, CHEQUE, OTHER)");
        }

        Seller seller = findSeller(sellerId);
        String title = documentType.name().replace("_", " ");
        String fileUrl = handleFileUpload(sellerId, documentType, file, title);

        // Update corresponding direct shortcut URLs on the Seller profile
        if (documentType == DocumentType.PAN || documentType == DocumentType.PAN_CARD || documentType == DocumentType.COMPANY_PAN) {
            seller.setPanCardUrl(fileUrl);
        } else if (documentType == DocumentType.AADHAAR || documentType == DocumentType.AADHAAR_CARD) {
            seller.setAadhaarCardUrl(fileUrl);
        } else if (documentType == DocumentType.GST || documentType == DocumentType.GST_CERTIFICATE) {
            seller.setGstCertificateUrl(fileUrl);
        }
        sellerRepository.save(seller);

        return documentRepository.findBySellerIdAndDocumentType(sellerId, documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to locate saved document for seller " + sellerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDocument> getDocumentsBySellerId(Integer sellerId) {
        findSeller(sellerId);
        return documentRepository.findBySellerId(sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDocument getDocumentBySellerIdAndType(Integer sellerId, DocumentType documentType) {
        findSeller(sellerId);
        return documentRepository.findBySellerIdAndDocumentType(sellerId, documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Document of type '" + documentType + "' not found for seller " + sellerId));
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOnboardingSummaryResponse getSummary(Integer sellerId) {
        Seller seller = findSeller(sellerId);
        List<SellerDocument> docs = documentRepository.findBySellerId(sellerId);

        boolean personalComplete = isNonBlank(seller.getName())
                && isNonBlank(seller.getEmail())
                && isNonBlank(seller.getPhone())
                && isNonBlank(seller.getPanNumber())
                && isNonBlank(seller.getAadhaarNumber());

        boolean businessComplete = isNonBlank(seller.getCompanyName())
                && seller.getBusinessType() != null
                && isNonBlank(seller.getGstin())
                && isNonBlank(seller.getBusinessAddress())
                && isNonBlank(seller.getState())
                && isNonBlank(seller.getCity())
                && isNonBlank(seller.getPincode());

        boolean bankComplete = isNonBlank(seller.getBankName())
                && isNonBlank(seller.getAccountHolderName())
                && isNonBlank(seller.getAccountNumber())
                && isNonBlank(seller.getIfscCode())
                && isNonBlank(seller.getAccountType());

        boolean isReady = personalComplete && businessComplete && bankComplete;
        String maskedAccNumber = maskAccountNumber(seller.getAccountNumber());

        var personalSummary = new SellerOnboardingSummaryResponse.PersonalSummary(
                seller.getName(),
                seller.getPhone(),
                seller.getEmail(),
                seller.getPanNumber(),
                seller.getAadhaarNumber(),
                personalComplete
        );

        var businessSummary = new SellerOnboardingSummaryResponse.BusinessSummary(
                seller.getCompanyName(),
                seller.getBusinessType(),
                seller.getGstin(),
                seller.getBusinessAddress(),
                seller.getState(),
                seller.getCity(),
                seller.getPincode(),
                businessComplete
        );

        var bankSummary = new SellerOnboardingSummaryResponse.BankSummary(
                seller.getBankName(),
                seller.getAccountHolderName(),
                maskedAccNumber,
                seller.getIfscCode(),
                seller.getAccountType(),
                bankComplete
        );

        var docSummaries = docs.stream()
                .map(d -> new SellerOnboardingSummaryResponse.DocumentSummary(
                d.getId(),
                d.getDocumentType() != null ? d.getDocumentType().name() : null,
                d.getTitle() != null ? d.getTitle() : d.getFileName(),
                d.getFileName(),
                d.getFileUrl(),
                d.getVerificationStatus() != null ? d.getVerificationStatus().name() : null
        ))
                .toList();

        return new SellerOnboardingSummaryResponse(
                seller.getSellerId(),
                seller.getOnboardingStatus(),
                personalSummary,
                businessSummary,
                bankSummary,
                docSummaries,
                isReady
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDocumentVaultResponse getDocumentVault(Integer sellerId) {
        findSeller(sellerId);
        List<SellerDocument> uploadedDocs = documentRepository.findBySellerId(sellerId);

        // Required Document Specs based on Compliance UI
        List<DocSpec> requiredSpecs = List.of(
                new DocSpec(DocumentType.PAN, "PAN Card", "Statutory Permanent Account Number card for identity verification"),
                new DocSpec(DocumentType.AADHAAR, "Aadhaar Card", "12-digit Unique Identification document for primary authorized signatory"),
                new DocSpec(DocumentType.GST, "GSTIN Registration Certificate", "Statutory GSTIN certificate with legal trading identity for B2B invoicing")
        );

        int submittedCount = 0;
        int verifiedCount = 0;
        List<SellerDocumentVaultResponse.DocumentVaultItem> vaultItems = new java.util.ArrayList<>();

        for (DocSpec spec : requiredSpecs) {
            SellerDocument matchingDoc = uploadedDocs.stream()
                    .filter(d -> isMatchingType(d.getDocumentType(), spec.type))
                    .findFirst()
                    .orElse(null);

            if (matchingDoc != null) {
                submittedCount++;
                VerificationStatus status = matchingDoc.getVerificationStatus() != null ? matchingDoc.getVerificationStatus() : VerificationStatus.PENDING;
                if (status == VerificationStatus.VERIFIED) {
                    verifiedCount++;
                }

                vaultItems.add(new SellerDocumentVaultResponse.DocumentVaultItem(
                        matchingDoc.getId(),
                        spec.type.name(),
                        spec.title,
                        spec.description,
                        status.getDisplayName(), // "Pending", "Verified", "Rejected"
                        status.name(),           // "PENDING", "VERIFIED", "REJECTED"
                        true,
                        matchingDoc.getFileName(),
                        matchingDoc.getFileUrl(),
                        matchingDoc.getFileSize(),
                        formatFileSize(matchingDoc.getFileSize()),
                        matchingDoc.getFileType(),
                        matchingDoc.getUploadedAt() != null ? matchingDoc.getUploadedAt().toString() : null,
                        matchingDoc.getRemarks()
                ));
            } else {
                vaultItems.add(new SellerDocumentVaultResponse.DocumentVaultItem(
                        null,
                        spec.type.name(),
                        spec.title,
                        spec.description,
                        VerificationStatus.NOT_UPLOADED.getDisplayName(), // "Not Uploaded"
                        VerificationStatus.NOT_UPLOADED.name(),           // "NOT_UPLOADED"
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        int totalRequired = requiredSpecs.size();
        String progressText = submittedCount + " of " + totalRequired + " submitted";
        boolean isAllSubmitted = submittedCount == totalRequired;
        boolean isAllVerified = isAllSubmitted && verifiedCount == totalRequired;
        boolean hasRejected = vaultItems.stream().anyMatch(d -> VerificationStatus.REJECTED.name().equalsIgnoreCase(d.statusCode()));

        String overallStatus;
        if (hasRejected) {
            overallStatus = VerificationStatus.REJECTED.getDisplayName(); // "Rejected"
        } else if (isAllVerified) {
            overallStatus = VerificationStatus.VERIFIED.getDisplayName(); // "Verified"
        } else if (submittedCount > 0) {
            overallStatus = VerificationStatus.PENDING.getDisplayName();  // "Pending"
        } else {
            overallStatus = VerificationStatus.NOT_UPLOADED.getDisplayName(); // "Not Uploaded"
        }

        return new SellerDocumentVaultResponse(
                sellerId,
                overallStatus,
                totalRequired,
                submittedCount,
                verifiedCount,
                progressText,
                isAllSubmitted,
                isAllVerified,
                vaultItems
        );
    }

    private static class DocSpec {
        final DocumentType type;
        final String title;
        final String description;

        DocSpec(DocumentType type, String title, String description) {
            this.type = type;
            this.title = title;
            this.description = description;
        }
    }

    private boolean isMatchingType(DocumentType docType, DocumentType target) {
        if (docType == null || target == null) return false;
        if (target == DocumentType.PAN) {
            return docType == DocumentType.PAN || docType == DocumentType.PAN_CARD || docType == DocumentType.COMPANY_PAN;
        }
        if (target == DocumentType.AADHAAR) {
            return docType == DocumentType.AADHAAR || docType == DocumentType.AADHAAR_CARD;
        }
        if (target == DocumentType.GST) {
            return docType == DocumentType.GST || docType == DocumentType.GST_CERTIFICATE;
        }
        return docType == target;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return null;
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(java.util.Locale.US, "%.1f MB", mb);
    }

    @Override
    @Transactional
    public Seller finalSubmit(Integer sellerId) {
        Seller seller = findSeller(sellerId);

        boolean personalComplete = isNonBlank(seller.getName())
                && isNonBlank(seller.getEmail())
                && isNonBlank(seller.getPhone())
                && isNonBlank(seller.getPanNumber())
                && isNonBlank(seller.getAadhaarNumber());

        boolean businessComplete = isNonBlank(seller.getCompanyName())
                && seller.getBusinessType() != null
                && isNonBlank(seller.getGstin())
                && isNonBlank(seller.getBusinessAddress())
                && isNonBlank(seller.getState())
                && isNonBlank(seller.getCity())
                && isNonBlank(seller.getPincode());

        boolean bankComplete = isNonBlank(seller.getBankName())
                && isNonBlank(seller.getAccountHolderName())
                && isNonBlank(seller.getAccountNumber())
                && isNonBlank(seller.getIfscCode())
                && isNonBlank(seller.getAccountType());

        if (!personalComplete || !businessComplete || !bankComplete) {
            throw new IllegalStateException("Cannot submit for verification: All mandatory personal, business, and bank details must be completed.");
        }

        seller.setOnboardingStatus(OnboardingStatus.PENDING_REVIEW);
        return sellerRepository.save(seller);
    }

    @Override
    @Transactional
    public Seller verifySellerByAdmin(Integer sellerId, boolean approved, String remarks) {
        Seller seller = findSeller(sellerId);
        List<SellerDocument> documents = documentRepository.findBySellerId(sellerId);

        if (approved) {
            seller.setOnboardingStatus(OnboardingStatus.VERIFIED);
            seller.setVerificationStatus(VerificationStatus.VERIFIED);
            for (SellerDocument doc : documents) {
                doc.setVerificationStatus(VerificationStatus.VERIFIED);
            }
        } else {
            seller.setOnboardingStatus(OnboardingStatus.REJECTED);
            seller.setVerificationStatus(VerificationStatus.REJECTED);
            for (SellerDocument doc : documents) {
                doc.setVerificationStatus(VerificationStatus.REJECTED);
            }
        }

        documentRepository.saveAll(documents);
        return sellerRepository.save(seller);
    }

    @Override
    @Transactional
    public SellerDocument verifyDocumentByAdmin(Integer sellerId, DocumentType documentType, VerificationStatus status, String remarks) {
        Seller seller = findSeller(sellerId);
        SellerDocument doc = documentRepository.findBySellerIdAndDocumentType(sellerId, documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Document of type '" + documentType + "' not found for seller " + sellerId));

        doc.setVerificationStatus(status != null ? status : VerificationStatus.VERIFIED);
        if (remarks != null && !remarks.isBlank()) {
            doc.setRemarks(remarks);
        }
        SellerDocument saved = documentRepository.save(doc);

        List<SellerDocument> allDocs = documentRepository.findBySellerId(sellerId);
        boolean allVerified = allDocs.size() >= 3 && allDocs.stream().allMatch(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED);
        boolean anyRejected = allDocs.stream().anyMatch(d -> d.getVerificationStatus() == VerificationStatus.REJECTED);
        if (anyRejected) {
            seller.setVerificationStatus(VerificationStatus.REJECTED);
            seller.setOnboardingStatus(OnboardingStatus.REJECTED);
        } else if (allVerified) {
            seller.setVerificationStatus(VerificationStatus.VERIFIED);
            seller.setOnboardingStatus(OnboardingStatus.VERIFIED);
        } else {
            seller.setVerificationStatus(VerificationStatus.PENDING);
        }
        sellerRepository.save(seller);

        return saved;
    }

    private <T> void validateRequest(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required and cannot be null");
        }
        if (validator != null) {
            Set<ConstraintViolation<T>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }

    private Seller findSeller(Integer sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String maskAccountNumber(String accNumber) {
        if (accNumber == null || accNumber.isBlank()) {
            return null;
        }
        String clean = accNumber.trim();
        if (clean.length() <= 4) {
            return clean;
        }
        int visibleCount = 4;
        int maskedCount = clean.length() - visibleCount;
        return "X".repeat(Math.max(maskedCount, 8)) + clean.substring(clean.length() - visibleCount);
    }

    private String handleFileUpload(Integer sellerId, DocumentType docType, MultipartFile file, String title) {
        String fileUrl = "https://mock-storage.example.com/sellers/" + sellerId + "/" + file.getOriginalFilename();
        if (s3ImageService != null) {
            try {
                fileUrl = s3ImageService.uploadImage(file, "sellers/" + sellerId + "/documents").getFileUrl();
            } catch (Exception ignored) {
                // fall back to mock URL if AWS credentials not configured
            }
        }

        SellerDocument doc = documentRepository.findBySellerIdAndDocumentType(sellerId, docType)
                .orElseGet(SellerDocument::new);

        doc.setSellerId(sellerId);
        doc.setDocumentType(docType);
        doc.setTitle(title);
        doc.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : docType.name().toLowerCase() + ".pdf");
        doc.setFileUrl(fileUrl);
        doc.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setFileSize(file.getSize());
        doc.setVerificationStatus(VerificationStatus.PENDING);

        documentRepository.save(doc);

        Seller seller = sellerRepository.findById(sellerId).orElse(null);
        if (seller != null) {
            seller.setVerificationStatus(VerificationStatus.PENDING);
            sellerRepository.save(seller);
        }

        return fileUrl;
    }
}
