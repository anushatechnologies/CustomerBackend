package com.example.project.customer.service;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.ResourceConflictException;
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
        Product product = apply(new Product(), request);
        product.setActive(false);
        product.setApprovalStatus(ApprovalStatus.PENDING);
        product.setRejectionReason(null);
        return response(repository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        return response(repository.findByProductIdAndApprovalStatusAndActive(id, ApprovalStatus.APPROVED, true)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return repository.findByApprovalStatusAndActive(ApprovalStatus.APPROVED, true).stream().map(this::response).toList();
    }

    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = find(id);
        boolean requestedActive = Boolean.TRUE.equals(request.active());
        apply(product, request);
        product.setActive(requestedActive && product.getApprovalStatus() == ApprovalStatus.APPROVED);
        return response(repository.save(product));
    }

    public void delete(Integer id) {
        repository.delete(find(id));
    }

    public ProductResponse activate(Integer id) {
        Product product = find(id);
        if (product.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResourceConflictException("Product cannot be activated because it is not approved.");
        }
        product.setActive(true);
        return response(repository.save(product));
    }

    public ProductResponse deactivate(Integer id) {
        Product product = find(id);
        product.setActive(false);
        return response(repository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductListResponse getPending() {
        List<ProductResponse> products = repository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
                .map(this::response).toList();
        return new ProductListResponse(products, products.size());
    }

    @Transactional(readOnly = true)
    public ProductListResponse getAdminAll() {
        List<ProductResponse> products = repository.findAll().stream().map(this::response).toList();
        return new ProductListResponse(products, products.size());
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminById(Integer id) {
        return response(find(id));
    }

    public ProductResponse approve(Integer id) {
        Product product = find(id);
        product.setApprovalStatus(ApprovalStatus.APPROVED);
        product.setRejectionReason(null);
        return response(repository.save(product));
    }

    public ProductResponse reject(Integer id, ProductRejectionRequest request) {
        Product product = find(id);
        product.setApprovalStatus(ApprovalStatus.REJECTED);
        product.setActive(false);
        product.setRejectionReason(request.reason());
        return response(repository.save(product));
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
        return item;
    }

    private ProductResponse response(Product p) {
        ApprovalStatus approvalStatus = p.getApprovalStatus() == null ? ApprovalStatus.PENDING : p.getApprovalStatus();
        String status = approvalStatus == ApprovalStatus.REJECTED ? "REJECTED" :
            approvalStatus == ApprovalStatus.PENDING ? "PENDING" :
                (p.isActive() ? "APPROVED" : "INACTIVE");
        return new ProductResponse(p.getProductId(), p.getSubcategory().getSubcategoryId(), p.getTitle(),
                p.getDescription(), p.getPrice(), p.getStockQty(), p.getUnit(), p.getImageUrl(),
            p.isActive(), approvalStatus.name(), status, p.getRejectionReason(), p.getCreatedAt());
    }
}