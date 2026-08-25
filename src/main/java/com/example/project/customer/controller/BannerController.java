package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerRequest request) {
        BannerResponse created = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Banner created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable Integer id) {
        BannerResponse banner = bannerService.getBannerById(id);
        return ResponseEntity.ok(ApiResponse.ok("Banner retrieved successfully", banner));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String position) {
        List<BannerResponse> banners = bannerService.getAllBanners(active, position);
        return ResponseEntity.ok(ApiResponse.ok("Banners retrieved successfully", banners));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(@PathVariable Integer id,
                                                                    @Valid @RequestBody BannerRequest request) {
        BannerResponse updated = bannerService.updateBanner(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Banner updated successfully", updated));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BannerResponse>> uploadBannerImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        BannerResponse updated = bannerService.uploadBannerImage(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Banner image uploaded successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Integer id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.ok("Banner deleted successfully", null));
    }
}
