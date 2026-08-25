package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CouponResponse;

public interface CartService {
    CartResponse getCart(Integer userId);
    CartResponse addItem(Integer userId, CartItemRequest request);
    CartResponse removeItem(Integer userId, Integer productId);
    void clearCart(Integer userId);
    CouponResponse applyCoupon(Integer userId, String couponCode);
}
