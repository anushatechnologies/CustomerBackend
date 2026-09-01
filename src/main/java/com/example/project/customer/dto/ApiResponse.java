package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private Integer statusCode;
    private String message;
    private T data;
    private PaginationMeta pagination;
    private List<ErrorDetail> errors;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, Integer statusCode, String message, T data) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.statusCode = 200;
        r.message = message;
        r.data = data;
        r.timestamp = LocalDateTime.now();
        
        return r;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> paginated(T data, PaginationMeta pagination) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        r.pagination = pagination;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> paginated(String message, T data, PaginationMeta pagination) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.statusCode = 200;
        r.message = message;
        r.data = data;
        r.pagination = pagination;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.statusCode = 201;
        r.message = message;
        r.data = data;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> created(T data) {
        return created("Resource created successfully", data);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.statusCode = statusCode;
        r.message = message;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> error(int statusCode, String message, List<ErrorDetail> errors) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.statusCode = statusCode;
        r.message = message;
        r.errors = errors;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    // Manual builder to avoid Lombok @Builder + manual constructor conflicts on generic class
    public static class ApiResponseBuilder<T> {
        private boolean success;
        private Integer statusCode;
        private String message;
        private T data;
        private PaginationMeta pagination;
        private List<ErrorDetail> errors;
        private LocalDateTime timestamp;

        public ApiResponseBuilder<T> success(boolean success) { this.success = success; return this; }
        public ApiResponseBuilder<T> statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public ApiResponseBuilder<T> message(String message) { this.message = message; return this; }
        public ApiResponseBuilder<T> data(T data) { this.data = data; return this; }
        public ApiResponseBuilder<T> pagination(PaginationMeta pagination) { this.pagination = pagination; return this; }
        public ApiResponseBuilder<T> errors(List<ErrorDetail> errors) { this.errors = errors; return this; }
        public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ApiResponse<T> build() {
            ApiResponse<T> r = new ApiResponse<>();
            r.success = this.success;
            r.statusCode = this.statusCode;
            r.message = this.message;
            r.data = this.data;
            r.pagination = this.pagination;
            r.errors = this.errors;
            r.timestamp = this.timestamp != null ? this.timestamp : LocalDateTime.now();
            return r;
        }
    }
}
