package com.example.project.customer.dto;

public class ImageUploadResponse {

    private String imageKey;
    private String imageUrl;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;

    public ImageUploadResponse() {
    }

    public ImageUploadResponse(String imageKey, String imageUrl, String originalFileName, String contentType, long sizeBytes) {
        this.imageKey = imageKey;
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
