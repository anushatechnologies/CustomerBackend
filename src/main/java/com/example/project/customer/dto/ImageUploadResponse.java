package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageUploadResponse {

    private String imageKey;

    @JsonProperty("fileUrl")
    private String fileUrl;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return fileUrl;
    }

    @JsonProperty("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.fileUrl = imageUrl;
    }

    @JsonProperty("originalFileName")
    public String getOriginalFileName() {
        return fileName;
    }

    @JsonProperty("originalFileName")
    public void setOriginalFileName(String originalFileName) {
        this.fileName = originalFileName;
    }

    @JsonProperty("contentType")
    public String getContentType() {
        return mimeType;
    }

    @JsonProperty("contentType")
    public void setContentType(String contentType) {
        this.mimeType = contentType;
    }

    @JsonProperty("sizeBytes")
    public Long getSizeBytes() {
        return fileSize;
    }

    @JsonProperty("sizeBytes")
    public void setSizeBytes(Long sizeBytes) {
        this.fileSize = sizeBytes;
    }
}
