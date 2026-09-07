package com.example.project.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ResendEmailServiceTest {

    private ResendEmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Instantiate with dummy placeholder key for unit test
        emailService = new ResendEmailServiceImpl(
                "https://api.resend.com/emails",
                "re_placeholder_test_key",
                "noreply@hinchmart.com",
                "HinchMart",
                "hinchmart@gmail.com"
        );
    }

    @Test
    @DisplayName("Should handle missing or empty recipient email gracefully")
    void shouldHandleEmptyEmailGracefully() {
        assertDoesNotThrow(() -> {
            emailService.sendLoginSuccessEmail("", "Test User", "127.0.0.1", "Mozilla/5.0");
            emailService.sendLoginSuccessEmail(null, "Test User", "127.0.0.1", "Mozilla/5.0");
        });
    }

    @Test
    @DisplayName("Should safely skip sending when API key is a placeholder or not configured")
    void shouldSkipWhenApiKeyIsPlaceholder() {
        assertDoesNotThrow(() -> {
            emailService.sendLoginSuccessEmail(
                    "customer@example.com",
                    "John Doe",
                    "192.168.1.1",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            );
        });
    }

    @Test
    @DisplayName("Should handle generic sendEmail call safely")
    void shouldHandleGenericEmailCallSafely() {
        assertDoesNotThrow(() -> {
            emailService.sendEmail("customer@example.com", "Test Subject", "<p>Hello</p>");
        });
    }
}
