package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.UserProfileRequest;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserContextUtil userContextUtil;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Integer userId = userContextUtil.getCurrentUserId();
        UserProfileResponse profile = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestBody UserProfileRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        UserProfileResponse updated = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", updated));
    }
}
