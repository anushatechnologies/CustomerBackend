package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemUpdateRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CartSyncRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.dto.SwitchStoreRequest;

public interface CartService {
    CartResponse getCart(Integer userId);
    CartResponse addItem(Integer userId, CartItemRequest request);
    CartResponse updateItem(Integer userId, Integer id, CartItemUpdateRequest request);
    CartResponse removeItem(Integer userId, Integer id);
    CartResponse syncCart(Integer userId, CartSyncRequest request);
    void clearCart(Integer userId);
    CouponResponse applyCoupon(Integer userId, String couponCode);
    CartResponse removeCoupon(Integer userId);
    CartResponse switchStore(Integer userId, SwitchStoreRequest request);
}
