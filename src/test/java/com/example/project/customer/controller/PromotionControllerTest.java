package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.PromotionalVideoResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.BannerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromotionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BannerService bannerService;

    @Test
    @DisplayName("GET /api/promotions/video/active - Should return active promotional video when available")
    void getActivePromotionalVideo_Found() throws Exception {
        PromotionalVideoResponse promo = PromotionalVideoResponse.builder()
                .id(1)
                .videoUrl("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo_24h.mp4")
                .posterUrl("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/poster.jpg")
                .title("Need Materials Urgently on Site?")
                .subtitle("Order before 4 PM for guaranteed next-day delivery.")
                .badge("24-HOUR DISPATCH")
                .ctaText("Explore 24H Catalog")
                .targetScreen("TwentyFourHourDelivery")
                .active(true)
                .build();

        when(bannerService.getActivePromotionalVideo()).thenReturn(promo);

        mockMvc.perform(get("/api/promotions/video/active")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Active promotional video found"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.videoUrl").value("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/promo_24h.mp4"))
                .andExpect(jsonPath("$.data.posterUrl").value("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/poster.jpg"))
                .andExpect(jsonPath("$.data.title").value("Need Materials Urgently on Site?"))
                .andExpect(jsonPath("$.data.subtitle").value("Order before 4 PM for guaranteed next-day delivery."))
                .andExpect(jsonPath("$.data.badge").value("24-HOUR DISPATCH"))
                .andExpect(jsonPath("$.data.ctaText").value("Explore 24H Catalog"))
                .andExpect(jsonPath("$.data.targetScreen").value("TwentyFourHourDelivery"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/promotions/video/active - Should return 200 with data: null when no active video")
    void getActivePromotionalVideo_NotFound() throws Exception {
        when(bannerService.getActivePromotionalVideo()).thenReturn(null);

        mockMvc.perform(get("/api/promotions/video/active")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("No active promotional video available"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
