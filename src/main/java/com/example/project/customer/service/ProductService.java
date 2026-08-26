package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import java.util.List;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse getById(Integer id);
    List<ProductResponse> getAll();
    ProductResponse update(Integer id, ProductRequest request);
    void delete(Integer id);
    ProductResponse activate(Integer id);
    ProductResponse deactivate(Integer id);
    ProductListResponse getPending();
    ProductListResponse getAdminAll();
    ProductResponse getAdminById(Integer id);
    ProductResponse approve(Integer id);
    ProductResponse reject(Integer id, ProductRejectionRequest request);
}