package com.example.project.customer.service;

import com.example.project.customer.dto.BannerRequest;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.exception.BannerNotFoundException;
import com.example.project.customer.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private BannerServiceImpl bannerService;

    private Banner banner;
    private BannerRequest bannerRequest;

    @BeforeEach
    void setUp() {
        banner = new Banner();
        banner.setBannerId(1);
        banner.setTitle("Mega Sale");
        banner.setImageUrl("https://example.com/banner.jpg");
        banner.setLinkType("PRODUCT");
        banner.setLinkValue("100");
        banner.setPosition("HEADER");
        banner.setSortOrder(1);
        banner.setActive(true);
        banner.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        banner.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59));
        banner.setCreatedAt(LocalDateTime.now());

        bannerRequest = new BannerRequest();
        bannerRequest.setTitle("Mega Sale");
        bannerRequest.setImageUrl("https://example.com/banner.jpg");
        bannerRequest.setLinkType("PRODUCT");
        bannerRequest.setLinkValue("100");
        bannerRequest.setPosition("HEADER");
        bannerRequest.setSortOrder(1);
        bannerRequest.setActive(true);
        bannerRequest.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        bannerRequest.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59));
    }

    @Test
    @DisplayName("createBanner should map all fields and save")
    void createBanner_Success() {
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        BannerResponse response = bannerService.createBanner(bannerRequest);

        assertNotNull(response);
        assertEquals(1, response.getBannerId());
        assertEquals("Mega Sale", response.getTitle());
        assertEquals("https://example.com/banner.jpg", response.getImageUrl());
        assertEquals("PRODUCT", response.getLinkType());
        assertEquals("100", response.getLinkValue());
        assertEquals("HEADER", response.getPosition());
        assertEquals(1, response.getSortOrder());
        assertTrue(response.getIsActive());
        assertNotNull(response.getStartDate());
        assertNotNull(response.getEndDate());
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    @DisplayName("getBannerById should return banner when found")
    void getBannerById_Success() {
        when(bannerRepository.findById(1)).thenReturn(Optional.of(banner));

        BannerResponse response = bannerService.getBannerById(1);

        assertNotNull(response);
        assertEquals(1, response.getBannerId());
        assertEquals("Mega Sale", response.getTitle());
    }

    @Test
    @DisplayName("getBannerById should throw BannerNotFoundException when not found")
    void getBannerById_NotFound() {
        when(bannerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BannerNotFoundException.class, () -> bannerService.getBannerById(99));
    }

    @Test
    @DisplayName("getAllBanners without filters should return all banners ordered by sort order")
    void getAllBanners_NoFilters() {
        when(bannerRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(banner));

        List<BannerResponse> results = bannerService.getAllBanners(null, null);

        assertEquals(1, results.size());
        assertEquals("Mega Sale", results.get(0).getTitle());
        verify(bannerRepository).findAllByOrderBySortOrderAsc();
    }

    @Test
    @DisplayName("getAllBanners with active=true and position should filter accordingly")
    void getAllBanners_WithActiveAndPosition() {
        when(bannerRepository.findByPositionAndActiveTrueOrderBySortOrderAsc("HEADER")).thenReturn(List.of(banner));

        List<BannerResponse> results = bannerService.getAllBanners(true, "HEADER");

        assertEquals(1, results.size());
        verify(bannerRepository).findByPositionAndActiveTrueOrderBySortOrderAsc("HEADER");
    }

    @Test
    @DisplayName("getAllBanners with active=true only should filter by active")
    void getAllBanners_WithActiveOnly() {
        when(bannerRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(banner));

        List<BannerResponse> results = bannerService.getAllBanners(true, null);

        assertEquals(1, results.size());
        verify(bannerRepository).findByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    @DisplayName("getAllBanners with position only should filter by position")
    void getAllBanners_WithPositionOnly() {
        when(bannerRepository.findByPositionOrderBySortOrderAsc("HEADER")).thenReturn(List.of(banner));

        List<BannerResponse> results = bannerService.getAllBanners(null, "HEADER");

        assertEquals(1, results.size());
        verify(bannerRepository).findByPositionOrderBySortOrderAsc("HEADER");
    }

    @Test
    @DisplayName("updateBanner should update existing banner")
    void updateBanner_Success() {
        when(bannerRepository.findById(1)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        bannerRequest.setTitle("New Flash Sale");
        bannerRequest.setActive(false);

        BannerResponse response = bannerService.updateBanner(1, bannerRequest);

        assertNotNull(response);
        verify(bannerRepository).save(banner);
    }

    @Test
    @DisplayName("updateBanner should throw BannerNotFoundException when banner does not exist")
    void updateBanner_NotFound() {
        when(bannerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BannerNotFoundException.class, () -> bannerService.updateBanner(99, bannerRequest));
    }

    @Test
    @DisplayName("uploadBannerImage should upload image to S3 and update banner imageUrl")
    void uploadBannerImage_Success() {
        when(bannerRepository.findById(1)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        MockMultipartFile file = new MockMultipartFile(
                "file", "new-banner.jpg", "image/jpeg", "bytes".getBytes()
        );
        ImageUploadResponse uploadResponse = ImageUploadResponse.builder()
                .imageKey("banners/new-uuid.jpg")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/banners/new-uuid.jpg")
                .fileName("new-banner.jpg")
                .mimeType("image/jpeg")
                .fileSize(5L)
                .build();

        when(s3ImageService.uploadImage(any(), any(ImageFolder.class))).thenReturn(uploadResponse);

        BannerResponse response = bannerService.uploadBannerImage(1, file);

        assertNotNull(response);
        verify(s3ImageService).uploadImage(file, ImageFolder.BANNERS);
        verify(bannerRepository).save(banner);
    }

    @Test
    @DisplayName("updateBanner should delete old S3 image when replaced")
    void updateBanner_ReplacesOldImage() {
        when(bannerRepository.findById(1)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        bannerRequest.setImageUrl("https://example.com/new-replaced-banner.jpg");

        bannerService.updateBanner(1, bannerRequest);

        verify(bannerRepository).save(banner);
        verify(s3ImageService).deleteImage("https://example.com/banner.jpg");
    }

    @Test
    @DisplayName("deleteBanner should delete banner and S3 image when exists")
    void deleteBanner_Success() {
        when(bannerRepository.findById(1)).thenReturn(Optional.of(banner));

        bannerService.deleteBanner(1);

        verify(bannerRepository).delete(banner);
        verify(s3ImageService).deleteImage("https://example.com/banner.jpg");
    }

    @Test
    @DisplayName("deleteBanner should throw BannerNotFoundException when not found")
    void deleteBanner_NotFound() {
        when(bannerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BannerNotFoundException.class, () -> bannerService.deleteBanner(99));
    }
}
