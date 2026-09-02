package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SellerEnquiryItemResponse;
import com.example.project.customer.dto.SellerQuotationCreateRequest;
import com.example.project.customer.dto.SellerQuotationRecordResponse;
import com.example.project.customer.service.SellerQuotationManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerQuotationController {

    private final SellerQuotationManagementService quotationService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping("/enquiries")
    public ResponseEntity<ApiResponse<List<SellerEnquiryItemResponse>>> getEnquiries() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        List<SellerEnquiryItemResponse> enquiries = quotationService.getEnquiries(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Buyer enquiries retrieved successfully", enquiries));
    }

    @PostMapping("/quotations")
    public ResponseEntity<Map<String, Object>> createQuotation(@Valid @RequestBody SellerQuotationCreateRequest request) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        Map<String, Object> result = quotationService.createQuotation(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/quotations")
    public ResponseEntity<ApiResponse<List<SellerQuotationRecordResponse>>> getQuotations() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        List<SellerQuotationRecordResponse> quotations = quotationService.getQuotations(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Quotations retrieved successfully", quotations));
    }
}
