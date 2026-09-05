package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.NewsItemResponse;

import java.util.List;

public interface NewsService {
    ApiResponse<List<NewsItemResponse>> getNews(String category, int page, int limit);
}
