package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionResponse {

    @Builder.Default
    private List<ProductSuggestion> products = new ArrayList<>();

    @Builder.Default
    private List<CategorySuggestion> categories = new ArrayList<>();

    @Builder.Default
    private List<String> popularSearches = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestion {
        private Integer productId;
        private String title;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySuggestion {
        private Integer categoryId;
        private String name;
    }
}
