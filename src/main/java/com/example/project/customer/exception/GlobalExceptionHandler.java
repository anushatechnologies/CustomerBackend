package com.example.project.customer.exception;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;

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
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorDetail> errors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(new ErrorDetail(error.getField(), error.getDefaultMessage()))
        );
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid request parameter", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Bad argument: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException exception) {
        log.warn("Invalid state: {}", exception.getMessage());
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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException exception) {
        log.debug("Static resource not found: {}", exception.getMessage());
        return response(HttpStatus.NOT_FOUND, "Resource not found: " + exception.getResourcePath());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException exception) {
        log.warn("Method not supported: {}", exception.getMessage());
        return response(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception exception) {
        log.error("Unhandled exception occurred: {}", exception.getMessage(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred: " + exception.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
    }
}
