package com.example.project.customer.controller;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.service.S3ImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    private final S3ImageService s3ImageService;

    public ImageUploadController(S3ImageService s3ImageService) {
        this.s3ImageService = s3ImageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "other") String folder) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadProductImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.PRODUCTS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.CATEGORIES);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.BANNERS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    public ResponseEntity<Void> deleteImage(@RequestParam("key") String key) {
        s3ImageService.deleteImage(key);
        return ResponseEntity.noContent().build();
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
