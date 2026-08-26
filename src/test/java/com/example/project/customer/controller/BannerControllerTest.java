package com.example.project.customer.controller;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.exception.BannerNotFoundException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.BannerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BannerController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class BannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BannerService bannerService;

    @Test
    @DisplayName("POST /api/banners - Should create banner and return 201 Created wrapped in ApiResponse")
    void createBanner_Success() throws Exception {
        BannerRequest request = BannerRequest.builder()
                .title("Summer Sale")
                .subtitle("Save up to 50%")
                .imageUrl("https://example.com/summer.png")
                .linkType("CATEGORY")
                .linkValue("summer-deals")
                .position("HOME_HERO")
                .sortOrder(1)
                .active(true)
                .startDate(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 6, 30, 23, 59))
                .build();

        BannerResponse response = BannerResponse.builder()
                .bannerId(1)
                .title("Summer Sale")
                .subtitle("Save up to 50%")
                .imageUrl("https://example.com/summer.png")
                .linkType("CATEGORY")
                .linkValue("summer-deals")
                .position("HOME_HERO")
                .sortOrder(1)
                .active(true)
                .startDate(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 6, 30, 23, 59))
                .createdAt(LocalDateTime.now())
                .build();

        when(bannerService.createBanner(any(BannerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.bannerId").value(1))
                .andExpect(jsonPath("$.data.title").value("Summer Sale"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/summer.png"))
                .andExpect(jsonPath("$.data.linkType").value("CATEGORY"))
                .andExpect(jsonPath("$.data.linkValue").value("summer-deals"))
                .andExpect(jsonPath("$.data.position").value("HOME_HERO"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/banners - Should return 400 Bad Request when title is blank")
    void createBanner_InvalidRequest_BlankTitle() throws Exception {
        BannerRequest request = BannerRequest.builder().title("").build();

        mockMvc.perform(post("/api/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/banners/{id} - Should return banner details when found")
    void getBannerById_Success() throws Exception {
        BannerResponse response = BannerResponse.builder()
                .bannerId(1)
                .title("Mega Discount")
                .imageUrl("https://example.com/discount.png")
                .linkType("PRODUCT")
                .linkValue("prod-123")
                .position("SIDEBAR")
                .sortOrder(2)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(bannerService.getBannerById(1)).thenReturn(response);

        mockMvc.perform(get("/api/banners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bannerId").value(1))
                .andExpect(jsonPath("$.data.title").value("Mega Discount"))
                .andExpect(jsonPath("$.data.position").value("SIDEBAR"))
                .andExpect(jsonPath("$.data.sortOrder").value(2));
    }

    @Test
    @DisplayName("GET /api/banners/{id} - Should return 404 Not Found when banner does not exist")
    void getBannerById_NotFound() throws Exception {
        when(bannerService.getBannerById(99)).thenThrow(new BannerNotFoundException("Banner not found with id: 99"));

        mockMvc.perform(get("/api/banners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Banner not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/banners - Should return all banners")
    void getAllBanners_Success() throws Exception {
        BannerResponse b1 = BannerResponse.builder().bannerId(1).title("Banner 1").position("POS").sortOrder(1).active(true).build();
        BannerResponse b2 = BannerResponse.builder().bannerId(2).title("Banner 2").position("POS").sortOrder(2).active(true).build();

        when(bannerService.getAllBanners(null, null)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].bannerId").value(1))
                .andExpect(jsonPath("$.data[1].bannerId").value(2));
    }

    @Test
    @DisplayName("GET /api/banners?active=true&position=HOME_HERO - Should return filtered banners")
    void getAllBanners_Filtered() throws Exception {
        BannerResponse b1 = BannerResponse.builder().bannerId(1).title("Hero Banner").position("HOME_HERO").sortOrder(1).active(true).build();

        when(bannerService.getAllBanners(true, "HOME_HERO")).thenReturn(List.of(b1));

        mockMvc.perform(get("/api/banners")
                        .param("active", "true")
                        .param("position", "HOME_HERO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Hero Banner"))
                .andExpect(jsonPath("$.data[0].position").value("HOME_HERO"));
    }

    @Test
    @DisplayName("PUT /api/banners/{id} - Should update and return banner")
    void updateBanner_Success() throws Exception {
        BannerRequest request = BannerRequest.builder()
                .title("Updated Title")
                .position("POPUP")
                .sortOrder(3)
                .active(false)
                .build();

        BannerResponse updated = BannerResponse.builder()
                .bannerId(1)
                .title("Updated Title")
                .position("POPUP")
                .sortOrder(3)
                .active(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(bannerService.updateBanner(eq(1), any(BannerRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/banners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.position").value("POPUP"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("POST /api/banners/{id}/image - Should upload banner image and return updated banner")
    void uploadBannerImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "promo.jpg",
                "image/jpeg",
                "image data".getBytes()
        );

        BannerResponse updated = BannerResponse.builder()
                .bannerId(1)
                .title("Summer Sale")
                .imageUrl("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo-uuid.jpg")
                .linkType("CATEGORY")
                .linkValue("summer-deals")
                .position("HOME_HERO")
                .sortOrder(1)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(bannerService.uploadBannerImage(eq(1), any())).thenReturn(updated);

        mockMvc.perform(multipart("/api/banners/1/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bannerId").value(1))
                .andExpect(jsonPath("$.data.imageUrl").value("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo-uuid.jpg"));
    }

    @Test
    @DisplayName("DELETE /api/banners/{id} - Should delete banner and return 200 OK with ApiResponse")
    void deleteBanner_Success() throws Exception {
        doNothing().when(bannerService).deleteBanner(1);

        mockMvc.perform(delete("/api/banners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerService).deleteBanner(1);
    }

    @Test
    @DisplayName("DELETE /api/banners/{id} - Should return 404 when banner to delete does not exist")
    void deleteBanner_NotFound() throws Exception {
        doThrow(new BannerNotFoundException("Banner not found with id: 99"))
                .when(bannerService).deleteBanner(99);

        mockMvc.perform(delete("/api/banners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Banner not found with id: 99"));
    }
}
