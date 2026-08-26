package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved successfully", service.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully", null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResponse>> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product activated successfully", service.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Product deactivated successfully", service.deactivate(id)));
    }
}