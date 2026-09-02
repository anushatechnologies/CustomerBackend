package com.example.project.customer.service;

import com.example.project.customer.dto.SellerEnquiryItemResponse;
import com.example.project.customer.dto.SellerQuotationCreateRequest;
import com.example.project.customer.dto.SellerQuotationRecordResponse;

import java.util.List;
import java.util.Map;

public interface SellerQuotationManagementService {

    List<SellerEnquiryItemResponse> getEnquiries(Integer sellerId);

    Map<String, Object> createQuotation(Integer sellerId, SellerQuotationCreateRequest request);

    List<SellerQuotationRecordResponse> getQuotations(Integer sellerId);
}
