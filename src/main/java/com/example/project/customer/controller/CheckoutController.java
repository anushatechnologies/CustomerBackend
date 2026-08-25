package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public ApiResponse<CheckoutPreviewResponse> previewCheckout(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(checkoutService.previewCheckout(uid, request));
    }
}
