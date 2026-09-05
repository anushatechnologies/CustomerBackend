package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BlogArticleResponse;
import com.example.project.customer.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<List<BlogArticleResponse>>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        ApiResponse<List<BlogArticleResponse>> response = blogService.getArticles(category, tag, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/articles/{slugOrId}")
    public ResponseEntity<ApiResponse<BlogArticleResponse>> getArticleBySlugOrId(
            @PathVariable String slugOrId) {
        BlogArticleResponse article = blogService.getArticleBySlugOrId(slugOrId);
        return ResponseEntity.ok(ApiResponse.ok("Blog article retrieved successfully", article));
    }
}
