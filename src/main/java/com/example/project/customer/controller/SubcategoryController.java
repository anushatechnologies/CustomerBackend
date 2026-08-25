package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.service.SubcategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<SubcategoryResponse>> create(@Valid @RequestBody SubcategoryRequest request) {
        SubcategoryResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Subcategory created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategory retrieved successfully", service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubcategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Subcategories retrieved successfully", service.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> update(@PathVariable Integer id, @Valid @RequestBody SubcategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategory updated successfully", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Subcategory deleted successfully", null));
    }
}