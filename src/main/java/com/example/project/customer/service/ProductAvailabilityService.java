package com.example.project.customer.service;

import com.example.project.customer.dto.ProductAvailability;
import com.example.project.customer.entity.Product;

import java.util.Collection;
import java.util.Map;

public interface ProductAvailabilityService {

    ProductAvailability getAvailability(Product product);

    ProductAvailability getAvailability(Integer productId);

    Map<Integer, ProductAvailability> getAvailabilityBatch(Collection<Product> products);

    Map<Integer, ProductAvailability> getAvailabilityBatchByIds(Collection<Integer> productIds);
}
