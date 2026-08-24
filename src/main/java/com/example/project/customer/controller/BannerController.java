package com.example.project.customer.controller;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @PostMapping
    public ResponseEntity<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.createBanner(request));
    }

    @GetMapping("/{id}")
    public BannerResponse getBannerById(@PathVariable Integer id) {
        return bannerService.getBannerById(id);
    }

    @GetMapping
    public List<BannerResponse> getAllBanners(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String position) {
        return bannerService.getAllBanners(active, position);
    }

    @PutMapping("/{id}")
    public BannerResponse updateBanner(@PathVariable Integer id,
                                       @Valid @RequestBody BannerRequest request) {
        return bannerService.updateBanner(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Integer id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
