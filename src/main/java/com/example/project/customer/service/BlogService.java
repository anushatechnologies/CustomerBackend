package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BlogArticleResponse;

import java.util.List;

public interface BlogService {
    ApiResponse<List<BlogArticleResponse>> getArticles(String category, String tag, int page, int limit);
    BlogArticleResponse getArticleBySlugOrId(String slugOrId);
}
