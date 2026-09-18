package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.InvalidImageException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ImageUploadController {

    private final S3ImageService s3ImageService;
    private final ProductRepository productRepository;
    private final SellerContextUtil sellerContextUtil;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "other") String folder) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Image uploaded successfully", response));
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.PRODUCTS);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product image uploaded successfully", response));
    }

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.CATEGORIES);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Category image uploaded successfully", response));
    }

    @PostMapping(value = "/subcategories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadSubcategoryImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.SUBCATEGORIES);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Subcategory image uploaded successfully", response));
    }

    @PostMapping(value = "/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.BANNERS);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Banner image uploaded successfully", response));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadDocument(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadFile(file, ImageFolder.DOCUMENTS);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document uploaded successfully", response));
    }

    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<java.util.List<ImageUploadResponse>>> uploadMultipleImages(
            @RequestParam("files") java.util.List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        java.util.List<ImageUploadResponse> responses = s3ImageService.uploadImages(files, folder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Images uploaded successfully", responses));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadImage(@RequestParam("key") String key) {
        byte[] data = s3ImageService.downloadImage(key);
        MediaType mediaType = determineMediaType(key);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + s3ImageService.extractKeyFromUrl(key) + "\"")
                .body(data);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("key") String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidImageException("Image key must not be blank");
        }
        validateImageDeletionAccess(key);
        s3ImageService.deleteImage(key);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully", null));
    }

    private void validateImageDeletionAccess(String key) {
        if (SecurityUtils.isAdmin()) {
            return;
        }

        if (SecurityUtils.isSeller()) {
            String cleanKey = s3ImageService.extractKeyFromUrl(key);
            String lowerKey = cleanKey.toLowerCase();

            // Prevent sellers from deleting platform or system-wide assets
            if (lowerKey.startsWith("banners/") || lowerKey.startsWith("categories/")
                    || lowerKey.startsWith("subcategories/") || lowerKey.startsWith("platform/")) {
                throw new ForbiddenException("Access denied: Sellers are not permitted to delete platform or catalog assets.");
            }

            // Verify product ownership if this image is associated with any product
            if (productRepository != null) {
                List<Product> products = productRepository.findByImageUrlContainingKey(cleanKey);
                if (products != null && !products.isEmpty()) {
                    Integer sellerId = (sellerContextUtil != null) ? sellerContextUtil.getCurrentSellerId() : null;
                    if (sellerId == null) {
                        throw new ForbiddenException("Access denied: Could not resolve seller identity for image ownership verification.");
                    }
                    boolean unauthorized = products.stream().anyMatch(product ->
                            product.getStore() == null
                                    || product.getStore().getSeller() == null
                                    || !sellerId.equals(product.getStore().getSeller().getSellerId())
                    );
                    if (unauthorized) {
                        throw new ForbiddenException("Access denied: You do not own the product associated with this image.");
                    }
                }
            }
            return;
        }

        throw new ForbiddenException("Access denied: Only administrators and sellers can delete images.");
    }

    private MediaType determineMediaType(String key) {
        if (key == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
