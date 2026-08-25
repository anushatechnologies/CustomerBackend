package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.SearchSuggestionsResponse;
import com.example.project.customer.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ProductService productService;

    public SearchController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/suggestions")
    public ApiResponse<SearchSuggestionsResponse> getSuggestions(@RequestParam(required = false, defaultValue = "") String q) {
        return ApiResponse.ok(productService.getSearchSuggestions(q));
    }
}
