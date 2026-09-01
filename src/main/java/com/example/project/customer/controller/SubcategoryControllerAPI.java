package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
public class SubcategoryControllerAPI {

    /**
     * GET /api/subcategories?categoryId={categoryId}
     * Fetch subcategories for a specific category
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getSubcategoriesByCategory(
            @RequestParam(required = true) Integer categoryId) {
        // TODO: Implement subcategory retrieval logic
        List<?> subcategories = new ArrayList<>();
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Subcategories retrieved successfully")
                .data(subcategories)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
