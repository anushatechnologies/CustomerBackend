package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.service.SubcategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    public SubcategoryController(SubcategoryService subcategoryService) {
        this.subcategoryService = subcategoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubcategoryResponse>> create(@Valid @RequestBody SubcategoryRequest request) {
        SubcategoryResponse response = subcategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Subcategory created successfully", response));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubcategoryResponse> getById(@PathVariable Integer id) {
        return ApiResponse.ok(subcategoryService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<SubcategoryResponse>> getAll(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(subcategoryService.getAll(categoryId, active));
    }

    @PutMapping("/{id}")
    public ApiResponse<SubcategoryResponse> update(@PathVariable Integer id, @Valid @RequestBody SubcategoryRequest request) {
        return ApiResponse.ok("Subcategory updated successfully", subcategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        subcategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}