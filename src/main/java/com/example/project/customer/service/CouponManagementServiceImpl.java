package com.example.project.customer.service;

import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponStatus;
import com.example.project.customer.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponManagementServiceImpl implements CouponManagementService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        log.info("Creating coupon with code: {}", request.getCode());

        // Check if coupon code already exists (case-insensitive)
        Optional<Coupon> existing = couponRepository.findByCodeIgnoreCase(request.getCode());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue())
                .applicableVehicleTypes(request.getApplicableVehicleTypes())
                .applicableUserSegment(request.getApplicableUserSegment())
                .specificUserIds(request.getSpecificUserIds())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .usageLimitTotal(request.getUsageLimitTotal())
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .createdBy(request.getCreatedBy())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created successfully with ID: {}", savedCoupon.getCouponId());

        return mapToResponse(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Integer couponId, CouponRequest request) {
        log.info("Updating coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));

        // Check if new code conflicts with existing (case-insensitive)
        if (!coupon.getCode().equalsIgnoreCase(request.getCode())) {
            Optional<Coupon> existing = couponRepository.findByCodeIgnoreCase(request.getCode());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
            }
        }

        // Update fields
        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setApplicableVehicleTypes(request.getApplicableVehicleTypes());
        coupon.setApplicableUserSegment(request.getApplicableUserSegment());
        coupon.setSpecificUserIds(request.getSpecificUserIds());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setUsageLimitTotal(request.getUsageLimitTotal());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());

        Coupon updatedCoupon = couponRepository.save(coupon);
        log.info("Coupon updated successfully: {}", couponId);

        return mapToResponse(updatedCoupon);
    }

    @Override
    public Optional<CouponResponse> getCouponById(Integer couponId) {
        log.debug("Fetching coupon by ID: {}", couponId);
        return couponRepository.findById(couponId).map(this::mapToResponse);
    }

    @Override
    public Optional<CouponResponse> getCouponByCode(String code) {
        log.debug("Fetching coupon by code: {}", code);
        return couponRepository.findByCodeIgnoreCase(code).map(this::mapToResponse);
    }

    @Override
    public List<CouponResponse> listAllCoupons() {
        log.debug("Fetching all coupons");
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CouponResponse activateCoupon(Integer couponId) {
        log.info("Activating coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));

        coupon.setStatus(CouponStatus.ACTIVE);
        Coupon updated = couponRepository.save(coupon);

        log.info("Coupon activated: {}", couponId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CouponResponse deactivateCoupon(Integer couponId) {
        log.info("Deactivating coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));

        coupon.setStatus(CouponStatus.INACTIVE);
        Coupon updated = couponRepository.save(coupon);

        log.info("Coupon deactivated: {}", couponId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCoupon(Integer couponId) {
        log.info("Deleting coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));

        couponRepository.delete(coupon);
        log.info("Coupon deleted: {}", couponId);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderValue(coupon.getMinOrderValue())
                .applicableVehicleTypes(coupon.getApplicableVehicleTypes())
                .applicableUserSegment(coupon.getApplicableUserSegment())
                .specificUserIds(coupon.getSpecificUserIds())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .usageLimitTotal(coupon.getUsageLimitTotal())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .currentUsageCount(coupon.getCurrentUsageCount())
                .status(coupon.getStatus())
                .createdBy(coupon.getCreatedBy())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
