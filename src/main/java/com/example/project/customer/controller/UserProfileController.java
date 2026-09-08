package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final UserContextUtil userContextUtil;

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Integer currentUserId = userContextUtil.getCurrentUserId();
        UserProfileResponse profile = userService.getUserProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", profile));
    }

    /**
     * Updates the profile of the currently authenticated user.
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Integer currentUserId = userContextUtil.getCurrentUserId();
        UserProfileResponse updated = userService.updateUserProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", updated));
    }
}
