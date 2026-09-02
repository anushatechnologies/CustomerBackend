package com.example.project.customer.service;

import com.example.project.customer.dto.SellerDocumentItemResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.SellerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.repository.SellerDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerDocumentVaultServiceImpl implements SellerDocumentVaultService {

    private final SellerOnboardingService onboardingService;
    private final SellerDocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SellerDocumentItemResponse> getSellerDocuments(Integer sellerId) {
        List<SellerDocument> documents = documentRepository.findBySellerId(sellerId);
        List<SellerDocumentItemResponse> responses = new ArrayList<>();

        for (SellerDocument doc : documents) {
            String status = doc.getVerificationStatus() != null ? doc.getVerificationStatus().name() : "PENDING";
            if ("VERIFIED".equalsIgnoreCase(status)) {
                status = "APPROVED";
            }
            responses.add(SellerDocumentItemResponse.builder()
                    .id("doc_" + doc.getId())
                    .documentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : "DOCUMENT")
                    .name(getDocumentReadableName(doc.getDocumentType()))
                    .fileName(doc.getFileName())
                    .fileUrl(doc.getFileUrl())
                    .status(status)
                    .uploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt() : Instant.now())
                    .build());
        }

        return responses;
    }

    @Override
    public Map<String, Object> uploadDocument(Integer sellerId, DocumentType documentType, MultipartFile file) {
        SellerDocument doc = onboardingService.uploadDocument(sellerId, documentType, file);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "doc_" + doc.getId());
        data.put("documentType", doc.getDocumentType() != null ? doc.getDocumentType().name() : documentType.name());
        data.put("fileName", doc.getFileName());
        data.put("status", "PENDING");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Document uploaded and queued for verification");
        response.put("data", data);
        return response;
    }

    private String getDocumentReadableName(DocumentType type) {
        if (type == null) return "Document";
        switch (type) {
            case GST:
            case GSTIN:
            case GST_CERTIFICATE:
                return "GST Registration Certificate";
            case PAN:
            case PAN_CARD:
            case COMPANY_PAN:
                return "Company PAN Card";
            case AADHAAR:
            case AADHAAR_CARD:
                return "Aadhaar Card";
            case INCORPORATION:
            case INCORPORATION_CERTIFICATE:
                return "Certificate of Incorporation";
            case MSME:
            case MSME_UDYAM:
                return "MSME / Udyam Registration";
            case TRADE_LICENSE:
                return "Trade License";
            case CHEQUE:
                return "Cancelled Cheque";
            default:
                return type.name().replace("_", " ");
        }
    }
}
