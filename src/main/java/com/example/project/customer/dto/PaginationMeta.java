package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMeta {
    private int page;
    private int limit;
    private long totalCount;
    private int totalPages;

    @JsonProperty("hasNextPage")
    private boolean hasNextPage;

    @JsonProperty("hasPrevPage")
    private boolean hasPrevPage;

    public static PaginationMeta of(int page, int limit, long totalCount) {
        int totalPages = limit > 0 ? (int) Math.ceil((double) totalCount / limit) : 0;
        return PaginationMeta.builder()
                .page(page)
                .limit(limit)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasNextPage(page < totalPages)
                .hasPrevPage(page > 1)
                .build();
    }
}
