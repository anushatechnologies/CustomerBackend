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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<ApiResponse<List<SubcategoryResponse>>> getAll(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean visibleOnWebsite) {
        List<SubcategoryResponse> list = service.getAll(categoryId, active, visibleOnWebsite);
        return ResponseEntity.ok(ApiResponse.ok("Subcategories retrieved successfully", list));
    }

    @GetMapping("/website")
    public ResponseEntity<ApiResponse<List<SubcategoryResponse>>> getWebsiteSubcategories(
            @RequestParam(required = false) Integer categoryId) {
        List<SubcategoryResponse> list = service.getWebsiteSubcategories(categoryId);
        return ResponseEntity.ok(ApiResponse.ok("Website subcategories retrieved successfully", list));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody SubcategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategory updated successfully", service.update(id, request)));
    }

    @PatchMapping(value = {"/{id}/website-visibility", "/admin/{id}/website-visibility"})
    public ResponseEntity<ApiResponse<SubcategoryResponse>> updateWebsiteVisibility(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        Boolean visible = null;
        if (body != null && body.containsKey("visibleOnWebsite")) {
            Object val = body.get("visibleOnWebsite");
            if (val instanceof Boolean b) {
                visible = b;
            } else if (val != null) {
                visible = Boolean.parseBoolean(val.toString());
            }
        }
        SubcategoryResponse updated = service.updateWebsiteVisibility(id, visible);
        return ResponseEntity.ok(ApiResponse.ok("Subcategory website visibility updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Subcategory deleted successfully", null));
    }
}