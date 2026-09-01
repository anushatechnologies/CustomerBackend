package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.BrandRequest;
import com.example.project.customer.dto.BrandResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.BrandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(BrandController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BrandService brandService;

    @Test
    @DisplayName("POST /api/brands - Should create brand and return 201 Created")
    void createBrand_Success() throws Exception {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .imageUrl("https://cdn.hinchmart.com/brands/tata_tiscon.png")
                .active(true)
                .sortOrder(1)
                .build();

        BrandResponse response = BrandResponse.builder()
                .brandId(1)
                .subcategoryId(10)
                .subcategoryName("TMT Steel")
                .categoryId(1)
                .categoryName("Civil & Structural")
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .imageUrl("https://cdn.hinchmart.com/brands/tata_tiscon.png")
                .active(true)
                .sortOrder(1)
                .productCount(0)
                .build();

        when(brandService.create(any(BrandRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.brandId").value(1))
                .andExpect(jsonPath("$.data.name").value("Tata Tiscon"))
                .andExpect(jsonPath("$.data.slug").value("tata-tiscon"))
                .andExpect(jsonPath("$.data.subcategoryId").value(10));
    }

    @Test
    @DisplayName("POST /api/brands - Should return 400 Bad Request when name is blank")
    void createBrand_InvalidRequest_BlankName() throws Exception {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("")
                .build();

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/brands/{id} - Should return brand details when found")
    void getBrandById_Success() throws Exception {
        BrandResponse response = BrandResponse.builder()
                .brandId(1)
                .subcategoryId(10)
                .name("Tata Tiscon")
                .slug("tata-tiscon")
                .active(true)
                .build();

        when(brandService.getById(1)).thenReturn(response);

        mockMvc.perform(get("/api/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.brandId").value(1))
                .andExpect(jsonPath("$.data.name").value("Tata Tiscon"));
    }

    @Test
    @DisplayName("GET /api/brands/{id} - Should return 404 Not Found when brand does not exist")
    void getBrandById_NotFound() throws Exception {
        when(brandService.getById(99)).thenThrow(new ResourceNotFoundException("Brand not found with id: 99"));

        mockMvc.perform(get("/api/brands/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/brands - Should return brands list")
    void getAllBrands_Success() throws Exception {
        BrandResponse b1 = BrandResponse.builder().brandId(1).name("Tata Tiscon").build();
        BrandResponse b2 = BrandResponse.builder().brandId(2).name("JSW Steel").build();

        when(brandService.getAll(null, null, null)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].brandId").value(1))
                .andExpect(jsonPath("$.data[1].brandId").value(2));
    }

    @Test
    @DisplayName("PUT /api/brands/{id} - Should update and return brand")
    void updateBrand_Success() throws Exception {
        BrandRequest request = BrandRequest.builder()
                .subcategoryId(10)
                .name("Tata Tiscon Updated")
                .build();

        BrandResponse updated = BrandResponse.builder()
                .brandId(1)
                .subcategoryId(10)
                .name("Tata Tiscon Updated")
                .build();

        when(brandService.update(eq(1), any(BrandRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tata Tiscon Updated"));
    }

    @Test
    @DisplayName("DELETE /api/brands/{id} - Should delete brand")
    void deleteBrand_Success() throws Exception {
        doNothing().when(brandService).delete(1);

        mockMvc.perform(delete("/api/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(brandService).delete(1);
    }
}
