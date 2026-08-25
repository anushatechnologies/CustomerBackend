package com.example.project.customer.common;

import java.util.List;

public class PagedResponse<T> {
    private boolean success = true;
    private List<T> data;
    private PaginationMeta pagination;

    public PagedResponse() {
    }

    public PagedResponse(List<T> data, PaginationMeta pagination) {
        this.success = true;
        this.data = data;
        this.pagination = pagination;
    }

    public static <T> PagedResponse<T> of(List<T> data, int page, int limit, long totalCount) {
        return new PagedResponse<>(data, PaginationMeta.of(page, limit, totalCount));
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public PaginationMeta getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMeta pagination) {
        this.pagination = pagination;
    }
}
