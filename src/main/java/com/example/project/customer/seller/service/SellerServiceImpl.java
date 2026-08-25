package com.example.project.customer.seller.service;

import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.seller.dto.*;
import com.example.project.customer.seller.entity.*;
import com.example.project.customer.seller.repository.SellerDocumentRepository;
import com.example.project.customer.seller.repository.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerServiceImpl implements SellerService {

    private static final Set<String> SUPPORTED_DOC_TYPES = Set.of(
            "GST_CERTIFICATE", "PAN", "MSME", "CHEQUE", "TRADE_LICENSE", "OTHER"
    );

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository sellerDocumentRepository;

    public SellerServiceImpl(SellerRepository sellerRepository, SellerDocumentRepository sellerDocumentRepository) {
        this.sellerRepository = sellerRepository;
        this.sellerDocumentRepository = sellerDocumentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(String sellerId) {
        Seller seller = findSeller(sellerId);
        return toProfileResponse(seller);
    }

    @Override
    public SellerProfileResponse updateSellerProfile(String sellerId, UpdateSellerProfileRequest request) {
        Seller seller = findSeller(sellerId);
        validateBusinessProfile(request);
        applyProfileUpdate(seller, request);
        return toProfileResponse(sellerRepository.save(seller));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDocumentResponse> getDocuments(String sellerId) {
        findSeller(sellerId);
        return sellerDocumentRepository.findBySellerId(sellerId).stream().map(this::toDocumentResponse).toList();
    }

    @Override
    public SellerDocumentResponse createDocument(String sellerId, CreateSellerDocumentRequest request) {
        Seller seller = findSeller(sellerId);
        DocumentType documentType = parseDocumentType(request.getDocumentType());
        validateFile(request);
        if (sellerDocumentRepository.existsBySellerIdAndDocumentType(sellerId, documentType.name())) {
            throw new ResourceConflictException("This document has already been uploaded for the seller.");
        }

        SellerDocument document = new SellerDocument();
        document.setSellerId(seller.getId());
        document.setDocumentType(documentType);
        document.setFileName(request.getFileName());
        document.setFileUrl(request.getFileUrl());
        document.setFileType(request.getFileType());
        document.setVerificationStatus(VerificationStatus.PENDING);

        return toDocumentResponse(sellerDocumentRepository.save(document));
    }

    @Override
    public void deleteDocument(String sellerId, Long documentId) {
        SellerDocument document = sellerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        if (!sellerId.equals(document.getSellerId())) {
            throw new SecurityException("You are not authorized to delete this document.");
        }
        sellerDocumentRepository.delete(document);
    }

    @Override
    public SubmitVerificationResponse submitForVerification(String sellerId) {
        Seller seller = findSeller(sellerId);
        if (seller.getVerificationStatus() == VerificationStatus.UNDER_REVIEW) {
            throw new ResourceConflictException("Seller is already under review.");
        }
        if (seller.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new ResourceConflictException("Seller is already verified.");
        }
        validateCompletionForVerification(seller);
        seller.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        seller.setSubmittedAt(Instant.now());
        sellerRepository.save(seller);

        SubmitVerificationResponse response = new SubmitVerificationResponse();
        response.setSellerId(sellerId);
        response.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        response.setSubmittedAt(seller.getSubmittedAt());
        response.setMessage("Submitted for verification successfully.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SellerVerificationStatusResponse getVerificationStatus(String sellerId) {
        Seller seller = findSeller(sellerId);
        VerificationChecklistResponse checklist = buildChecklist(seller);
        SellerVerificationStatusResponse response = new SellerVerificationStatusResponse();
        response.setVerificationStatus(seller.getVerificationStatus());
        response.setCompletionPercentage(calculateCompletionPercentage(seller));
        response.setChecklist(checklist);
        response.setSubmittedAt(seller.getSubmittedAt());
        return response;
    }

    private void validateBusinessProfile(UpdateSellerProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile request is required");
        }

        String country = request.getAddress() != null ? request.getAddress().getCountry() : request.getCountry();
        String state = request.getAddress() != null ? request.getAddress().getState() : request.getState();
        String city = request.getAddress() != null ? request.getAddress().getCity() : request.getCity();
        String pincode = request.getAddress() != null ? request.getAddress().getPincode() : request.getPincode();
        String completeAddress = request.getAddress() != null ? request.getAddress().getCompleteAddress() : request.getCompleteAddress();
        String gstin = request.getLegal() != null ? request.getLegal().getGstin() : request.getGstin();
        String pan = request.getLegal() != null ? request.getLegal().getPan() : request.getPan();
        String cin = request.getLegal() != null ? request.getLegal().getCin() : request.getCin();
        String msme = request.getLegal() != null ? request.getLegal().getMsme() : request.getMsme();

        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be blank");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State cannot be blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
        if (pincode == null || !pincode.matches("^[1-9][0-9]{5}$")) {
            throw new IllegalArgumentException("Pincode must be a valid 6-digit value");
        }
        if (completeAddress == null || completeAddress.isBlank()) {
            throw new IllegalArgumentException("Complete address cannot be blank");
        }
        if (gstin != null && !gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
            throw new IllegalArgumentException("GSTIN is invalid");
        }
        if (pan != null && !pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
            throw new IllegalArgumentException("PAN is invalid");
        }
        if (cin != null && !cin.isBlank() && !cin.matches("^[A-Z]{1}[0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$")) {
            throw new IllegalArgumentException("CIN is invalid");
        }
        if (msme != null && msme.isBlank()) {
            throw new IllegalArgumentException("MSME cannot be blank");
        }
    }

    private void applyProfileUpdate(Seller seller, UpdateSellerProfileRequest request) {
        if (request.getDescription() != null) {
            seller.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            seller.setCountry(request.getAddress().getCountry());
            seller.setState(request.getAddress().getState());
            seller.setCity(request.getAddress().getCity());
            seller.setPincode(request.getAddress().getPincode());
            seller.setCompleteAddress(request.getAddress().getCompleteAddress());
        } else {
            if (request.getCountry() != null) seller.setCountry(request.getCountry());
            if (request.getState() != null) seller.setState(request.getState());
            if (request.getCity() != null) seller.setCity(request.getCity());
            if (request.getPincode() != null) seller.setPincode(request.getPincode());
            if (request.getCompleteAddress() != null) seller.setCompleteAddress(request.getCompleteAddress());
        }
        if (request.getLegal() != null) {
            if (request.getLegal().getGstin() != null) seller.setGstin(request.getLegal().getGstin());
            if (request.getLegal().getPan() != null) seller.setPan(request.getLegal().getPan());
            if (request.getLegal().getCin() != null) seller.setCin(request.getLegal().getCin());
            if (request.getLegal().getMsme() != null) seller.setMsme(request.getLegal().getMsme());
        } else {
            if (request.getGstin() != null) seller.setGstin(request.getGstin());
            if (request.getPan() != null) seller.setPan(request.getPan());
            if (request.getCin() != null) seller.setCin(request.getCin());
            if (request.getMsme() != null) seller.setMsme(request.getMsme());
        }
        if (request.getServiceAreas() != null) {
            seller.setServiceAreas(request.getServiceAreas().stream().filter(area -> area != null && !area.isBlank()).collect(Collectors.toList()));
        }
        if (request.getWarehouseLocations() != null) {
            seller.setWarehouseLocations(request.getWarehouseLocations().stream().map(item -> new WarehouseLocation(item.getState(), item.getCity(), item.getAddress())).collect(Collectors.toList()));
        }
    }

    private void validateFile(CreateSellerDocumentRequest request) {
        String fileType = request.getFileType();
        if (fileType == null || (!fileType.equalsIgnoreCase("application/pdf")
                && !fileType.equalsIgnoreCase("image/jpeg")
                && !fileType.equalsIgnoreCase("image/jpg")
                && !fileType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, JPG, JPEG, PNG.");
        }
        String url = request.getFileUrl();
        if (url == null || !url.matches("^(https?|ftp)://.*$")) {
            throw new IllegalArgumentException("Invalid file URL.");
        }
    }

    private DocumentType parseDocumentType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Document type is required");
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_DOC_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid document type");
        }
        return DocumentType.valueOf(normalized);
    }

    private void validateCompletionForVerification(Seller seller) {
        List<String> requiredChecks = new ArrayList<>();
        if (seller.getCompanyName() == null || seller.getCompanyName().isBlank()) requiredChecks.add("Company name");
        if (seller.getCountry() == null || seller.getCountry().isBlank()) requiredChecks.add("Country");
        if (seller.getState() == null || seller.getState().isBlank()) requiredChecks.add("State");
        if (seller.getCity() == null || seller.getCity().isBlank()) requiredChecks.add("City");
        if (seller.getPincode() == null || !seller.getPincode().matches("^[1-9][0-9]{5}$")) requiredChecks.add("Pincode");
        if (seller.getCompleteAddress() == null || seller.getCompleteAddress().isBlank()) requiredChecks.add("Address");
        if (seller.getGstin() == null || !seller.getGstin().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) requiredChecks.add("GSTIN");
        if (seller.getPan() == null || !seller.getPan().matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) requiredChecks.add("PAN");
        if (seller.getMsme() == null || seller.getMsme().isBlank()) requiredChecks.add("MSME");
        if (seller.getServiceAreas() == null || seller.getServiceAreas().isEmpty()) requiredChecks.add("Service areas");
        boolean hasGst = sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.GST_CERTIFICATE.name()).isPresent();
        boolean hasPan = sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.PAN.name()).isPresent();
        boolean hasMsme = sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.MSME.name()).isPresent();
        if (!hasGst) requiredChecks.add("GST certificate");
        if (!hasPan) requiredChecks.add("PAN document");
        if (!hasMsme) requiredChecks.add("MSME document");

        if (!requiredChecks.isEmpty()) {
            throw new IllegalArgumentException("Profile incomplete for verification. Missing: " + String.join(", ", requiredChecks));
        }
    }

    private Seller findSeller(String sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
    }

    private SellerProfileResponse toProfileResponse(Seller seller) {
        SellerProfileResponse response = new SellerProfileResponse();
        response.setId(seller.getId());
        response.setCompanyName(seller.getCompanyName());
        response.setBusinessType(seller.getBusinessType());
        response.setDescription(seller.getDescription());
        response.setAddress(toAddressDTO(seller));
        response.setLegal(toLegalDTO(seller));
        response.setServiceAreas(seller.getServiceAreas());
        response.setVerificationStatus(seller.getVerificationStatus());
        response.setCompletionPercentage(calculateCompletionPercentage(seller));
        return response;
    }

    private AddressDTO toAddressDTO(Seller seller) {
        AddressDTO address = new AddressDTO();
        address.setCountry(seller.getCountry());
        address.setState(seller.getState());
        address.setCity(seller.getCity());
        address.setPincode(seller.getPincode());
        address.setCompleteAddress(seller.getCompleteAddress());
        return address;
    }

    private LegalInfoDTO toLegalDTO(Seller seller) {
        LegalInfoDTO legalInfo = new LegalInfoDTO();
        legalInfo.setGstin(seller.getGstin());
        legalInfo.setPan(seller.getPan());
        legalInfo.setCin(seller.getCin());
        legalInfo.setMsme(seller.getMsme());
        return legalInfo;
    }

    private SellerDocumentResponse toDocumentResponse(SellerDocument document) {
        SellerDocumentResponse response = new SellerDocumentResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType().name());
        response.setFileName(document.getFileName());
        response.setFileUrl(document.getFileUrl());
        response.setFileType(document.getFileType());
        response.setVerificationStatus(document.getVerificationStatus());
        return response;
    }

    private VerificationChecklistResponse buildChecklist(Seller seller) {
        VerificationChecklistResponse checklist = new VerificationChecklistResponse();
        checklist.setCompanyProfile(seller.getCompanyName() != null && !seller.getCompanyName().isBlank());
        checklist.setBusinessAddress(seller.getCountry() != null && !seller.getCountry().isBlank() && seller.getState() != null && !seller.getState().isBlank() && seller.getCity() != null && !seller.getCity().isBlank() && seller.getPincode() != null && seller.getPincode().matches("^[1-9][0-9]{5}$") && seller.getCompleteAddress() != null && !seller.getCompleteAddress().isBlank());
        checklist.setGstin(seller.getGstin() != null && seller.getGstin().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"));
        checklist.setPan(seller.getPan() != null && seller.getPan().matches("^[A-Z]{5}[0-9]{4}[A-Z]$"));
        checklist.setCin(seller.getCin() != null && !seller.getCin().isBlank());
        checklist.setMsme(seller.getMsme() != null && !seller.getMsme().isBlank());
        checklist.setServiceAreas(seller.getServiceAreas() != null && !seller.getServiceAreas().isEmpty());
        checklist.setWarehouseLocations(seller.getWarehouseLocations() != null && !seller.getWarehouseLocations().isEmpty());
        checklist.setGstCertificate(sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.GST_CERTIFICATE.name()).isPresent());
        checklist.setPanDocument(sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.PAN.name()).isPresent());
        checklist.setMsmeDocument(sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.MSME.name()).isPresent());
        checklist.setCancelledCheque(sellerDocumentRepository.findBySellerIdAndDocumentType(seller.getId(), DocumentType.CHEQUE.name()).isPresent());
        return checklist;
    }

    private int calculateCompletionPercentage(Seller seller) {
        int completed = 0;
        int total = 0;

        total++; completed += seller.getCompanyName() != null && !seller.getCompanyName().isBlank() ? 1 : 0;
        total++; completed += seller.getCountry() != null && !seller.getCountry().isBlank() ? 1 : 0;
        total++; completed += seller.getState() != null && !seller.getState().isBlank() ? 1 : 0;
        total++; completed += seller.getCity() != null && !seller.getCity().isBlank() ? 1 : 0;
        total++; completed += seller.getPincode() != null && seller.getPincode().matches("^[1-9][0-9]{5}$") ? 1 : 0;
        total++; completed += seller.getCompleteAddress() != null && !seller.getCompleteAddress().isBlank() ? 1 : 0;
        total++; completed += seller.getGstin() != null && seller.getGstin().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$") ? 1 : 0;
        total++; completed += seller.getPan() != null && seller.getPan().matches("^[A-Z]{5}[0-9]{4}[A-Z]$") ? 1 : 0;
        total++; completed += seller.getCin() != null && !seller.getCin().isBlank() ? 1 : 0;
        total++; completed += seller.getMsme() != null && !seller.getMsme().isBlank() ? 1 : 0;
        total++; completed += seller.getServiceAreas() != null && !seller.getServiceAreas().isEmpty() ? 1 : 0;
        total++; completed += seller.getWarehouseLocations() != null && !seller.getWarehouseLocations().isEmpty() ? 1 : 0;

        return (int) Math.round((completed * 100.0) / total);
    }
}
