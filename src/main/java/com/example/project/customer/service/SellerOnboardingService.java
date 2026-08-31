package com.example.project.customer.service;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerDocumentVaultResponse;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.example.project.customer.entity.VerificationStatus;

public interface SellerOnboardingService {

    Seller savePersonalKyc(PersonalKycRequest request);

    Seller savePersonalKyc(PersonalKycRequest request, MultipartFile panCardFile);

    Seller saveBusinessTax(Integer sellerId, BusinessTaxRequest request);

    Seller saveBankDetails(Integer sellerId, BankDetailsRequest request);

    SellerDocument uploadDocument(Integer sellerId, DocumentType documentType, MultipartFile file);

    List<SellerDocument> getDocumentsBySellerId(Integer sellerId);

    SellerDocument getDocumentBySellerIdAndType(Integer sellerId, DocumentType documentType);

    SellerOnboardingSummaryResponse getSummary(Integer sellerId);

    SellerDocumentVaultResponse getDocumentVault(Integer sellerId);

    Seller finalSubmit(Integer sellerId);

    Seller verifySellerByAdmin(Integer sellerId, boolean approved, String remarks);

    SellerDocument verifyDocumentByAdmin(Integer sellerId, DocumentType documentType, VerificationStatus status, String remarks);
}
