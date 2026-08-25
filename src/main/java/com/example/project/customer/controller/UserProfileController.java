package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(userProfileService.getProfile(uid));
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody UserProfileUpdateRequest request) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok("Profile updated successfully", userProfileService.updateProfile(uid, request));
    }
}
