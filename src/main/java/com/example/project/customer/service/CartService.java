package com.example.project.customer.service;

import com.example.project.customer.dto.AddToCartRequest;
import com.example.project.customer.dto.ApplyCouponResponse;
import com.example.project.customer.dto.CartDto;

public interface CartService {
    CartDto getCart(Integer userId);
    CartDto addItem(Integer userId, AddToCartRequest request);
    CartDto removeItem(Integer userId, Integer cartItemId);
    CartDto clearCart(Integer userId);
    ApplyCouponResponse applyCoupon(Integer userId, String couponCode);
}
