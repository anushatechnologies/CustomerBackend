package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentType {
    GST,
    GST_CERTIFICATE,
    AADHAAR,
    AADHAAR_CARD,
    PAN,
    PAN_CARD,
    COMPANY_PAN,
    INCORPORATION_CERTIFICATE,
    MSME_UDYAM,
    MSME,
    CHEQUE,
    TRADE_LICENSE,
    OTHER;

    @JsonCreator
    public static DocumentType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if (normalized.equals("AADHAR") || normalized.equals("AADHAR_CARD")) {
            return AADHAAR;
        }
        for (DocumentType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown document type: " + value + ". Supported types include: GST, AADHAAR, PAN, CHEQUE, OTHER");
    }
}
