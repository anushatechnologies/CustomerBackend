package com.example.project.customer.seller.dto;

import com.example.project.customer.seller.entity.VerificationStatus;

import java.time.Instant;

public class SubmitVerificationResponse {

    private String sellerId;
    private VerificationStatus verificationStatus;
    private Instant submittedAt;
    private String message;

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
