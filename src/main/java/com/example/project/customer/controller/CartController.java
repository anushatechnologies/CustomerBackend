package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCart(101);
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addOrUpdateItem(@Valid @RequestBody CartItemRequest request) {
        CartResponse updated = cartService.addItem(101, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart updated successfully", updated));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Integer productId) {
        CartResponse updated = cartService.removeItem(101, productId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart successfully", updated));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart(101);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared successfully", null));
    }

    @PostMapping("/coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> applyCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse couponResponse = cartService.applyCoupon(101, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Coupon " + request.getCode() + " applied successfully", couponResponse));
    }
}
