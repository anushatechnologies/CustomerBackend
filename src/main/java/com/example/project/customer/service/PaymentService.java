package com.example.project.customer.service;

import com.example.project.customer.dto.PaymentOrderCreateRequest;
import com.example.project.customer.dto.PaymentOrderCreateResponse;
import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;

import java.util.List;

public interface PaymentService {

    PaymentOrderCreateResponse createPaymentOrder(Integer customerId, PaymentOrderCreateRequest request);

    PaymentStatusResponse verifyPayment(Integer customerId, PaymentVerifyRequest request);

    PaymentStatusResponse getPaymentStatus(String razorpayPaymentId);

    PaymentStatusResponse getLatestOrderPaymentStatus(Integer orderId);

    List<PaymentStatusResponse> getCustomerPayments(Integer customerId);

    void handleWebhook(String payload, String signatureHeader);
}
