package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.S3ImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3ImageService s3ImageService;

    @Test
    @DisplayName("POST /api/upload/file - Unauthenticated request should be rejected with 401/403")
    void uploadFile_Unauthenticated_Rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(file))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/upload/file - Customer role should be rejected with 403 Forbidden")
    void uploadFile_CustomerRole_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/upload/file - Authorized seller upload succeeds")
    void uploadFile_Seller_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "catalog.png", "image/png", "png data".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("products/catalog-123.png")
                .fileUrl("https://s3.amazonaws.com/products/catalog-123.png")
                .fileName("catalog.png")
                .mimeType("image/png")
                .fileSize(8L)
                .build();

        when(s3ImageService.uploadImage(any(), eq("products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/upload/file")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("products/catalog-123.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/upload/image - Authorized admin upload succeeds")
    void uploadImage_Admin_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "banner.webp", "image/webp", "webp data".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("products/banner-456.webp")
                .fileUrl("https://s3.amazonaws.com/products/banner-456.webp")
                .fileName("banner.webp")
                .mimeType("image/webp")
                .fileSize(9L)
                .build();

        when(s3ImageService.uploadImage(any(), eq("products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/upload/image")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("products/banner-456.webp"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/upload/file - File exceeding 15MB is rejected with 400 Bad Request")
    void uploadFile_Exceeds15MB_Rejected() throws Exception {
        // Create a mock file with oversized size reported (16MB)
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[10]
        ) {
            @Override
            public long getSize() {
                return 16L * 1024 * 1024;
            }
        };

        mockMvc.perform(multipart("/api/upload/file").file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File size exceeds the 15MB limit"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/upload/file - Invalid file extension/type is rejected with 400 Bad Request")
    void uploadFile_InvalidFileType_Rejected() throws Exception {
        MockMultipartFile maliciousScript = new MockMultipartFile(
                "file", "malicious.sh", "application/x-sh", "echo 'evil'".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(maliciousScript))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
