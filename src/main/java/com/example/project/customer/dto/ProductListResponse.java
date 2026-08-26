package com.example.project.customer.dto;

import java.util.List;

public record ProductListResponse(List<ProductResponse> products, int total) {
}