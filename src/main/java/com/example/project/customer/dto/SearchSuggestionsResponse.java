package com.example.project.customer.dto;

import java.util.List;

public class SearchSuggestionsResponse {

    public static class ProductSuggestion {
        private Integer productId;
        private String title;
        private String category;

        public ProductSuggestion() {
        }

        public ProductSuggestion(Integer productId, String title, String category) {
            this.productId = productId;
            this.title = title;
            this.category = category;
        }

        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    public static class CategorySuggestion {
        private Integer categoryId;
        private String name;

        public CategorySuggestion() {
        }

        public CategorySuggestion(Integer categoryId, String name) {
            this.categoryId = categoryId;
            this.name = name;
        }

        public Integer getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private List<ProductSuggestion> products;
    private List<CategorySuggestion> categories;
    private List<String> popularSearches;

    public SearchSuggestionsResponse() {
    }

    public SearchSuggestionsResponse(List<ProductSuggestion> products, List<CategorySuggestion> categories, List<String> popularSearches) {
        this.products = products;
        this.categories = categories;
        this.popularSearches = popularSearches;
    }

    public List<ProductSuggestion> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSuggestion> products) {
        this.products = products;
    }

    public List<CategorySuggestion> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySuggestion> categories) {
        this.categories = categories;
    }

    public List<String> getPopularSearches() {
        return popularSearches;
    }

    public void setPopularSearches(List<String> popularSearches) {
        this.popularSearches = popularSearches;
    }
}
