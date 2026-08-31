package com.example.project.customer.service;

import com.example.project.customer.dto.BrandRequest;
import com.example.project.customer.dto.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse create(BrandRequest request);

    BrandResponse getById(Integer id);

    List<BrandResponse> getAll(Integer categoryId, Integer subcategoryId, Boolean active);

    BrandResponse update(Integer id, BrandRequest request);

    void delete(Integer id);
}
