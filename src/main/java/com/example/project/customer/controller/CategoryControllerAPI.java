package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryControllerAPI {

    /**
     * GET /api/categories
     * Fetch all active master product categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllCategories() {
        // TODO: Implement category retrieval logic
        List<Map<String, Object>> categories = new ArrayList<>();
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Categories retrieved successfully")
                .data(categories)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
