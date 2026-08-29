package com.example.project.customer.service;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.Seller;
import org.springframework.web.multipart.MultipartFile;

public interface SellerOnboardingService {

    Seller savePersonalKyc(PersonalKycRequest request);

    Seller savePersonalKyc(PersonalKycRequest request, MultipartFile panCardFile);

    Seller saveBusinessTax(Integer sellerId, BusinessTaxRequest request);

    Seller saveBankDetails(Integer sellerId, BankDetailsRequest request);

    SellerOnboardingSummaryResponse getSummary(Integer sellerId);

    Seller finalSubmit(Integer sellerId);
}
