package com.example.project.customer.controller;

import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("POST /api/products - Success")
    void create_Success() throws Exception {
        ProductRequest request = new ProductRequest(1, "Product 1", "Desc", new BigDecimal("19.99"), 10, "pcs", "img.jpg", true);
        ProductResponse response = new ProductResponse(1, 1, "Product 1", "Desc", new BigDecimal("19.99"), 10, "pcs", "img.jpg", true, LocalDateTime.now());

        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.title").value("Product 1"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Success")
    void getById_Success() throws Exception {
        ProductResponse response = new ProductResponse(1, 1, "Product 1", "Desc", new BigDecimal("19.99"), 10, "pcs", "img.jpg", true, LocalDateTime.now());

        when(productService.getById(1)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Not Found")
    void getById_NotFound() throws Exception {
        when(productService.getById(99)).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/products - Success")
    void getAll_Success() throws Exception {
        ProductResponse response = new ProductResponse(1, 1, "Product 1", "Desc", new BigDecimal("19.99"), 10, "pcs", "img.jpg", true, LocalDateTime.now());

        when(productService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Success")
    void update_Success() throws Exception {
        ProductRequest request = new ProductRequest(1, "Product Updated", "Desc", new BigDecimal("29.99"), 15, "pcs", "img.jpg", true);
        ProductResponse response = new ProductResponse(1, 1, "Product Updated", "Desc", new BigDecimal("29.99"), 15, "pcs", "img.jpg", true, LocalDateTime.now());

        when(productService.update(eq(1), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Product Updated"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - Success")
    void delete_Success() throws Exception {
        doNothing().when(productService).delete(1);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1);
    }
}
