package com.example.project.customer.security;

import com.example.project.customer.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<FirebaseUserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FirebaseUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Integer> getCurrentUserId() {
        return getCurrentUserPrincipal().map(FirebaseUserPrincipal::getInternalUserId);
    }

    public static Optional<String> getCurrentFirebaseUid() {
        return getCurrentUserPrincipal().map(FirebaseUserPrincipal::getFirebaseUid);
    }

    public static Optional<String> getCurrentEmail() {
        return getCurrentUserPrincipal().map(FirebaseUserPrincipal::getEmail);
    }

    public static Optional<Integer> getCurrentSellerId() {
        return getCurrentUserPrincipal().map(FirebaseUserPrincipal::getSellerId);
    }

    public static Optional<Role> getCurrentRole() {
        return getCurrentUserPrincipal().map(FirebaseUserPrincipal::getRole);
    }

    public static boolean hasRole(Role role) {
        if (role == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String expectedAuthority = role.getAuthority();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (expectedAuthority.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public static boolean isSeller() {
        return hasRole(Role.SELLER);
    }

    public static boolean isCustomer() {
        return hasRole(Role.CUSTOMER);
    }
}
