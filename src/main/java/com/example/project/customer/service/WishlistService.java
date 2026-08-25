package com.example.project.customer.service;

import com.example.project.customer.dto.WishlistResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistResponse> getWishlist(Integer userId);
    WishlistResponse addToWishlist(Integer userId, Integer productId);
    void removeFromWishlist(Integer userId, Integer productId);
}
