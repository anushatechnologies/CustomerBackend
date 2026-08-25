package com.example.project.customer.seller.service;

import com.example.project.customer.seller.dto.CreateSellerDocumentRequest;
import com.example.project.customer.seller.dto.SellerDocumentResponse;
import com.example.project.customer.seller.dto.SellerProfileResponse;
import com.example.project.customer.seller.dto.SellerVerificationStatusResponse;
import com.example.project.customer.seller.dto.SubmitVerificationResponse;
import com.example.project.customer.seller.dto.UpdateSellerProfileRequest;

import java.util.List;

public interface SellerService {

    SellerProfileResponse getSellerProfile(String sellerId);

    SellerProfileResponse updateSellerProfile(String sellerId, UpdateSellerProfileRequest request);

    List<SellerDocumentResponse> getDocuments(String sellerId);

    SellerDocumentResponse createDocument(String sellerId, CreateSellerDocumentRequest request);

    void deleteDocument(String sellerId, Long documentId);

    SubmitVerificationResponse submitForVerification(String sellerId);

    SellerVerificationStatusResponse getVerificationStatus(String sellerId);
}
