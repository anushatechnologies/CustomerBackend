package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.exception.InvalidImageException;
import com.example.project.customer.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping({"/api/upload", "/api/documents/upload"})
@RequiredArgsConstructor
public class FileUploadController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf"
    );

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    private final S3ImageService s3ImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.example.project.customer.dto.FileUploadResultResponse> uploadDirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "kyc") String folder) {
        validateUploadFile(file);
        ImageUploadResponse response = s3ImageService.uploadImage(file, folder);
        com.example.project.customer.dto.FileUploadResultResponse result = com.example.project.customer.dto.FileUploadResultResponse.builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("File uploaded successfully")
                .url(response.getFileUrl())
                .fileUrl(response.getFileUrl())
                .fileName(response.getFileName())
                .fileSize(formatFileSize(file.getSize()))
                .sizeBytes(file.getSize())
                .data(response)
                .build();
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        validateUploadFile(file);
        ImageUploadResponse response = s3ImageService.uploadImage(file, folder);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("File uploaded successfully", response));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        validateUploadFile(file);
        ImageUploadResponse response = s3ImageService.uploadImage(file, folder);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("File uploaded successfully", response));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException("File size exceeds the 15MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidImageException("File original filename is missing");
        }

        String lowerName = originalFilename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new InvalidImageException("Invalid or unsupported file extension. Allowed extensions are: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException("Invalid or unsupported content type: " + contentType + ". Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
    }
}
