package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.AuthSyncRequest;
import com.example.project.customer.dto.AuthUserResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Synchronizes a newly authenticated Firebase user with the internal MySQL database.
     * Called by frontend after Firebase login/register.
     */
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthUserResponse>> syncUser(@RequestBody(required = false) AuthSyncRequest request) {
        FirebaseUserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("Authenticated user principal not found"));

        String phone = (request != null && request.getPhone() != null) ? request.getPhone() : null;
        String name = (request != null && request.getName() != null) ? request.getName() : principal.getName();

        Customer customer = userService.syncUserWithFirebase(
                principal.getFirebaseUid(),
                principal.getEmail(),
                name,
                phone
        );

        Integer sellerId = userService.resolveSellerIdForUser(customer);

        AuthUserResponse response = AuthUserResponse.builder()
                .userId(customer.getCustomerId())
                .firebaseUid(customer.getFirebaseUid())
                .email(customer.getEmail())
                .name(customer.getName())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .sellerId(sellerId)
                .claims(principal.getClaims())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("User synchronized successfully", response));
    }

    /**
     * Retrieves the current authenticated user's profile and authority details.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser() {
        FirebaseUserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        Customer customer = userService.getCustomerById(principal.getInternalUserId());
        Integer sellerId = userService.resolveSellerIdForUser(customer);

        AuthUserResponse response = AuthUserResponse.builder()
                .userId(customer.getCustomerId())
                .firebaseUid(customer.getFirebaseUid())
                .email(customer.getEmail())
                .name(customer.getName())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .sellerId(sellerId)
                .claims(principal.getClaims())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Current user details retrieved", response));
    }
}
