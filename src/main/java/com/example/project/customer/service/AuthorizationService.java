package com.example.project.customer.service;

import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;

    /**
     * Checks if the currently authenticated user owns the specified sellerId, or is an ADMIN.
     */
    @Transactional(readOnly = true)
    public boolean isCurrentSeller(Integer sellerId) {
        if (sellerId == null) {
            return false;
        }

        // 1. ADMIN users can access any seller's data
        if (SecurityUtils.isAdmin()) {
            return true;
        }

        Optional<FirebaseUserPrincipal> principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isEmpty()) {
            log.warn("Ownership check failed: No authenticated principal in SecurityContext");
            return false;
        }

        FirebaseUserPrincipal principal = principalOpt.get();

        // 2. Direct match with principal's resolved sellerId
        if (principal.getSellerId() != null && principal.getSellerId().equals(sellerId)) {
            return true;
        }

        // 3. Fallback database lookup via email
        if (principal.getEmail() != null) {
            Optional<Seller> seller = sellerRepository.findById(sellerId);
            if (seller.isPresent() && principal.getEmail().equalsIgnoreCase(seller.get().getEmail())) {
                return true;
            }
        }

        log.warn("Ownership check failed: User {} (email: {}) is not authorized to access sellerId {}",
                principal.getInternalUserId(), principal.getEmail(), sellerId);
        return false;
    }

    /**
     * Checks if the currently authenticated user owns the specified storeId, or is an ADMIN.
     */
    @Transactional(readOnly = true)
    public boolean isCurrentStore(Integer storeId) {
        if (storeId == null) {
            return false;
        }

        if (SecurityUtils.isAdmin()) {
            return true;
        }

        Optional<Store> storeOpt = storeRepository.findById(storeId);
        if (storeOpt.isEmpty()) {
            return false;
        }

        Integer sellerId = storeOpt.get().getSeller().getSellerId();
        return isCurrentSeller(sellerId);
    }

    /**
     * Checks if the currently authenticated user matches the specified internal userId, or is an ADMIN.
     */
    public boolean isCurrentUser(Integer userId) {
        if (userId == null) {
            return false;
        }

        if (SecurityUtils.isAdmin()) {
            return true;
        }

        Optional<FirebaseUserPrincipal> principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isEmpty()) {
            return false;
        }

        FirebaseUserPrincipal principal = principalOpt.get();
        return principal.getInternalUserId() != null && principal.getInternalUserId().equals(userId);
    }

    /**
     * Programmatic assertion of seller ownership; throws ForbiddenException if unauthorized.
     */
    public void validateSellerOwnership(Integer sellerId) {
        if (!isCurrentSeller(sellerId)) {
            throw new ForbiddenException("Access Denied: You do not own the requested seller resource (sellerId=" + sellerId + ").");
        }
    }

    /**
     * Programmatic assertion of store ownership; throws ForbiddenException if unauthorized.
     */
    public void validateStoreOwnership(Integer storeId) {
        if (!isCurrentStore(storeId)) {
            throw new ForbiddenException("Access Denied: You do not have permission to manage store ID: " + storeId);
        }
    }

    /**
     * Programmatic assertion of user ownership; throws ForbiddenException if unauthorized.
     */
    public void validateUserOwnership(Integer userId) {
        if (!isCurrentUser(userId)) {
            throw new ForbiddenException("Access Denied: You do not have permission to access user resource (userId=" + userId + ").");
        }
    }
}
