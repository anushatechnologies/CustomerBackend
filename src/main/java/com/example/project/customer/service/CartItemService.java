package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;

import java.util.List;

public interface CartItemService {

    CartItemResponse create(CartItemRequest request);

    CartItemResponse getById(Integer id);

    List<CartItemResponse> getAll(Integer customerId);

    List<CartItemResponse> getByCustomerId(Integer customerId);

    CartItemResponse update(Integer id, CartItemRequest request);

    void delete(Integer id);

    void deleteByCustomerId(Integer customerId);
}
