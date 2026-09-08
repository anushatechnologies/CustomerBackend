package com.example.project.customer.service;

import com.example.project.customer.entity.Role;
import com.example.project.customer.exception.UnauthorizedException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    @Autowired
    public FirebaseAuthServiceImpl(@Autowired(required = false) FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public FirebaseToken verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Firebase ID token cannot be blank");
        }

        if (firebaseAuth == null) {
            log.error("FirebaseAuth is not initialized. Check your Firebase credentials configuration.");
            throw new UnauthorizedException("Authentication service is temporarily unavailable.");
        }

        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Failed to verify Firebase ID token: AuthErrorCode={}, Message={}", e.getAuthErrorCode(), e.getMessage());
            throw new UnauthorizedException("Invalid, expired, or revoked authentication token");
        } catch (Exception e) {
            log.error("Unexpected error during token verification: {}", e.getMessage(), e);
            throw new UnauthorizedException("Unable to process authentication token");
        }
    }

    @Override
    public void setUserRoleClaim(String firebaseUid, Role role) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID cannot be empty");
        }
        Role targetRole = role != null ? role : Role.CUSTOMER;
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", targetRole.name());
        setCustomUserClaims(firebaseUid, claims);
    }

    @Override
    public void setCustomUserClaims(String firebaseUid, Map<String, Object> claims) {
        if (firebaseAuth == null) {
            log.warn("FirebaseAuth not initialized, skipping custom claims update for UID: {}", firebaseUid);
            return;
        }
        try {
            firebaseAuth.setCustomUserClaims(firebaseUid, claims);
            log.info("Successfully updated Firebase custom claims for UID: {} -> {}", firebaseUid, claims);
        } catch (FirebaseAuthException e) {
            log.error("Failed to set Firebase custom claims for UID: {}: {}", firebaseUid, e.getMessage(), e);
            throw new RuntimeException("Failed to update Firebase user claims: " + e.getMessage(), e);
        }
    }

    @Override
    public UserRecord getFirebaseUser(String firebaseUid) {
        if (firebaseAuth == null) {
            return null;
        }
        try {
            return firebaseAuth.getUser(firebaseUid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to retrieve Firebase user record for UID: {}: {}", firebaseUid, e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteFirebaseUser(String firebaseUid) {
        if (firebaseAuth == null) return;
        try {
            firebaseAuth.deleteUser(firebaseUid);
            log.info("Successfully deleted Firebase user UID: {}", firebaseUid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to delete Firebase user UID {}: {}", firebaseUid, e.getMessage());
        }
    }
}
