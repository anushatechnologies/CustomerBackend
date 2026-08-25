package com.example.project.customer.service;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface S3ImageService {

    ImageUploadResponse uploadImage(MultipartFile file, ImageFolder folder);

    ImageUploadResponse uploadImage(MultipartFile file, String folderName);

    byte[] downloadImage(String imageKey);

    void deleteImage(String imageKeyOrUrl);

    String extractKeyFromUrl(String imageKeyOrUrl);

    boolean imageExists(String imageKey);
}
