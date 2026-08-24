package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import java.util.List;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse getById(Integer id);
    List<ProductResponse> getAll();
    ProductResponse update(Integer id, ProductRequest request);
    void delete(Integer id);
}