package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemUpdateRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CartSyncRequest;
import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.dto.SwitchStoreRequest;
import com.example.project.customer.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/cart", "/api/customer/cart"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addOrUpdateItem(@Valid @RequestBody CartItemRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse updated = cartService.addItem(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart updated successfully", updated));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Integer id,
            @Valid @RequestBody CartItemUpdateRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse updated = cartService.updateItem(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated successfully", updated));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<CartResponse>> syncCart(@Valid @RequestBody CartSyncRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse synced = cartService.syncCart(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart synchronized successfully", synced));
    }

    @PostMapping(value = {"/switch-store", "/switch"})
    public ResponseEntity<ApiResponse<CartResponse>> switchStore(@RequestBody SwitchStoreRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse updated = cartService.switchStore(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Switched active store cart successfully", updated));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Integer id) {
        Integer userId = userContextUtil.getCurrentUserId();
        CartResponse updated = cartService.removeItem(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart successfully", updated));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Integer userId = userContextUtil.getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared successfully", null));
    }

    @PostMapping("/coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> applyCoupon(@Valid @RequestBody CouponRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        CouponResponse couponResponse = cartService.applyCoupon(userId, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Coupon " + request.getCode() + " applied successfully", couponResponse));
    }

    @DeleteMapping("/coupon")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon() {
        Integer userId = userContextUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Coupon removed successfully", cartService.removeCoupon(userId)));
    }
}
