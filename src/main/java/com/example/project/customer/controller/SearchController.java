package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SearchSuggestionResponse;
import com.example.project.customer.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductService productService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> getSuggestions(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        SearchSuggestionResponse suggestions = productService.getSearchSuggestions(query);
        return ResponseEntity.ok(ApiResponse.ok("Search suggestions retrieved successfully", suggestions));
    }
}
