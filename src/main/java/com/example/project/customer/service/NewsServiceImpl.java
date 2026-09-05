package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.NewsItemResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.entity.NewsItem;
import com.example.project.customer.repository.NewsItemRepository;
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
public class NewsServiceImpl implements NewsService {

    private final NewsItemRepository repository;

    @Override
    public ApiResponse<List<NewsItemResponse>> getNews(String category, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<NewsItem> pageResult = repository.findAllByOrderByPublishedAtDesc(pageable);

        List<NewsItemResponse> data = pageResult.getContent().stream()
                .filter(n -> category == null || category.isBlank() || n.getCategory().equalsIgnoreCase(category.trim()))
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Industry news and material rates retrieved successfully", data, meta);
    }

    private NewsItemResponse mapToResponse(NewsItem n) {
        return NewsItemResponse.builder()
                .newsId(n.getNewsId())
                .title(n.getTitle())
                .summary(n.getSummary())
                .content(n.getContent())
                .category(n.getCategory())
                .source(n.getSource())
                .sourceUrl(n.getSourceUrl())
                .imageUrl(n.getImageUrl())
                .priceChangePercentage(n.getPriceChangePercentage())
                .trendDirection(n.getTrendDirection())
                .publishedAt(n.getPublishedAt())
                .build();
    }
}
