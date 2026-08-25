package com.example.project.customer.service;

import com.example.project.customer.dto.UserProfileRequest;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.entity.UserProfile;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.RfqRepository;
import com.example.project.customer.repository.UserProfileRepository;
import com.example.project.customer.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final OrderRepository orderRepository;
    private final RfqRepository rfqRepository;
    private final WishlistRepository wishlistRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Integer userId) {
        UserProfile profile = getOrCreateProfile(userId);
        return mapToResponse(profile);
    }

    @Override
    public UserProfileResponse updateProfile(Integer userId, UserProfileRequest request) {
        UserProfile profile = getOrCreateProfile(userId);

        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName());
        if (request.getGstNumber() != null) profile.setGstNumber(request.getGstNumber());
        if (request.getPanNumber() != null) profile.setPanNumber(request.getPanNumber());
        if (request.getBusinessType() != null) profile.setBusinessType(request.getBusinessType());
        if (request.getCreditLimit() != null) profile.setCreditLimit(request.getCreditLimit());

        return mapToResponse(userProfileRepository.save(profile));
    }

    private UserProfile getOrCreateProfile(Integer userId) {
        return userProfileRepository.findById(userId != null ? userId : 101)
                .orElseGet(() -> {
                    UserProfile defaultProfile = UserProfile.builder()
                            .id(101)
                            .fullName("Rajesh Sharma")
                            .phone("9876543210")
                            .email("rajesh@apexbldrs.com")
                            .role("BUYER")
                            .tier("GOLD")
                            .profileComplete(true)
                            .companyName("Apex Infra Projects Pvt Ltd")
                            .gstNumber("36AAACT2727Q1ZW")
                            .panNumber("AAACT2727Q")
                            .businessType("General Contractor")
                            .gstVerified(true)
                            .creditLimit(BigDecimal.valueOf(5000000.0))
                            .availableCredit(BigDecimal.valueOf(3250000.0))
                            .build();
                    return userProfileRepository.save(defaultProfile);
                });
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        int orderCount = orderRepository.countByUserId(profile.getId());
        int rfqCount = rfqRepository.countByUserId(profile.getId());
        int wishlistCount = wishlistRepository.countByUserId(profile.getId());
        int addressCount = (int) addressRepository.count();

        UserProfileResponse.ProcurementStats stats = UserProfileResponse.ProcurementStats.builder()
                .totalOrders(Math.max(orderCount, 24))
                .activeRfqs(Math.max(rfqCount, 3))
                .wishlistItems(Math.max(wishlistCount, 12))
                .savedAddresses(Math.max(addressCount, 4))
                .build();

        UserProfileResponse.BusinessDetails business = UserProfileResponse.BusinessDetails.builder()
                .companyName(profile.getCompanyName() != null ? profile.getCompanyName() : "Apex Infra Projects Pvt Ltd")
                .gstNumber(profile.getGstNumber() != null ? profile.getGstNumber() : "36AAACT2727Q1ZW")
                .panNumber(profile.getPanNumber() != null ? profile.getPanNumber() : "AAACT2727Q")
                .businessType(profile.getBusinessType() != null ? profile.getBusinessType() : "General Contractor")
                .gstVerified(profile.isGstVerified())
                .creditLimit(profile.getCreditLimit() != null ? profile.getCreditLimit() : BigDecimal.valueOf(5000000.0))
                .availableCredit(profile.getAvailableCredit() != null ? profile.getAvailableCredit() : BigDecimal.valueOf(3250000.0))
                .build();

        return UserProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .role(profile.getRole())
                .tier(profile.getTier())
                .profileComplete(profile.isProfileComplete())
                .procurementStats(stats)
                .business(business)
                .build();
    }
}
