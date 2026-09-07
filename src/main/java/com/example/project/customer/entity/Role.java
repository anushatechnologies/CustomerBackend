package com.example.project.customer.entity;

public enum Role {
    CUSTOMER,
    SELLER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return CUSTOMER;
        }
        String clean = roleStr.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        if ("BUYER".equals(clean)) {
            return CUSTOMER;
        }
        try {
            return Role.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return CUSTOMER;
        }
    }
}
