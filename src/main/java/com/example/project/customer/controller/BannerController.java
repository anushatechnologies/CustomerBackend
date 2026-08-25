package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerRequest request) {
        BannerResponse response = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Banner created successfully", response));
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerResponse> getBannerById(@PathVariable Integer id) {
        return ApiResponse.ok(bannerService.getBannerById(id));
    }

    @GetMapping
    public ApiResponse<List<BannerResponse>> getAllBanners(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String position) {
        return ApiResponse.ok(bannerService.getAllBanners(active, position));
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerResponse> updateBanner(@PathVariable Integer id,
                                                    @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok("Banner updated successfully", bannerService.updateBanner(id, request));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BannerResponse> uploadBannerImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Banner image uploaded successfully", bannerService.uploadBannerImage(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Integer id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
