package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.WishlistResponse;
import com.example.project.customer.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist() {
        Integer userId = userContextUtil.getCurrentUserId();
        List<WishlistResponse> wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(ApiResponse.ok("Wishlist retrieved successfully", wishlist));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(@PathVariable Integer productId) {
        Integer userId = userContextUtil.getCurrentUserId();
        WishlistResponse item = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product added to wishlist successfully", item));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Integer productId) {
        Integer userId = userContextUtil.getCurrentUserId();
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed from wishlist successfully", null));
    }
}
