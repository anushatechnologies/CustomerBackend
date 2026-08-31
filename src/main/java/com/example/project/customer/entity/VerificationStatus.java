package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationStatus {
    NOT_UPLOADED("Not Uploaded"),
    PENDING("Pending"),
    UNDER_REVIEW("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    EXPIRED("Expired");

    private final String displayName;

    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonValue
    public String getJsonValue() {
        return name();
    }

    @JsonCreator
    public static VerificationStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        if (normalized.equals("NOT_UPLOADED") || normalized.equals("NOTUPLOADED")) {
            return NOT_UPLOADED;
        }
        for (VerificationStatus s : values()) {
            if (s.name().equalsIgnoreCase(normalized) || s.displayName.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return PENDING;
    }
}
