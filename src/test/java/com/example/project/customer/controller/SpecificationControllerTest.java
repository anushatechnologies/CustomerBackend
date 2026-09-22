package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.CategorySpecificationRequest;
import com.example.project.customer.dto.CategorySpecificationResponse;
import com.example.project.customer.dto.SpecificationOptionRequest;
import com.example.project.customer.dto.SpecificationOptionResponse;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;
import com.example.project.customer.entity.SpecificationInputType;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.CategoryService;
import com.example.project.customer.service.SpecificationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SpecificationController.class, CategoryController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class SpecificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SpecificationService specificationService;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("POST /api/specifications - Create specification returns 201")
    void createSpecification_Returns201() throws Exception {
        SpecificationRequest req = SpecificationRequest.builder()
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .options(List.of("Fe 415", "Fe 500", "Fe 550D"))
                .build();

        SpecificationResponse resp = SpecificationResponse.builder()
                .id(1)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .active(true)
                .build();

        when(specificationService.create(any(SpecificationRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/specifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Grade"))
                .andExpect(jsonPath("$.data.key").value("grade"));
    }

    @Test
    @DisplayName("GET /api/specifications - Returns list of specifications")
    void getAllSpecifications_Returns200() throws Exception {
        SpecificationResponse resp = SpecificationResponse.builder()
                .id(1)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .active(true)
                .build();

        when(specificationService.getAll(null, null)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/specifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Grade"));
    }

    @Test
    @DisplayName("POST /api/specifications/{id}/options - Adds option and returns 201")
    void addOption_Returns201() throws Exception {
        SpecificationOptionRequest req = SpecificationOptionRequest.builder()
                .value("Fe 550D")
                .displayOrder(1)
                .build();

        SpecificationOptionResponse resp = SpecificationOptionResponse.builder()
                .id(101)
                .value("Fe 550D")
                .displayOrder(1)
                .build();

        when(specificationService.addOption(eq(1), any(SpecificationOptionRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/specifications/1/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.value").value("Fe 550D"));
    }

    @Test
    @DisplayName("GET /api/categories/{categoryId}/specifications - Returns category specifications")
    void getCategorySpecifications_Returns200() throws Exception {
        CategorySpecificationResponse resp = CategorySpecificationResponse.builder()
                .id(1)
                .mappingId(50)
                .categoryId(10)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .required(true)
                .displayOrder(1)
                .active(true)
                .options(List.of("Fe 415", "Fe 500", "Fe 550D"))
                .build();

        when(specificationService.getCategorySpecifications(eq(10), eq(true))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/categories/10/specifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Grade"))
                .andExpect(jsonPath("$.data[0].required").value(true))
                .andExpect(jsonPath("$.data[0].options[0]").value("Fe 415"));
    }

    @Test
    @DisplayName("POST /api/categories/{categoryId}/specifications - Assigns specification to category")
    void addCategorySpecification_Returns201() throws Exception {
        CategorySpecificationRequest req = CategorySpecificationRequest.builder()
                .specificationId(1)
                .required(true)
                .displayOrder(1)
                .build();

        CategorySpecificationResponse resp = CategorySpecificationResponse.builder()
                .id(1)
                .mappingId(50)
                .categoryId(10)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .required(true)
                .displayOrder(1)
                .active(true)
                .build();

        when(specificationService.addCategorySpecification(eq(10), any(CategorySpecificationRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/categories/10/specifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Grade"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{categoryId}/specifications/{specificationId} - Removes mapping")
    void removeCategorySpecification_Returns200() throws Exception {
        mockMvc.perform(delete("/api/categories/10/specifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
