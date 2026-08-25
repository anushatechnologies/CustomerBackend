package com.example.project.customer.service;

import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;

public interface CheckoutService {
    CheckoutPreviewResponse previewCheckout(Integer userId, CheckoutPreviewRequest request);
}
