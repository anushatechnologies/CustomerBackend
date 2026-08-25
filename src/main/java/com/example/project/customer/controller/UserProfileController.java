package com.example.project.customer.controller;

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

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse profile = userProfileService.getProfile(101);
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestBody UserProfileRequest request) {
        UserProfileResponse updated = userProfileService.updateProfile(101, request);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", updated));
    }
}
