package com.example.project.customer.controller;

import com.example.project.customer.dto.CategoryResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubcategoryWebsiteVisibilityControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SubcategoryService subcategoryService;

    private SubcategoryResponse visibleSubcategory;
    private SubcategoryResponse hiddenSubcategory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CategoryController(categoryService),
                new SubcategoryController(subcategoryService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();

        visibleSubcategory = SubcategoryResponse.builder()
                .subcategoryId(101)
                .categoryId(10)
                .name("Wall Art & Paintings")
                .active(true)
                .visibleOnWebsite(true)
                .build();

        hiddenSubcategory = SubcategoryResponse.builder()
                .subcategoryId(102)
                .categoryId(10)
                .name("Luxury Chandeliers")
                .active(true)
                .visibleOnWebsite(false)
                .build();
    }

    @Test
    @DisplayName("Existing App: GET /api/subcategories?categoryId=10&active=true returns both subcategories")
    void existingApp_FetchesSubcategories_WithoutWebsiteFilter() throws Exception {
        when(subcategoryService.getAll(eq(10), eq(true), eq(null)))
                .thenReturn(List.of(visibleSubcategory, hiddenSubcategory));

        mockMvc.perform(get("/api/subcategories")
                        .param("categoryId", "10")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Wall Art & Paintings"))
                .andExpect(jsonPath("$.data[1].name").value("Luxury Chandeliers"));

        verify(subcategoryService).getAll(10, true, null);
    }

    @Test
    @DisplayName("New Website: GET /api/subcategories?categoryId=10&active=true&visibleOnWebsite=true returns only visible subcategories")
    void newWebsite_FetchesSubcategories_WithVisibleOnWebsiteParam() throws Exception {
        when(subcategoryService.getAll(eq(10), eq(true), eq(true)))
                .thenReturn(List.of(visibleSubcategory));

        mockMvc.perform(get("/api/subcategories")
                        .param("categoryId", "10")
                        .param("active", "true")
                        .param("visibleOnWebsite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Wall Art & Paintings"))
                .andExpect(jsonPath("$.data[0].visibleOnWebsite").value(true));

        verify(subcategoryService).getAll(10, true, true);
    }

    @Test
    @DisplayName("New Website Dedicated Endpoint: GET /api/subcategories/website?categoryId=10 returns only website visible subcategories")
    void newWebsite_DedicatedEndpoint_ReturnsWebsiteSubcategories() throws Exception {
        when(subcategoryService.getWebsiteSubcategories(eq(10)))
                .thenReturn(List.of(visibleSubcategory));

        mockMvc.perform(get("/api/subcategories/website")
                        .param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Wall Art & Paintings"));

        verify(subcategoryService).getWebsiteSubcategories(10);
    }

    @Test
    @DisplayName("Admin Toggle: PATCH /api/subcategories/{id}/website-visibility updates website visibility")
    void adminToggle_PatchWebsiteVisibility_Success() throws Exception {
        SubcategoryResponse updated = SubcategoryResponse.builder()
                .subcategoryId(102)
                .categoryId(10)
                .name("Luxury Chandeliers")
                .active(true)
                .visibleOnWebsite(false)
                .build();

        when(subcategoryService.updateWebsiteVisibility(eq(102), eq(false)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/subcategories/102/website-visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("visibleOnWebsite", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subcategoryId").value(102))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.visibleOnWebsite").value(false));

        verify(subcategoryService).updateWebsiteVisibility(102, false);
    }

    @Test
    @DisplayName("Category API: GET /api/categories/{id}?website=true queries website-filtered subcategories")
    void categoryApi_WebsiteParam_FiltersSubcategories() throws Exception {
        CategoryResponse websiteCategoryResponse = CategoryResponse.builder()
                .categoryId(10)
                .name("Home Decors")
                .subcategories(List.of(visibleSubcategory))
                .build();

        when(categoryService.getById(eq(10), eq(true)))
                .thenReturn(websiteCategoryResponse);

        mockMvc.perform(get("/api/categories/10")
                        .param("website", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(10))
                .andExpect(jsonPath("$.data.subcategories.length()").value(1))
                .andExpect(jsonPath("$.data.subcategories[0].name").value("Wall Art & Paintings"));

        verify(categoryService).getById(10, true);
    }
}
