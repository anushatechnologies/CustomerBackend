package com.example.project.customer.service;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.exception.BannerNotFoundException;
import com.example.project.customer.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final S3ImageService s3ImageService;

    @Override
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .imageUrl(request.getImageUrl())
                .linkType(request.getLinkType())
                .linkValue(request.getLinkValue())
                .position(request.getPosition())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return mapToResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(Integer id) {
        Banner banner = findBanner(id);
        return mapToResponse(banner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners(Boolean active, String position) {
        List<Banner> banners;
        if (position != null && !position.isBlank()) {
            if (Boolean.TRUE.equals(active)) {
                banners = bannerRepository.findByPositionAndActiveTrueOrderBySortOrderAsc(position.trim());
            } else {
                banners = bannerRepository.findByPositionOrderBySortOrderAsc(position.trim());
            }
        } else if (Boolean.TRUE.equals(active)) {
            banners = bannerRepository.findByActiveTrueOrderBySortOrderAsc();
        } else {
            banners = bannerRepository.findAllByOrderBySortOrderAsc();
        }

        return banners.stream().map(this::mapToResponse).toList();
    }

    @Override
    public BannerResponse updateBanner(Integer id, BannerRequest request) {
        Banner banner = findBanner(id);
        String oldImageUrl = banner.getImageUrl();

        banner.setTitle(request.getTitle());
        if (request.getSubtitle() != null) {
            banner.setSubtitle(request.getSubtitle());
        }
        if (request.getImageUrl() != null) {
            banner.setImageUrl(request.getImageUrl());
        }
        if (request.getLinkType() != null) {
            banner.setLinkType(request.getLinkType());
        }
        if (request.getLinkValue() != null) {
            banner.setLinkValue(request.getLinkValue());
        }
        if (request.getPosition() != null) {
            banner.setPosition(request.getPosition());
        }
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }
        if (request.getStartDate() != null) {
            banner.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            banner.setEndDate(request.getEndDate());
        }

        Banner saved = bannerRepository.save(banner);

        if (request.getImageUrl() != null && oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(request.getImageUrl())) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved);
    }

    @Override
    public BannerResponse uploadBannerImage(Integer id, MultipartFile file) {
        Banner banner = findBanner(id);
        String oldImageUrl = banner.getImageUrl();

        ImageUploadResponse uploadResponse = s3ImageService.uploadImage(file, ImageFolder.BANNERS);
        banner.setImageUrl(uploadResponse.getImageUrl());
        Banner saved = bannerRepository.save(banner);

        if (oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(uploadResponse.getImageUrl())) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved);
    }

    @Override
    public void deleteBanner(Integer id) {
        Banner banner = findBanner(id);
        String imageUrl = banner.getImageUrl();
        bannerRepository.delete(banner);

        if (imageUrl != null && !imageUrl.isBlank()) {
            s3ImageService.deleteImage(imageUrl);
        }
    }

    private Banner findBanner(Integer id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner not found with id: " + id));
    }

    private BannerResponse mapToResponse(Banner banner) {
        return BannerResponse.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .linkType(banner.getLinkType())
                .linkValue(banner.getLinkValue())
                .position(banner.getPosition())
                .sortOrder(banner.getSortOrder())
                .active(banner.getActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
