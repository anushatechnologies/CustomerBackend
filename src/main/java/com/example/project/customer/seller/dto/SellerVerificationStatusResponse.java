package com.example.project.customer.seller.dto;

import com.example.project.customer.seller.entity.VerificationStatus;

import java.time.Instant;

public class SellerVerificationStatusResponse {

    private VerificationStatus verificationStatus;
    private Integer completionPercentage;
    private VerificationChecklistResponse checklist;
    private Instant submittedAt;

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public VerificationChecklistResponse getChecklist() {
        return checklist;
    }

    public void setChecklist(VerificationChecklistResponse checklist) {
        this.checklist = checklist;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
