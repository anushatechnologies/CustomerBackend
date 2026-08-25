package com.example.project.customer.common;

public class PaginationMeta {
    private int page;
    private int limit;
    private long totalCount;
    private int totalPages;
    private boolean hasNextPage;
    private boolean hasPrevPage;

    public PaginationMeta() {
    }

    public PaginationMeta(int page, int limit, long totalCount, int totalPages, boolean hasNextPage, boolean hasPrevPage) {
        this.page = page;
        this.limit = limit;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.hasNextPage = hasNextPage;
        this.hasPrevPage = hasPrevPage;
    }

    public static PaginationMeta of(int page, int limit, long totalCount) {
        int totalPages = limit > 0 ? (int) Math.ceil((double) totalCount / limit) : 1;
        boolean hasNextPage = page < totalPages;
        boolean hasPrevPage = page > 1;
        return new PaginationMeta(page, limit, totalCount, totalPages, hasNextPage, hasPrevPage);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public boolean isHasPrevPage() {
        return hasPrevPage;
    }

    public void setHasPrevPage(boolean hasPrevPage) {
        this.hasPrevPage = hasPrevPage;
    }
}
