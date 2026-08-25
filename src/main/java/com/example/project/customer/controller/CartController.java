package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.AddToCartRequest;
import com.example.project.customer.dto.ApplyCouponRequest;
import com.example.project.customer.dto.ApplyCouponResponse;
import com.example.project.customer.dto.CartDto;
import com.example.project.customer.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartDto> getCart(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(cartService.getCart(uid));
    }

    @PostMapping("/items")
    public ApiResponse<CartDto> addItem(@RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                        @Valid @RequestBody AddToCartRequest request) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok("Cart updated successfully", cartService.addItem(uid, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartDto> removeItem(@RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                           @PathVariable Integer cartItemId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok("Item removed from cart", cartService.removeItem(uid, cartItemId));
    }

    @DeleteMapping
    public ApiResponse<CartDto> clearCart(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok("Cart cleared", cartService.clearCart(uid));
    }

    @PostMapping("/coupon")
    public ApiResponse<ApplyCouponResponse> applyCoupon(@RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                                        @Valid @RequestBody ApplyCouponRequest request) {
        Integer uid = userId != null ? userId : 101;
        ApplyCouponResponse response = cartService.applyCoupon(uid, request.getCode());
        return ApiResponse.ok("Coupon " + response.getCouponCode() + " applied successfully", response);
    }
}
