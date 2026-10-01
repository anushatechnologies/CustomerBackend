package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PromotionalVideoResponse;
import com.example.project.customer.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final BannerService bannerService;

    @GetMapping("/video/active")
    public ResponseEntity<ApiResponse<PromotionalVideoResponse>> getActivePromotionalVideo() {
        PromotionalVideoResponse video = bannerService.getActivePromotionalVideo();
        if (video != null) {
            return ResponseEntity.ok(ApiResponse.ok("Active promotional video found", video));
        } else {
            return ResponseEntity.ok(ApiResponse.ok("No active promotional video available", null));
        }
    }
}
