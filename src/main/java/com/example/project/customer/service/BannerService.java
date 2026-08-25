package com.example.project.customer.service;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    BannerResponse createBanner(BannerRequest request);
    BannerResponse getBannerById(Integer id);
    List<BannerResponse> getAllBanners(Boolean active, String position);
    BannerResponse updateBanner(Integer id, BannerRequest request);
    BannerResponse uploadBannerImage(Integer id, MultipartFile file);
    void deleteBanner(Integer id);
}
