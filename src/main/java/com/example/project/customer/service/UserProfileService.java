package com.example.project.customer.service;

import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;

public interface UserProfileService {
    UserProfileResponse getProfile(Integer userId);
    UserProfileResponse updateProfile(Integer userId, UserProfileUpdateRequest request);
}
