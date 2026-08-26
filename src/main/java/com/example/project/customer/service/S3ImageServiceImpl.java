package com.example.project.customer.service;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.exception.ImageStorageException;
import com.example.project.customer.exception.InvalidImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class S3ImageServiceImpl implements S3ImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public S3ImageServiceImpl(
            S3Client s3Client,
            @Value("${aws.s3.bucket-name:hinchmart-storage-191481838776-ap-south-2-an}") String bucketName,
            @Value("${aws.region:ap-south-2}") String region) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
    }

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, ImageFolder folder) {
        ImageFolder targetFolder = folder != null ? folder : ImageFolder.OTHER;
        return uploadImage(file, targetFolder.getFolderName());
    }

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, String folderName) {
        validateImageFile(file);

        String cleanFolder = sanitizeFolderName(folderName);
        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        String s3Key = cleanFolder + "/" + uniqueFileName;

        String imageUrl;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            imageUrl = buildImageUrl(s3Key);
            log.info("Successfully uploaded image to S3: bucket={}, key={}", bucketName, s3Key);
        } catch (SdkException e) {
            log.error("S3 upload failed for bucket={}, key={}", bucketName, s3Key, e);
            throw new ImageStorageException("Failed to upload image to S3 storage", e);
        } catch (IOException e) {
            log.error("Failed to read image stream for key: {}", s3Key, e);
            throw new ImageStorageException("Failed to read image content for upload", e);
        }

        return ImageUploadResponse.builder()
                .imageKey(s3Key)
                .fileUrl(imageUrl)
                .fileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    @Override
    public byte[] downloadImage(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            throw new InvalidImageException("Image key must not be blank");
        }

        String key = extractKeyFromUrl(imageKey);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("Image not found in S3 for key: {}", key);
            throw new InvalidImageException("Image not found with key: " + key);
        } catch (SdkException e) {
            log.error("AWS S3 download error for key: {}", key, e);
            throw new ImageStorageException("Failed to download image from S3 storage", e);
        }
    }

    @Override
    public void deleteImage(String imageKeyOrUrl) {
        if (imageKeyOrUrl == null || imageKeyOrUrl.isBlank()) {
            return;
        }

        String key = extractKeyFromUrl(imageKeyOrUrl);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted image from S3: bucket={}, key={}", bucketName, key);
        } catch (SdkException e) {
            log.warn("AWS S3 delete error for key: {}", key, e);
        }
    }

    @Override
    public boolean imageExists(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return false;
        }
        String key = extractKeyFromUrl(imageKey);
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            log.error("Error checking S3 image existence for key: {}", key, e);
            return false;
        }
    }

    @Override
    public String extractKeyFromUrl(String imageKeyOrUrl) {
        if (imageKeyOrUrl == null) {
            return "";
        }
        String trimmed = imageKeyOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            int domainIndex = trimmed.indexOf(".amazonaws.com/");
            if (domainIndex != -1) {
                return trimmed.substring(domainIndex + ".amazonaws.com/".length());
            }
            int cdnIndex = trimmed.indexOf("/uploads/");
            if (cdnIndex != -1) {
                return trimmed.substring(cdnIndex + "/uploads/".length());
            }
            int slashIndex = trimmed.lastIndexOf('/');
            if (slashIndex != -1) {
                return trimmed.substring(slashIndex + 1);
            }
        }
        return trimmed;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException("File size exceeds the 15MB limit");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Non-standard content type: {}", contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidImageException("File original filename is missing");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }

    private String sanitizeFolderName(String folderName) {
        if (folderName == null || folderName.isBlank()) {
            return ImageFolder.OTHER.getFolderName();
        }
        String cleaned = folderName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isEmpty() ? ImageFolder.OTHER.getFolderName() : cleaned;
    }

    private String buildImageUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
}
