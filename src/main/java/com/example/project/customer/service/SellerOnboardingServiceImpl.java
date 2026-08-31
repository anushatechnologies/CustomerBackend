package com.example.project.customer.service;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
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
        doc.setVerificationStatus(VerificationStatus.PENDING);

        documentRepository.save(doc);
        return fileUrl;
    }
}
