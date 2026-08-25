package com.example.project.customer.exception;

import com.example.project.customer.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(CustomerNotFoundException exception) {
        log.warn("Customer not found: {}", exception.getMessage());
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BannerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBannerNotFound(BannerNotFoundException exception) {
        log.warn("Banner not found: {}", exception.getMessage());
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CustomerConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(CustomerConflictException exception) {
        log.warn("Customer conflict: {}", exception.getMessage());
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceConflict(ResourceConflictException exception) {
        log.warn("Resource conflict: {}", exception.getMessage());
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Validation failed", errors));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException exception) {
        log.warn("Unauthorized request: {}", exception.getMessage());
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("Invalid argument: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidImage(InvalidImageException exception) {
        log.warn("Invalid image: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleImageStorage(ImageStorageException exception) {
        log.error("Image storage error: {}", exception.getMessage(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Image storage error: " + exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeExceeded(MaxUploadSizeExceededException exception) {
        log.warn("Upload size exceeded: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, "File upload size exceeded maximum limit of 10MB");
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
    }
}
