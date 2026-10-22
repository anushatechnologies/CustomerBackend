package com.example.project.customer.config;

import com.example.project.customer.entity.OnboardingStatus;
import com.example.project.customer.entity.Role;
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
    private final com.example.project.customer.repository.CustomerRepository customerRepository;

    public SellerContextUtil() {
        this(null, null, null);
    }

    public SellerContextUtil(
            SellerRepository sellerRepository,
            StoreRepository storeRepository
    ) {
        this(sellerRepository, storeRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SellerContextUtil(
            @org.springframework.beans.factory.annotation.Autowired(required = false) SellerRepository sellerRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) StoreRepository storeRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.project.customer.repository.CustomerRepository customerRepository
    ) {
        this.sellerRepository = sellerRepository;
        this.storeRepository = storeRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Resolves the current seller ID strictly from the authenticated SecurityContext.
     * If an authenticated seller user does not yet have a record in MySQL sellers table,
     * auto-provisions a Seller record and Store seamlessly.
     */
    public Integer getCurrentSellerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required: No valid authenticated principal in SecurityContext. Please provide a valid Authorization token.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof FirebaseUserPrincipal fup) {
            // Check if user is an ADMIN
            if (fup.getRole() == Role.ADMIN) {
                throw new ForbiddenException("Access Denied: Admin accounts do not have an associated seller profile. Please use an authorized seller account.");
            }

            if (fup.getSellerId() != null) {
                if (sellerRepository != null) {
                    Optional<Seller> sellerOpt = sellerRepository.findById(fup.getSellerId());
                    if (sellerOpt.isPresent() && Boolean.TRUE.equals(sellerOpt.get().getIsDeleted())) {
                        throw new ForbiddenException("Access Denied: This seller account has been deactivated / deleted.");
                    }
                }
                return fup.getSellerId();
            }

            // Fallback database lookup via authenticated user email
            if (fup.getEmail() != null && !fup.getEmail().isBlank()) {
                String cleanEmail = fup.getEmail().trim().toLowerCase();
                if (sellerRepository != null) {
                    Optional<Seller> sellerOpt = sellerRepository.findFirstByEmailIgnoreCase(cleanEmail);
                    if (sellerOpt.isPresent()) {
                        if (Boolean.TRUE.equals(sellerOpt.get().getIsDeleted())) {
                            throw new ForbiddenException("Access Denied: This seller account has been deactivated / deleted.");
                        }
                        return sellerOpt.get().getSellerId();
                    }
                }
            }

            // Fallback database lookup via Customer phone / internalUserId
            com.example.project.customer.entity.Customer linkedCustomer = null;
            if (customerRepository != null) {
                if (fup.getInternalUserId() != null) {
                    linkedCustomer = customerRepository.findById(fup.getInternalUserId()).orElse(null);
                }
                if (linkedCustomer == null && fup.getFirebaseUid() != null) {
                    linkedCustomer = customerRepository.findByFirebaseUid(fup.getFirebaseUid()).orElse(null);
                }
            }

            if (linkedCustomer != null && linkedCustomer.getPhone() != null && !linkedCustomer.getPhone().isBlank() && sellerRepository != null) {
                String cleanPhone = linkedCustomer.getPhone().trim();
                java.util.List<Seller> byPhone = sellerRepository.findAllByPhone(cleanPhone);
                if (!byPhone.isEmpty()) {
                    Seller seller = byPhone.get(0);
                    if (Boolean.TRUE.equals(seller.getIsDeleted())) {
                        throw new ForbiddenException("Access Denied: This seller account has been deactivated / deleted.");
                    }
                    return seller.getSellerId();
                }
            }

            // Auto-provision seller profile if authenticated user is interacting with seller portal
            if (sellerRepository != null) {
                return autoProvisionSellerForPrincipal(fup, linkedCustomer).getSellerId();
            }
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            String username = ud.getUsername();
            if (sellerRepository != null) {
                Optional<Seller> s = sellerRepository.findFirstByEmailIgnoreCase(username);
                if (s.isPresent()) {
                    return s.get().getSellerId();
                }
            }
        } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
            if (sellerRepository != null) {
                Optional<Seller> s = sellerRepository.findFirstByEmailIgnoreCase(str);
                if (s.isPresent()) {
                    return s.get().getSellerId();
                }
            }
        }

        throw new ForbiddenException("Access Denied: The authenticated account is not registered as a seller. Please complete seller onboarding first.");
    }

    public Seller autoProvisionSellerForPrincipal(FirebaseUserPrincipal fup, com.example.project.customer.entity.Customer customer) {
        String name = (fup.getName() != null && !fup.getName().isBlank())
                ? fup.getName().trim()
                : (customer != null && customer.getName() != null && !customer.getName().isBlank() ? customer.getName().trim() : "Seller");

        String email = (fup.getEmail() != null && !fup.getEmail().isBlank())
                ? fup.getEmail().trim().toLowerCase()
                : (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()
                    ? customer.getEmail().trim().toLowerCase()
                    : "seller_" + (fup.getFirebaseUid() != null ? fup.getFirebaseUid() : System.currentTimeMillis()) + "@seller.hinchmart.com");

        String phone = (customer != null && customer.getPhone() != null && !customer.getPhone().isBlank())
                ? customer.getPhone().trim()
                : "+91" + (9000000000L + ThreadLocalRandom.current().nextLong(999999999L));

        Optional<Seller> byEmail = sellerRepository.findFirstByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }
        java.util.List<Seller> byPhone = sellerRepository.findAllByPhone(phone);
        if (!byPhone.isEmpty()) {
            return byPhone.get(0);
        }

        Seller seller = Seller.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .companyName(name + " Store")
                .onboardingStatus(OnboardingStatus.STEP_1)
                .verificationStatus(VerificationStatus.VERIFIED)
                .isDeleted(false)
                .build();

        Seller savedSeller = sellerRepository.save(seller);
        log.info("Auto-provisioned new Seller record #{} ({}, {}) for authenticated principal",
                savedSeller.getSellerId(), savedSeller.getEmail(), savedSeller.getPhone());

        if (customer != null && customerRepository != null && !Role.ADMIN.name().equalsIgnoreCase(customer.getRole())) {
            customer.setRole(Role.SELLER.name());
            customerRepository.save(customer);
        }

        autoCreateStoreForSeller(savedSeller);
        return savedSeller;
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
                .commissionRate(BigDecimal.ZERO)
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
