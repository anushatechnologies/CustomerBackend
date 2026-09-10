package com.example.project.customer.config;

import com.example.project.customer.entity.OnboardingStatus;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
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
     * If an authenticated seller user does not yet have a record in MySQL sellers table,
     * auto-provisions a Seller record and Store seamlessly.
     */
    public Integer getCurrentSellerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required: No valid authenticated principal in SecurityContext. Please provide a valid Firebase Authorization token.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof FirebaseUserPrincipal fup) {
            if (fup.getSellerId() != null) {
                return fup.getSellerId();
            }

            // Fallback database lookup via authenticated user email
            if (fup.getEmail() != null && !fup.getEmail().isBlank()) {
                String cleanEmail = fup.getEmail().trim().toLowerCase();
                if (sellerRepository != null) {
                    Optional<Seller> sellerOpt = sellerRepository.findFirstByEmailIgnoreCase(cleanEmail);
                    if (sellerOpt.isPresent()) {
                        return sellerOpt.get().getSellerId();
                    }

                    // Auto-provision Seller profile and default Store for authenticated user
                    String uniquePhone = (fup.getClaims() != null && fup.getClaims().get("phone_number") != null)
                            ? String.valueOf(fup.getClaims().get("phone_number"))
                            : "9" + String.format("%09d", Math.abs(cleanEmail.hashCode() % 1000000000));

                    while (!sellerRepository.findAllByPhone(uniquePhone).isEmpty()) {
                        uniquePhone = "9" + String.format("%09d", Math.abs(ThreadLocalRandom.current().nextInt(100000000, 999999999)));
                    }

                    Seller newSeller = Seller.builder()
                            .name(fup.getName() != null && !fup.getName().isBlank() ? fup.getName() : "Seller")
                            .email(cleanEmail)
                            .phone(uniquePhone)
                            .onboardingStatus(OnboardingStatus.STEP_1)
                            .verificationStatus(VerificationStatus.PENDING)
                            .build();

                    Seller savedSeller = sellerRepository.save(newSeller);
                    autoCreateStoreForSeller(savedSeller);
                    log.info("Auto-provisioned new Seller (ID: {}) and Store for authenticated user: {}", savedSeller.getSellerId(), cleanEmail);
                    return savedSeller.getSellerId();
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
        if (storeRepository == null) {
            return null;
        }
        return storeRepository.findBySeller_SellerId(sellerId)
                .orElseGet(() -> {
                    if (sellerRepository != null) {
                        return sellerRepository.findById(sellerId)
                                .map(this::autoCreateStoreForSeller)
                                .orElseThrow(() -> new ForbiddenException("Access Denied: No active store found for seller ID: " + sellerId));
                    }
                    throw new ForbiddenException("Access Denied: No active store found for seller ID: " + sellerId);
                });
    }

    public Store autoCreateStoreForSeller(Seller seller) {
        if (seller == null || seller.getSellerId() == null || storeRepository == null) return null;
        Optional<Store> existing = storeRepository.findBySeller_SellerId(seller.getSellerId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String storeName = seller.getCompanyName() != null && !seller.getCompanyName().isBlank()
                ? seller.getCompanyName().trim()
                : (seller.getName() != null && !seller.getName().isBlank() ? seller.getName().trim() : "Seller " + seller.getSellerId());

        String baseSlug = storeName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (baseSlug.isBlank()) baseSlug = "store-" + seller.getSellerId();
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (storeRepository.existsBySlugIgnoreCase(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }

        Store store = Store.builder()
                .seller(seller)
                .name(storeName)
                .slug(uniqueSlug)
                .status(StoreStatus.ACTIVE)
                .commissionRate(BigDecimal.valueOf(5.00))
                .description("Official Store of " + storeName)
                .rating(4.8)
                .reviewCount(0)
                .serviceRadiusKm(50)
                .minOrderValue(BigDecimal.ZERO)
                .build();

        Store savedStore = storeRepository.save(store);
        log.info("Auto-created active marketplace Store #{} ('{}', slug: '{}') for Seller #{}",
                savedStore.getStoreId(), savedStore.getName(), savedStore.getSlug(), seller.getSellerId());
        return savedStore;
    }

    /**
     * Resolves the current Store ID associated with the authenticated seller.
     */
    public Integer getCurrentStoreId() {
        Store store = getCurrentStore();
        return store != null ? store.getStoreId() : 1;
    }

    public String getCurrentSellerIdString() {
        Integer id = getCurrentSellerId();
        return "seller_" + id;
    }
}
