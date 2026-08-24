package com.example.project.customer.service;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.exception.BannerNotFoundException;
import com.example.project.customer.repository.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = new Banner();
        applyRequest(banner, request);
        return toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(Integer id) {
        return toResponse(findBanner(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners(Boolean active, String position) {
        List<Banner> banners;
        if (Boolean.TRUE.equals(active) && position != null && !position.isBlank()) {
            banners = bannerRepository.findByPositionAndIsActiveTrueOrderBySortOrderAsc(position);
        } else if (Boolean.TRUE.equals(active)) {
            banners = bannerRepository.findByIsActiveTrueOrderBySortOrderAsc();
        } else if (position != null && !position.isBlank()) {
            banners = bannerRepository.findByPositionOrderBySortOrderAsc(position);
        } else {
            banners = bannerRepository.findAllByOrderBySortOrderAsc();
        }
        return banners.stream().map(this::toResponse).toList();
    }

    @Override
    public BannerResponse updateBanner(Integer id, BannerRequest request) {
        Banner banner = findBanner(id);
        applyRequest(banner, request);
        return toResponse(bannerRepository.save(banner));
    }

    @Override
    public void deleteBanner(Integer id) {
        bannerRepository.delete(findBanner(id));
    }

    private Banner findBanner(Integer id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner not found with id: " + id));
    }

    private void applyRequest(Banner banner, BannerRequest request) {
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkType(request.getLinkType());
        banner.setLinkValue(request.getLinkValue());
        banner.setPosition(request.getPosition());
        banner.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        banner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
    }

    private BannerResponse toResponse(Banner banner) {
        return new BannerResponse(
                banner.getBannerId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkType(),
                banner.getLinkValue(),
                banner.getPosition(),
                banner.getSortOrder(),
                banner.getIsActive(),
                banner.getStartDate(),
                banner.getEndDate(),
                banner.getCreatedAt()
        );
    }
}
