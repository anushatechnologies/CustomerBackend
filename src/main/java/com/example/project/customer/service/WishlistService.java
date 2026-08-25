package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;

import java.util.List;

public interface WishlistService {
    List<ProductResponse> getWishlist(Integer userId);
    void addToWishlist(Integer userId, Integer productId);
    void removeFromWishlist(Integer userId, Integer productId);
}
