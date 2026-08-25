package com.example.project.customer.controller;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BannerController.class)
@Import(GlobalExceptionHandler.class)
class BannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BannerService bannerService;

    @Test
    @DisplayName("POST /api/banners - Should create banner and return 201 Created")
    void createBanner_Success() throws Exception {
        BannerRequest request = new BannerRequest();
        request.setTitle("Summer Sale");
        request.setImageUrl("https://example.com/summer.png");
        request.setLinkType("CATEGORY");
        request.setLinkValue("summer-deals");
        request.setPosition("HOME_HERO");
        request.setSortOrder(1);
        request.setIsActive(true);
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 59));

        BannerResponse response = new BannerResponse(
                1,
                "Summer Sale",
                "https://example.com/summer.png",
                "CATEGORY",
                "summer-deals",
                "HOME_HERO",
                1,
                true,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59),
                LocalDateTime.now()
        );

        when(bannerService.createBanner(any(BannerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bannerId").value(1))
                .andExpect(jsonPath("$.title").value("Summer Sale"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/summer.png"))
                .andExpect(jsonPath("$.linkType").value("CATEGORY"))
                .andExpect(jsonPath("$.linkValue").value("summer-deals"))
                .andExpect(jsonPath("$.position").value("HOME_HERO"))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/banners - Should return 400 Bad Request when title is blank")
    void createBanner_InvalidRequest_BlankTitle() throws Exception {
        BannerRequest request = new BannerRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Title must not be blank"));
    }

    @Test
    @DisplayName("GET /api/banners/{id} - Should return banner details when found")
    void getBannerById_Success() throws Exception {
        BannerResponse response = new BannerResponse(
                1,
                "Mega Discount",
                "https://example.com/discount.png",
                "PRODUCT",
                "prod-123",
                "SIDEBAR",
                2,
                true,
                null,
                null,
                LocalDateTime.now()
        );

        when(bannerService.getBannerById(1)).thenReturn(response);

        mockMvc.perform(get("/api/banners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerId").value(1))
                .andExpect(jsonPath("$.title").value("Mega Discount"))
                .andExpect(jsonPath("$.position").value("SIDEBAR"))
                .andExpect(jsonPath("$.sortOrder").value(2));
    }

    @Test
    @DisplayName("GET /api/banners/{id} - Should return 404 Not Found when banner does not exist")
    void getBannerById_NotFound() throws Exception {
        when(bannerService.getBannerById(99)).thenThrow(new BannerNotFoundException("Banner not found with id: 99"));

        mockMvc.perform(get("/api/banners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Banner not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/banners - Should return all banners")
    void getAllBanners_Success() throws Exception {
        BannerResponse b1 = new BannerResponse(1, "Banner 1", "url1", "TYPE", "val1", "POS", 1, true, null, null, LocalDateTime.now());
        BannerResponse b2 = new BannerResponse(2, "Banner 2", "url2", "TYPE", "val2", "POS", 2, true, null, null, LocalDateTime.now());

        when(bannerService.getAllBanners(null, null)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bannerId").value(1))
                .andExpect(jsonPath("$[1].bannerId").value(2));
    }

    @Test
    @DisplayName("GET /api/banners?active=true&position=HOME_HERO - Should return filtered banners")
    void getAllBanners_Filtered() throws Exception {
        BannerResponse b1 = new BannerResponse(1, "Hero Banner", "url1", "TYPE", "val1", "HOME_HERO", 1, true, null, null, LocalDateTime.now());

        when(bannerService.getAllBanners(true, "HOME_HERO")).thenReturn(List.of(b1));

        mockMvc.perform(get("/api/banners")
                        .param("active", "true")
                        .param("position", "HOME_HERO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Hero Banner"))
                .andExpect(jsonPath("$[0].position").value("HOME_HERO"));
    }

    @Test
    @DisplayName("PUT /api/banners/{id} - Should update and return banner")
    void updateBanner_Success() throws Exception {
        BannerRequest request = new BannerRequest();
        request.setTitle("Updated Title");
        request.setPosition("POPUP");
        request.setSortOrder(3);
        request.setIsActive(false);

        BannerResponse updated = new BannerResponse(
                1,
                "Updated Title",
                null,
                null,
                null,
                "POPUP",
                3,
                false,
                null,
                null,
                LocalDateTime.now()
        );

        when(bannerService.updateBanner(eq(1), any(BannerRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/banners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.position").value("POPUP"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("POST /api/banners/{id}/image - Should upload banner image and return updated banner")
    void uploadBannerImage_Success() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "promo.jpg",
                "image/jpeg",
                "image data".getBytes()
        );

        BannerResponse updated = new BannerResponse(
                1,
                "Summer Sale",
                "https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo-uuid.jpg",
                "CATEGORY",
                "summer-deals",
                "HOME_HERO",
                1,
                true,
                null,
                null,
                LocalDateTime.now()
        );

        when(bannerService.uploadBannerImage(eq(1), any())).thenReturn(updated);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/banners/1/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerId").value(1))
                .andExpect(jsonPath("$.imageUrl").value("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo-uuid.jpg"));
    }

    @Test
    @DisplayName("DELETE /api/banners/{id} - Should delete banner and return 204 No Content")
    void deleteBanner_Success() throws Exception {
        doNothing().when(bannerService).deleteBanner(1);

        mockMvc.perform(delete("/api/banners/1"))
                .andExpect(status().isNoContent());

        verify(bannerService).deleteBanner(1);
    }

    @Test
    @DisplayName("DELETE /api/banners/{id} - Should return 404 when banner to delete does not exist")
    void deleteBanner_NotFound() throws Exception {
        doThrow(new BannerNotFoundException("Banner not found with id: 99"))
                .when(bannerService).deleteBanner(99);

        mockMvc.perform(delete("/api/banners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Banner not found with id: 99"));
    }
}
