package com.example.project.customer.controller;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.InvalidImageException;
import com.example.project.customer.service.S3ImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageUploadController.class)
@Import(GlobalExceptionHandler.class)
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3ImageService s3ImageService;

    @Test
    @DisplayName("POST /api/images/upload - Should upload image to specified folder and return 201")
    void uploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shoe.jpg",
                "image/jpeg",
                "image bytes".getBytes()
        );

        ImageUploadResponse response = new ImageUploadResponse(
                "products/shoe-123.jpg",
                "https://bucket.s3.ap-south-2.amazonaws.com/products/shoe-123.jpg",
                "shoe.jpg",
                "image/jpeg",
                11
        );

        when(s3ImageService.uploadImage(any(), eq("products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageKey").value("products/shoe-123.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("https://bucket.s3.ap-south-2.amazonaws.com/products/shoe-123.jpg"))
                .andExpect(jsonPath("$.originalFileName").value("shoe.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.sizeBytes").value(11));
    }

    @Test
    @DisplayName("POST /api/images/products - Should upload image to products folder")
    void uploadProductImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.png",
                "image/png",
                "png bytes".getBytes()
        );

        ImageUploadResponse response = new ImageUploadResponse(
                "products/uuid.png",
                "https://bucket.s3.ap-south-2.amazonaws.com/products/uuid.png",
                "product.png",
                "image/png",
                9
        );

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.PRODUCTS))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/products").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageKey").value("products/uuid.png"));
    }

    @Test
    @DisplayName("POST /api/images/categories - Should upload image to categories folder")
    void uploadCategoryImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.webp",
                "image/webp",
                "webp bytes".getBytes()
        );

        ImageUploadResponse response = new ImageUploadResponse(
                "categories/uuid.webp",
                "https://bucket.s3.ap-south-2.amazonaws.com/categories/uuid.webp",
                "cat.webp",
                "image/webp",
                10
        );

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.CATEGORIES))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/categories").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageKey").value("categories/uuid.webp"));
    }

    @Test
    @DisplayName("POST /api/images/banners - Should upload image to banners folder")
    void uploadBannerImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "banner bytes".getBytes()
        );

        ImageUploadResponse response = new ImageUploadResponse(
                "banners/uuid.jpg",
                "https://bucket.s3.ap-south-2.amazonaws.com/banners/uuid.jpg",
                "banner.jpg",
                "image/jpeg",
                12
        );

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.BANNERS))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/banners").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageKey").value("banners/uuid.jpg"));
    }

    @Test
    @DisplayName("POST /api/images/upload - Should return 400 when image is invalid")
    void uploadImage_InvalidImage_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "text bytes".getBytes()
        );

        when(s3ImageService.uploadImage(any(), eq("products")))
                .thenThrow(new InvalidImageException("Invalid image content type: text/plain"));

        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid image content type: text/plain"));
    }

    @Test
    @DisplayName("GET /api/images/download - Should return image bytes with proper content type")
    void downloadImage_Success() throws Exception {
        byte[] imageBytes = "sample image data".getBytes();
        when(s3ImageService.downloadImage("products/test.png")).thenReturn(imageBytes);
        when(s3ImageService.extractKeyFromUrl("products/test.png")).thenReturn("products/test.png");

        mockMvc.perform(get("/api/images/download").param("key", "products/test.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    @DisplayName("DELETE /api/images - Should delete image and return 204 No Content")
    void deleteImage_Success() throws Exception {
        doNothing().when(s3ImageService).deleteImage("products/test.png");

        mockMvc.perform(delete("/api/images").param("key", "products/test.png"))
                .andExpect(status().isNoContent());

        verify(s3ImageService).deleteImage("products/test.png");
    }
}
