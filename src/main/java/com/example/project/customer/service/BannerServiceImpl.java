package com.example.project.customer.service;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.dto.BannerVideoUploadResponse;
import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.dto.PromotionalVideoResponse;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.exception.BannerNotFoundException;
import com.example.project.customer.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
        String effectivePoster = request.getPosterUrl();
        if (effectivePoster == null && request.getThumbnailUrl() != null) {
            effectivePoster = request.getThumbnailUrl();
        }

        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .imageUrl(request.getImageUrl())
                .videoUrl(request.getVideoUrl())
                .posterUrl(effectivePoster)
                .badge(request.getBadge() != null ? request.getBadge() : "24-HOUR DISPATCH")
                .ctaText(request.getCtaText() != null ? request.getCtaText() : "Explore 24H Catalog")
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
        String oldVideoUrl = banner.getVideoUrl();
        String oldPosterUrl = banner.getPosterUrl();

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            banner.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            banner.setSubtitle(request.getSubtitle());
        }
        if (request.getImageUrl() != null) {
            banner.setImageUrl(request.getImageUrl());
        }
        if (request.getVideoUrl() != null) {
            banner.setVideoUrl(request.getVideoUrl());
        }
        if (request.getPosterUrl() != null) {
            banner.setPosterUrl(request.getPosterUrl());
        } else if (request.getThumbnailUrl() != null) {
            banner.setPosterUrl(request.getThumbnailUrl());
        }
        if (request.getBadge() != null) {
            banner.setBadge(request.getBadge());
        }
        if (request.getCtaText() != null) {
            banner.setCtaText(request.getCtaText());
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
        if (request.getVideoUrl() != null && oldVideoUrl != null && !oldVideoUrl.isBlank() && !oldVideoUrl.equals(request.getVideoUrl())) {
            s3ImageService.deleteImage(oldVideoUrl);
        }
        if (request.getPosterUrl() != null && oldPosterUrl != null && !oldPosterUrl.isBlank() && !oldPosterUrl.equals(request.getPosterUrl()) && !oldPosterUrl.equals(oldImageUrl)) {
            s3ImageService.deleteImage(oldPosterUrl);
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
    public BannerVideoUploadResponse uploadBannerVideo(Integer id, MultipartFile file) {
        Banner banner = findBanner(id);
        String oldVideoUrl = banner.getVideoUrl();

        ImageUploadResponse uploadResponse = s3ImageService.uploadVideo(file, ImageFolder.BANNERS);
        banner.setVideoUrl(uploadResponse.getFileUrl());
        Banner saved = bannerRepository.save(banner);

        if (oldVideoUrl != null && !oldVideoUrl.isBlank() && !oldVideoUrl.equals(uploadResponse.getFileUrl())) {
            s3ImageService.deleteImage(oldVideoUrl);
        }

        return BannerVideoUploadResponse.builder()
                .bannerId(saved.getBannerId())
                .videoUrl(saved.getVideoUrl())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionalVideoResponse getActivePromotionalVideo() {
        List<Banner> banners = bannerRepository.findByPositionAndActiveTrueOrderBySortOrderAsc("HOME_VIDEO");
        LocalDateTime now = LocalDateTime.now();

        Banner activeVideoBanner = banners.stream()
                .filter(b -> b.getVideoUrl() != null && !b.getVideoUrl().isBlank())
                .filter(b -> b.getStartDate() == null || !b.getStartDate().isAfter(now))
                .filter(b -> b.getEndDate() == null || !b.getEndDate().isBefore(now))
                .findFirst()
                .orElse(null);

        if (activeVideoBanner == null) {
            return null;
        }

        String effectivePoster = activeVideoBanner.getPosterUrl() != null && !activeVideoBanner.getPosterUrl().isBlank()
                ? activeVideoBanner.getPosterUrl()
                : activeVideoBanner.getImageUrl();

        return PromotionalVideoResponse.builder()
                .id(activeVideoBanner.getBannerId())
                .videoUrl(activeVideoBanner.getVideoUrl())
                .posterUrl(effectivePoster)
                .title(activeVideoBanner.getTitle())
                .subtitle(activeVideoBanner.getSubtitle())
                .badge(activeVideoBanner.getBadge() != null ? activeVideoBanner.getBadge() : "24-HOUR DISPATCH")
                .ctaText(activeVideoBanner.getCtaText() != null ? activeVideoBanner.getCtaText() : "Explore 24H Catalog")
                .targetScreen(activeVideoBanner.getLinkValue() != null ? activeVideoBanner.getLinkValue() : "TwentyFourHourDelivery")
                .active(Boolean.TRUE.equals(activeVideoBanner.getActive()))
                .build();
    }

    @Override
    public void deleteBanner(Integer id) {
        Banner banner = findBanner(id);
        String imageUrl = banner.getImageUrl();
        String videoUrl = banner.getVideoUrl();
        String posterUrl = banner.getPosterUrl();
        bannerRepository.delete(banner);

        if (imageUrl != null && !imageUrl.isBlank()) {
            s3ImageService.deleteImage(imageUrl);
        }
        if (videoUrl != null && !videoUrl.isBlank()) {
            s3ImageService.deleteImage(videoUrl);
        }
        if (posterUrl != null && !posterUrl.isBlank() && !posterUrl.equals(imageUrl)) {
            s3ImageService.deleteImage(posterUrl);
        }
    }

    private Banner findBanner(Integer id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner not found with id: " + id));
    }

    private BannerResponse mapToResponse(Banner banner) {
        String effectivePoster = banner.getPosterUrl() != null && !banner.getPosterUrl().isBlank()
                ? banner.getPosterUrl()
                : banner.getImageUrl();

        return BannerResponse.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .videoUrl(banner.getVideoUrl())
                .posterUrl(banner.getPosterUrl())
                .thumbnailUrl(effectivePoster)
                .badge(banner.getBadge())
                .ctaText(banner.getCtaText())
                .linkType(banner.getLinkType())
                .linkValue(banner.getLinkValue())
                .position(banner.getPosition())
                .sortOrder(banner.getSortOrder())
                .active(banner.getActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt() != null ? banner.getUpdatedAt() : banner.getCreatedAt())
                .build();
    }
}
