package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.service.CategoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;
    private final com.example.project.customer.service.SpecificationService specificationService;

    @org.springframework.beans.factory.annotation.Autowired
    public CategoryController(CategoryService service, com.example.project.customer.service.SpecificationService specificationService) {
        this.service = service;
        this.specificationService = specificationService;
    }

    public CategoryController(CategoryService service) {
        this(service, null);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Category created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Category retrieved successfully", service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "false") Boolean includeSubcategories,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        ApiResponse<List<CategoryResponse>> response = service.getAll(active, includeSubcategories, page, limit);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully", null));
    }

    // =========================================================
    // CATEGORY SPECIFICATION MAPPINGS
    // =========================================================

    @GetMapping("/{categoryId}/specifications")
    public ResponseEntity<ApiResponse<List<com.example.project.customer.dto.CategorySpecificationResponse>>> getCategorySpecifications(
            @PathVariable Integer categoryId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        List<com.example.project.customer.dto.CategorySpecificationResponse> specs =
                specificationService.getCategorySpecifications(categoryId, activeOnly);
        return ResponseEntity.ok(ApiResponse.ok("Category specifications retrieved successfully", specs));
    }

    @PostMapping("/{categoryId}/specifications")
    public ResponseEntity<ApiResponse<com.example.project.customer.dto.CategorySpecificationResponse>> addCategorySpecification(
            @PathVariable Integer categoryId,
            @Valid @RequestBody com.example.project.customer.dto.CategorySpecificationRequest request) {
        com.example.project.customer.dto.CategorySpecificationResponse created =
                specificationService.addCategorySpecification(categoryId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Specification assigned to category successfully", created));
    }

    @PutMapping("/{categoryId}/specifications/{specificationId}")
    public ResponseEntity<ApiResponse<com.example.project.customer.dto.CategorySpecificationResponse>> updateCategorySpecification(
            @PathVariable Integer categoryId,
            @PathVariable Integer specificationId,
            @Valid @RequestBody com.example.project.customer.dto.CategorySpecificationUpdateRequest request) {
        com.example.project.customer.dto.CategorySpecificationResponse updated =
                specificationService.updateCategorySpecification(categoryId, specificationId, request);
        return ResponseEntity.ok(ApiResponse.ok("Category specification updated successfully", updated));
    }

    @DeleteMapping("/{categoryId}/specifications/{specificationId}")
    public ResponseEntity<ApiResponse<Void>> removeCategorySpecification(
            @PathVariable Integer categoryId,
            @PathVariable Integer specificationId) {
        specificationService.removeCategorySpecification(categoryId, specificationId);
        return ResponseEntity.ok(ApiResponse.ok("Specification removed from category successfully", null));
    }

    @PutMapping("/{categoryId}/specifications")
    public ResponseEntity<ApiResponse<List<com.example.project.customer.dto.CategorySpecificationResponse>>> batchConfigureCategorySpecifications(
            @PathVariable Integer categoryId,
            @RequestBody List<com.example.project.customer.dto.CategorySpecificationRequest> requests) {
        List<com.example.project.customer.dto.CategorySpecificationResponse> updated =
                specificationService.batchConfigureCategorySpecifications(categoryId, requests);
        return ResponseEntity.ok(ApiResponse.ok("Category specifications configured successfully", updated));
    }
}