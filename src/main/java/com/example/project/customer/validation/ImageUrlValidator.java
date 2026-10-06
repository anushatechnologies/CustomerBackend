package com.example.project.customer.validation;

import com.example.project.customer.exception.InvalidImageException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ImageUrlValidator {

    private static final int MAX_URL_LENGTH = 2048;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".svg", ".gif"
    );

    private ImageUrlValidator() {
        // utility class
    }

    /**
     * Validates and normalizes an image URL for saving into the database.
     *
     * @param imageUrl the input image URL or S3 key
     * @return normalized URL or null if empty
     * @throws InvalidImageException if the image URL is invalid, blob, data URL, localhost, or unsupported
     */
    public static String validateForSave(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String trimmed = imageUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new InvalidImageException("Image URL exceeds maximum allowed length of " + MAX_URL_LENGTH + " characters");
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.startsWith("blob:") || lower.contains("blob:http")) {
            throw new InvalidImageException("Blob URLs (e.g., 'blob:http...') are temporary browser-local references and cannot be saved as permanent image URLs. Upload the image file to S3 first.");
        }

        if (lower.startsWith("data:")) {
            throw new InvalidImageException("Base64 data URLs cannot be saved directly into the database. Upload the image file to obtain a permanent S3 URL.");
        }

        if (lower.startsWith("http://localhost") || lower.startsWith("https://localhost")
                || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://127.0.0.1")) {
            throw new InvalidImageException("Localhost and loopback URLs cannot be saved as image URLs. Upload the file to S3 first.");
        }

        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("ftp:")) {
            throw new InvalidImageException("Image URL scheme is not allowed: " + trimmed);
        }

        // Web HTTP/HTTPS URL
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String host = uri.getHost();
                if (host == null || host.isBlank()) {
                    throw new InvalidImageException("Invalid image URL host: " + trimmed);
                }

                String path = uri.getPath();
                if (path == null || path.isBlank() || path.equals("/")) {
                    throw new InvalidImageException("Image URL path cannot be empty: " + trimmed);
                }

                // If S3 or CDN or standard image extension
                boolean hasExtension = hasImageExtension(path);
                boolean isS3OrCdn = host.contains("amazonaws.com") || host.contains("hinchmart.com") || path.contains("/uploads/");

                if (!hasExtension && !isS3OrCdn) {
                    throw new InvalidImageException("Invalid or unexpected external image URL. Must be an S3 URL or point to an image file (.jpg, .jpeg, .png, .webp, .svg, .gif): " + trimmed);
                }

                return trimmed;
            } catch (IllegalArgumentException e) {
                throw new InvalidImageException("Malformed image URL: " + trimmed);
            }
        }

        // Relative S3 key (e.g. "categories/uuid.jpg", "subcategories/uuid.png")
        if (isValidS3Key(trimmed)) {
            return trimmed;
        }

        throw new InvalidImageException("Invalid image URL format: must be an HTTPS URL or valid S3 object key, got: " + trimmed);
    }

    /**
     * Determines whether a given URL is a browser-local blob or data URL.
     */
    public static boolean isBlobOrDataUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("blob:") || lower.contains("blob:http") || lower.startsWith("data:")
                || lower.startsWith("http://localhost") || lower.startsWith("https://localhost")
                || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://127.0.0.1");
    }

    /**
     * Checks if the given relative key represents a valid S3 key with image extension.
     */
    public static boolean isValidS3Key(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String lower = key.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("blob:") || lower.startsWith("data:") || lower.contains("://") || lower.startsWith("/")) {
            return false;
        }
        if (!key.matches("^[a-zA-Z0-9_\\-]+(/[a-zA-Z0-9_\\.\\-]+)+$")) {
            return false;
        }
        return hasImageExtension(key);
    }

    private static boolean hasImageExtension(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        int queryIdx = lower.indexOf('?');
        if (queryIdx != -1) {
            lower = lower.substring(0, queryIdx);
        }
        for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
