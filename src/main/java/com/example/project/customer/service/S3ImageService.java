package com.example.project.customer.service;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3ImageService {

    ImageUploadResponse uploadImage(MultipartFile file, ImageFolder folder);

    ImageUploadResponse uploadImage(MultipartFile file, String folderName);

    List<ImageUploadResponse> uploadImages(List<MultipartFile> files, ImageFolder folder);

    List<ImageUploadResponse> uploadImages(List<MultipartFile> files, String folderName);

    ImageUploadResponse uploadFile(MultipartFile file, ImageFolder folder);

    ImageUploadResponse uploadFile(MultipartFile file, String folderName);

    byte[] downloadImage(String imageKey);

    void deleteImage(String imageKeyOrUrl);

    void deleteImages(List<String> imageKeysOrUrls);

    String extractKeyFromUrl(String imageKeyOrUrl);

    boolean imageExists(String imageKey);
}
