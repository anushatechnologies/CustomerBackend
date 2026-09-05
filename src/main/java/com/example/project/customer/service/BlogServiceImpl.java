package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BlogArticleResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.entity.BlogArticle;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BlogArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class BlogServiceImpl implements BlogService {

    private final BlogArticleRepository repository;

    @Override
    public ApiResponse<List<BlogArticleResponse>> getArticles(String category, String tag, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 10;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        String cleanCat = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanTag = (tag != null && !tag.isBlank()) ? tag.trim() : null;

        Page<BlogArticle> pageResult = repository.findPublishedArticles(cleanCat, cleanTag, pageable);

        List<BlogArticleResponse> data = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Blog articles retrieved successfully", data, meta);
    }

    @Override
    public BlogArticleResponse getArticleBySlugOrId(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) {
            throw new IllegalArgumentException("Article identifier cannot be blank");
        }

        BlogArticle article;
        try {
            int id = Integer.parseInt(slugOrId.trim());
            article = repository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            article = repository.findBySlugIgnoreCase(slugOrId.trim()).orElse(null);
        }

        if (article == null) {
            // Try by slug as fallback
            article = repository.findBySlugIgnoreCase(slugOrId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Blog article not found: " + slugOrId));
        }

        return mapToResponse(article);
    }

    private BlogArticleResponse mapToResponse(BlogArticle a) {
        return BlogArticleResponse.builder()
                .articleId(a.getArticleId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .excerpt(a.getExcerpt())
                .content(a.getContent())
                .author(a.getAuthor())
                .category(a.getCategory())
                .tags(a.getTags())
                .readTimeMinutes(a.getReadTimeMinutes())
                .imageUrl(a.getImageUrl())
                .published(a.getPublished())
                .publishedAt(a.getPublishedAt())
                .build();
    }
}
