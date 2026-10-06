package com.example.project.customer.validation;

import com.example.project.customer.exception.InvalidImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUrlValidatorTest {

    @Test
    @DisplayName("validateForSave - should return null when input is null")
    void validateForSave_Null_ReturnsNull() {
        assertThat(ImageUrlValidator.validateForSave(null)).isNull();
    }

    @Test
    @DisplayName("validateForSave - should return null when input is empty or whitespace")
    void validateForSave_Blank_ReturnsNull() {
        assertThat(ImageUrlValidator.validateForSave("")).isNull();
        assertThat(ImageUrlValidator.validateForSave("   ")).isNull();
    }

    @Test
    @DisplayName("validateForSave - should accept valid full S3 HTTPS URL")
    void validateForSave_ValidS3Url_ReturnsUrl() {
        String url = "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/categories/uuid-123.jpg";
        assertThat(ImageUrlValidator.validateForSave(url)).isEqualTo(url);
    }

    @Test
    @DisplayName("validateForSave - should accept valid CDN image URL")
    void validateForSave_ValidCdnUrl_ReturnsUrl() {
        String url = "https://cdn.hinchmart.com/categories/civil_structural.jpg";
        assertThat(ImageUrlValidator.validateForSave(url)).isEqualTo(url);
    }

    @Test
    @DisplayName("validateForSave - should accept valid relative S3 key")
    void validateForSave_ValidS3Key_ReturnsKey() {
        String key = "categories/58fca786-fa31-4b60-bb91-e03efe75662c.png";
        assertThat(ImageUrlValidator.validateForSave(key)).isEqualTo(key);
    }

    @Test
    @DisplayName("validateForSave - should reject browser-local blob URL")
    void validateForSave_BlobUrl_ThrowsException() {
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("blob:http://localhost:5173/58fca786-fa31-4b60-bb91-e03efe75662c"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Blob URLs");
    }

    @Test
    @DisplayName("validateForSave - should reject base64 data URL")
    void validateForSave_DataUri_ThrowsException() {
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Base64 data URLs");
    }

    @Test
    @DisplayName("validateForSave - should reject localhost or loopback URL")
    void validateForSave_Localhost_ThrowsException() {
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("http://localhost:5173/test.jpg"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Localhost");

        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("http://127.0.0.1:8080/test.png"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Localhost");
    }

    @Test
    @DisplayName("validateForSave - should reject dangerous schemes")
    void validateForSave_DangerousSchemes_ThrowsException() {
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("javascript:alert(1)"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("scheme is not allowed");

        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("file:///etc/passwd"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("scheme is not allowed");
    }

    @Test
    @DisplayName("validateForSave - should reject unexpected external URL with non-image extension")
    void validateForSave_UnexpectedExternalUrl_ThrowsException() {
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("https://random-site.com/malicious.exe"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Must be an S3 URL or point to an image file");

        assertThatThrownBy(() -> ImageUrlValidator.validateForSave("https://random-site.com/endpoint"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Must be an S3 URL or point to an image file");
    }

    @Test
    @DisplayName("validateForSave - should reject excessively long strings exceeding 2048 chars")
    void validateForSave_ExcessiveLength_ThrowsException() {
        String hugeString = "https://example.com/" + "a".repeat(2050) + ".jpg";
        assertThatThrownBy(() -> ImageUrlValidator.validateForSave(hugeString))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("exceeds maximum allowed length");
    }

    @Test
    @DisplayName("isBlobOrDataUrl - should identify blob, data, and localhost URLs")
    void isBlobOrDataUrl_Checks() {
        assertThat(ImageUrlValidator.isBlobOrDataUrl("blob:http://localhost:5173/uuid")).isTrue();
        assertThat(ImageUrlValidator.isBlobOrDataUrl("data:image/jpeg;base64,/9j/4AAQSkZJRg==")).isTrue();
        assertThat(ImageUrlValidator.isBlobOrDataUrl("http://localhost:8080/img.jpg")).isTrue();
        assertThat(ImageUrlValidator.isBlobOrDataUrl("https://hinchmart-storage.s3.amazonaws.com/cat.jpg")).isFalse();
        assertThat(ImageUrlValidator.isBlobOrDataUrl("categories/cat.jpg")).isFalse();
        assertThat(ImageUrlValidator.isBlobOrDataUrl(null)).isFalse();
        assertThat(ImageUrlValidator.isBlobOrDataUrl("")).isFalse();
    }
}
