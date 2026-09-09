package com.example.project.customer.config;

import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SellerContextUtil {

    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;

    public SellerContextUtil(
            @org.springframework.beans.factory.annotation.Autowired(required = false) SellerRepository sellerRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) StoreRepository storeRepository
    ) {
        this.sellerRepository = sellerRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * Resolves the current seller ID strictly from the authenticated SecurityContext.
     * Header-based or request-parameter fallbacks are completely eliminated for security.
     */
    public Integer getCurrentSellerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required: No valid authenticated principal in SecurityContext.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof FirebaseUserPrincipal fup) {
            if (fup.getSellerId() != null) {
                return fup.getSellerId();
            }

            // Fallback database lookup via authenticated user email
            if (fup.getEmail() != null) {
                Optional<Seller> seller = sellerRepository.findFirstByEmailIgnoreCase(fup.getEmail());
                if (seller.isPresent()) {
                    return seller.get().getSellerId();
                }
            }
        }

        throw new ForbiddenException("Access Denied: The authenticated account is not registered as a seller.");
    }

    /**
     * Resolves the current Store associated with the authenticated seller.
     */
    public Store getCurrentStore() {
        Integer sellerId = getCurrentSellerId();
        return storeRepository.findBySeller_SellerId(sellerId)
                .orElseThrow(() -> new ForbiddenException("Access Denied: No active store found for seller ID: " + sellerId));
    }

    /**
     * Resolves the current Store ID associated with the authenticated seller.
     */
    public Integer getCurrentStoreId() {
        return getCurrentStore().getStoreId();
    }

    public String getCurrentSellerIdString() {
        Integer id = getCurrentSellerId();
        return "seller_" + id;
    }
}
