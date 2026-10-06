package com.example.project.customer.controller;

import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.CategoryService;
import com.example.project.customer.service.SubcategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CategorySubcategoryImageValidationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SubcategoryService subcategoryService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CategoryController(categoryService),
                new SubcategoryController(subcategoryService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // ==========================================
    // CATEGORY CONTROLLER VALIDATION
    // ==========================================

    @Test
    @DisplayName("POST /api/categories - rejects blob URL with 400 Bad Request")
    void createCategory_BlobUrl_RejectedWith400() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Steel")
                .imageUrl("blob:http://localhost:5173/58fca786-fa31-4b60-bb91-e03efe75662c")
                .build();

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(categoryService, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - rejects blob URL with 400 Bad Request")
    void updateCategory_BlobUrl_RejectedWith400() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Steel Updated")
                .imageUrl("blob:http://localhost:5173/bad-uuid")
                .build();

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(categoryService, never()).update(any(), any());
    }

    @Test
    @DisplayName("POST /api/categories - rejects base64 data URI with 400 Bad Request")
    void createCategory_DataUri_RejectedWith400() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Cement")
                .imageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .build();

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(categoryService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/categories - accepts valid S3 URL and returns 201 Created")
    void createCategory_ValidS3Url_Success() throws Exception {
        String s3Url = "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/categories/cat-1.jpg";
        CategoryRequest request = CategoryRequest.builder()
                .name("Cement")
                .imageUrl(s3Url)
                .build();

        CategoryResponse response = CategoryResponse.builder()
                .categoryId(1)
                .name("Cement")
                .imageUrl(s3Url)
                .build();

        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").value(s3Url));

        verify(categoryService).create(any());
    }

    // ==========================================
    // SUBCATEGORY CONTROLLER VALIDATION
    // ==========================================

    @Test
    @DisplayName("POST /api/subcategories - rejects blob URL with 400 Bad Request")
    void createSubcategory_BlobUrl_RejectedWith400() throws Exception {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("TMT Bars")
                .imageUrl("blob:http://localhost:5173/58fca786-fa31-4b60-bb91-e03efe75662c")
                .build();

        mockMvc.perform(post("/api/subcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(subcategoryService, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/subcategories/{id} - rejects blob URL with 400 Bad Request")
    void updateSubcategory_BlobUrl_RejectedWith400() throws Exception {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("TMT Bars Updated")
                .imageUrl("blob:http://localhost:5173/another-blob")
                .build();

        mockMvc.perform(put("/api/subcategories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(subcategoryService, never()).update(any(), any());
    }

    @Test
    @DisplayName("POST /api/subcategories - rejects base64 data URI with 400 Bad Request")
    void createSubcategory_DataUri_RejectedWith400() throws Exception {
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Structural Steel")
                .imageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .build();

        mockMvc.perform(post("/api/subcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrl"));

        verify(subcategoryService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/subcategories - accepts valid S3 URL and returns 201 Created")
    void createSubcategory_ValidS3Url_Success() throws Exception {
        String s3Url = "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/subcategories/sub-1.jpg";
        SubcategoryRequest request = SubcategoryRequest.builder()
                .categoryId(1)
                .name("Structural Steel")
                .imageUrl(s3Url)
                .build();

        SubcategoryResponse response = SubcategoryResponse.builder()
                .subcategoryId(10)
                .categoryId(1)
                .name("Structural Steel")
                .imageUrl(s3Url)
                .build();

        when(subcategoryService.create(any(SubcategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/subcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").value(s3Url));

        verify(subcategoryService).create(any());
    }
}
