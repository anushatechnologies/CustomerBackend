package com.example.project.customer.service;

import com.example.project.customer.dto.BusinessDetailsDto;
import com.example.project.customer.dto.ProcurementStatsDto;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.entity.UserProfile;
import com.example.project.customer.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final OrderRepository orderRepository;
    private final RfqRepository rfqRepository;
    private final WishlistRepository wishlistRepository;
    private final AddressRepository addressRepository;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository,
                                  OrderRepository orderRepository,
                                  RfqRepository rfqRepository,
                                  WishlistRepository wishlistRepository,
                                  AddressRepository addressRepository) {
        this.userProfileRepository = userProfileRepository;
        this.orderRepository = orderRepository;
        this.rfqRepository = rfqRepository;
        this.wishlistRepository = wishlistRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Integer userId) {
        Integer uid = userId != null ? userId : 101;
        UserProfile profile = getOrCreateProfile(uid);
        return toResponse(profile);
    }

    @Override
    public UserProfileResponse updateProfile(Integer userId, UserProfileUpdateRequest request) {
        Integer uid = userId != null ? userId : 101;
        UserProfile profile = getOrCreateProfile(uid);

        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName());
        if (request.getGstNumber() != null) profile.setGstNumber(request.getGstNumber());
        if (request.getPanNumber() != null) profile.setPanNumber(request.getPanNumber());
        if (request.getBusinessType() != null) profile.setBusinessType(request.getBusinessType());

        return toResponse(userProfileRepository.save(profile));
    }

    private UserProfile getOrCreateProfile(Integer userId) {
        return userProfileRepository.findById(userId).orElseGet(() -> {
            UserProfile defaultProfile = new UserProfile(
                    userId,
                    "Rajesh Sharma",
                    "9876543210",
                    "rajesh@apexbldrs.com",
                    "BUYER",
                    "GOLD",
                    "Apex Infra Projects Pvt Ltd",
                    "36AAACT2727Q1ZW",
                    "AAACT2727Q",
                    "General Contractor",
                    true,
                    new BigDecimal("5000000.00"),
                    new BigDecimal("3250000.00")
            );
            return userProfileRepository.save(defaultProfile);
        });
    }

    private UserProfileResponse toResponse(UserProfile p) {
        long totalOrders = orderRepository.countByUserId(p.getId());
        long activeRfqs = rfqRepository.countByUserIdAndStatusIgnoreCase(p.getId(), "OPEN");
        long wishlistCount = wishlistRepository.countByUserId(p.getId());
        long addressCount = addressRepository.findByUserIdOrderByCreatedAtDesc(p.getId()).size();

        ProcurementStatsDto stats = new ProcurementStatsDto(
                totalOrders,
                activeRfqs,
                wishlistCount,
                addressCount
        );

        BusinessDetailsDto business = new BusinessDetailsDto(
                p.getCompanyName(),
                p.getGstNumber(),
                p.getPanNumber(),
                p.isGstVerified(),
                p.getCreditLimit(),
                p.getAvailableCredit()
        );

        return new UserProfileResponse(
                p.getId(),
                p.getFullName(),
                p.getPhone(),
                p.getEmail(),
                p.getRole(),
                p.getTier(),
                stats,
                business
        );
    }
}
