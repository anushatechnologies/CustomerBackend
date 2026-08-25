package com.example.project.customer.controller;

import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("POST /api/categories - Success")
    void create_Success() throws Exception {
        CategoryRequest request = new CategoryRequest("Electronics", "electronics", "img.png", true, 1);
        CategoryResponse response = new CategoryResponse(1, "Electronics", "electronics", "img.png", true, 1, LocalDateTime.now());

        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Success")
    void getById_Success() throws Exception {
        CategoryResponse response = new CategoryResponse(1, "Electronics", "electronics", "img.png", true, 1, LocalDateTime.now());

        when(categoryService.getById(1)).thenReturn(response);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Not Found")
    void getById_NotFound() throws Exception {
        when(categoryService.getById(99)).thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/categories - Success")
    void getAll_Success() throws Exception {
        CategoryResponse response = new CategoryResponse(1, "Electronics", "electronics", "img.png", true, 1, LocalDateTime.now());

        when(categoryService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - Success")
    void update_Success() throws Exception {
        CategoryRequest request = new CategoryRequest("Electronics Updated", "electronics", "img.png", true, 2);
        CategoryResponse response = new CategoryResponse(1, "Electronics Updated", "electronics", "img.png", true, 2, LocalDateTime.now());

        when(categoryService.update(eq(1), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics Updated"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Success")
    void delete_Success() throws Exception {
        doNothing().when(categoryService).delete(1);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1);
    }
}
