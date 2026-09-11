package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.AuthSyncRequest;
import com.example.project.customer.dto.AuthUserResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final FirebaseAuthService firebaseAuthService;

    public AuthController(
            UserService userService,
            @Autowired(required = false) FirebaseAuthService firebaseAuthService
    ) {
        this.userService = userService;
        this.firebaseAuthService = firebaseAuthService;
    }

    /**
     * Synchronizes a newly authenticated Firebase user with the internal MySQL database.
     * Called by frontend after Firebase login/register. Accessible to all users with a valid token.
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<AuthUserResponse>> syncUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) AuthSyncRequest request
    ) {
        String firebaseUid = null;
        String email = null;
        String name = (request != null && request.getName() != null) ? request.getName() : null;
        String phone = (request != null && request.getPhone() != null) ? request.getPhone() : null;
        Map<String, Object> claims = null;

        Optional<FirebaseUserPrincipal> principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            FirebaseUserPrincipal principal = principalOpt.get();
            firebaseUid = principal.getFirebaseUid();
            email = principal.getEmail();
            if (name == null || name.isBlank()) {
                name = principal.getName();
            }
            claims = principal.getClaims();
        } else if (authHeader != null && authHeader.startsWith("Bearer ") && firebaseAuthService != null) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    com.google.firebase.auth.FirebaseToken decoded = firebaseAuthService.verifyIdToken(token);
                    firebaseUid = decoded.getUid();
                    email = decoded.getEmail();
                    if (name == null || name.isBlank()) {
                        name = decoded.getName();
                    }
                    claims = decoded.getClaims();
                    if (phone == null && claims != null && claims.get("phone_number") != null) {
                        phone = String.valueOf(claims.get("phone_number"));
                    }
                } catch (Exception ex) {
                    log.warn("Direct token verification in /api/auth/sync encountered: {}", ex.getMessage());
                }
            }
        }

        if (firebaseUid == null) {
            throw new com.example.project.customer.exception.UnauthorizedException(
                    "Authentication token is missing, invalid, or expired."
            );
        }

        Customer customer = userService.syncUserWithFirebase(
                firebaseUid,
                email,
                name,
                phone,
                request != null ? request.getRole() : null
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
                .claims(claims)
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
