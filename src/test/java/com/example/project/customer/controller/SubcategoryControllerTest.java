package com.example.project.customer.controller;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.SubcategoryService;
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

@WebMvcTest(SubcategoryController.class)
@Import(GlobalExceptionHandler.class)
class SubcategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubcategoryService subcategoryService;

    @Test
    @DisplayName("POST /api/subcategories - Success")
    void create_Success() throws Exception {
        SubcategoryRequest request = new SubcategoryRequest(1, "Phones", "phones", "phone.png", true, 1);
        SubcategoryResponse response = new SubcategoryResponse(1, 1, "Phones", "phones", "phone.png", true, 1, LocalDateTime.now());

        when(subcategoryService.create(any(SubcategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/subcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subcategoryId").value(1))
                .andExpect(jsonPath("$.name").value("Phones"));
    }

    @Test
    @DisplayName("GET /api/subcategories/{id} - Success")
    void getById_Success() throws Exception {
        SubcategoryResponse response = new SubcategoryResponse(1, 1, "Phones", "phones", "phone.png", true, 1, LocalDateTime.now());

        when(subcategoryService.getById(1)).thenReturn(response);

        mockMvc.perform(get("/api/subcategories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcategoryId").value(1));
    }

    @Test
    @DisplayName("GET /api/subcategories/{id} - Not Found")
    void getById_NotFound() throws Exception {
        when(subcategoryService.getById(99)).thenThrow(new ResourceNotFoundException("Subcategory not found with id: 99"));

        mockMvc.perform(get("/api/subcategories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subcategory not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/subcategories - Success")
    void getAll_Success() throws Exception {
        SubcategoryResponse response = new SubcategoryResponse(1, 1, "Phones", "phones", "phone.png", true, 1, LocalDateTime.now());

        when(subcategoryService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/subcategories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/subcategories/{id} - Success")
    void update_Success() throws Exception {
        SubcategoryRequest request = new SubcategoryRequest(1, "Phones Updated", "phones", "phone.png", true, 2);
        SubcategoryResponse response = new SubcategoryResponse(1, 1, "Phones Updated", "phones", "phone.png", true, 2, LocalDateTime.now());

        when(subcategoryService.update(eq(1), any(SubcategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/subcategories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phones Updated"));
    }

    @Test
    @DisplayName("DELETE /api/subcategories/{id} - Success")
    void delete_Success() throws Exception {
        doNothing().when(subcategoryService).delete(1);

        mockMvc.perform(delete("/api/subcategories/1"))
                .andExpect(status().isNoContent());

        verify(subcategoryService).delete(1);
    }
}
