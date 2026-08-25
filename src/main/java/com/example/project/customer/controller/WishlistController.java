package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.service.WishlistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getWishlist(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(wishlistService.getWishlist(uid));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> addToWishlist(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer productId) {
        Integer uid = userId != null ? userId : 101;
        wishlistService.addToWishlist(uid, productId);
        return ApiResponse.okMessage("Product added to wishlist");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeFromWishlist(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer productId) {
        Integer uid = userId != null ? userId : 101;
        wishlistService.removeFromWishlist(uid, productId);
        return ApiResponse.okMessage("Product removed from wishlist");
    }
}
