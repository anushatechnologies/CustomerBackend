package com.example.project.customer.dto;

import com.example.project.customer.entity.BusinessType;
import com.example.project.customer.entity.OnboardingStatus;

import java.util.List;

public record SellerOnboardingSummaryResponse(
        Integer sellerId,
        OnboardingStatus onboardingStatus,
        PersonalSummary personalDetails,
        BusinessSummary businessTaxDetails,
        BankSummary bankDetails,
        List<DocumentSummary> documents,
        boolean isReadyForSubmission
) {
    public record PersonalSummary(
            String name,
            String phone,
            String email,
            String panNumber,
            String aadhaarNumber,
            boolean isComplete
    ) {
    }

    public record BusinessSummary(
            String companyName,
            BusinessType businessType,
            String gstin,
            String businessAddress,
            String state,
            String city,
            String pincode,
            boolean isComplete
    ) {
    }

    public record BankSummary(
            String bankName,
            String accountHolderName,
            String maskedAccountNumber,
            String ifscCode,
            String accountType,
            boolean isComplete
    ) {
    }

    public record DocumentSummary(
            Long documentId,
            String documentType,
            String title,
            String fileName,
            String fileUrl,
            String status
    ) {
    }
}
