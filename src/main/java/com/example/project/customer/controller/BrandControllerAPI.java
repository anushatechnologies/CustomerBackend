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
@RequestMapping("/api/brands")
public class BrandControllerAPI {

    /**
     * GET /api/brands?subcategoryId={subcategoryId}
     * Fetch brands tagged for a specific subcategory
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getBrandsBySubcategory(
            @RequestParam(required = true) Integer subcategoryId) {
        // TODO: Implement brand retrieval logic
        List<?> brands = new ArrayList<>();
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Brands retrieved successfully")
                .data(brands)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
