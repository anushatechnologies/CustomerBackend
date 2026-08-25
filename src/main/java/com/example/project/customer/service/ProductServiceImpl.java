package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final SubcategoryRepository subcategoryRepository;

    public ProductResponse create(ProductRequest request) {
        return response(repository.save(apply(new Product(), request)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        return response(find(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public ProductResponse update(Integer id, ProductRequest request) {
        return response(repository.save(apply(find(id), request)));
    }

    public void delete(Integer id) {
        repository.delete(find(id));
    }

    private Product find(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Product apply(Product item, ProductRequest request) {
        item.setSubcategory(subcategoryRepository.findById(request.subcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.subcategoryId())));
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setStockQty(request.stockQty());
        item.setUnit(request.unit());
        item.setImageUrl(request.imageUrl());
        item.setActive(request.active());
        return item;
    }

    private ProductResponse response(Product p) {
        return new ProductResponse(p.getProductId(), p.getSubcategory().getSubcategoryId(), p.getTitle(),
                p.getDescription(), p.getPrice(), p.getStockQty(), p.getUnit(), p.getImageUrl(),
                p.isActive(), p.getCreatedAt());
    }
}