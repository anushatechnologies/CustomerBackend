package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> previewCheckout(
            @Valid @RequestBody CheckoutPreviewRequest request) {
        CheckoutPreviewResponse preview = checkoutService.previewCheckout(101, request);
        return ResponseEntity.ok(ApiResponse.ok("Checkout preview calculated successfully", preview));
    }
}
