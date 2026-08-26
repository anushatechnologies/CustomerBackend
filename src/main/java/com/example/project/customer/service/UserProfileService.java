package com.example.project.customer.service;

import com.example.project.customer.dto.UserProfileRequest;
import com.example.project.customer.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getProfile(Integer userId);
    UserProfileResponse updateProfile(Integer userId, UserProfileRequest request);
}
