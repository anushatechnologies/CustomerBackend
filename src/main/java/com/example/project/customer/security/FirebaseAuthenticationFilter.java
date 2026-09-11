package com.example.project.customer.security;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;
    private final com.example.project.customer.repository.AdminUserRepository adminUserRepository;

    public FirebaseAuthenticationFilter(
            @Autowired(required = false) FirebaseAuthService firebaseAuthService,
            @Autowired(required = false) UserService userService,
            @Autowired(required = false) com.example.project.customer.repository.AdminUserRepository adminUserRepository
    ) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (firebaseAuthService == null || userService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();

            if (!token.isEmpty()) {
                try {
                    FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
                    String firebaseUid = decodedToken.getUid();
                    String email = decodedToken.getEmail();
                    String name = decodedToken.getName();
                    Map<String, Object> claims = decodedToken.getClaims();
                    String phone = (claims != null && claims.get("phone_number") != null) 
                            ? String.valueOf(claims.get("phone_number")) 
                            : null;

                    // Synchronize or load internal customer from MySQL database
                    Customer customer = userService.syncUserWithFirebase(firebaseUid, email, name, phone);

                    // Resolve internal sellerId if applicable
                    Integer sellerId = userService.resolveSellerIdForUser(customer);

                    // Determine Role: Priority: 1. Firebase Custom Claims -> 2. Database Role -> 3. Seller Profile
                    Role effectiveRole = resolveEffectiveRole(claims, customer, sellerId);

                    // Build authenticated principal
                    FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                            customer.getCustomerId(),
                            firebaseUid,
                            customer.getEmail(),
                            customer.getName(),
                            effectiveRole,
                            sellerId,
                            customer.isActive(),
                            claims
                    );

                    FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                            principal,
                            token,
                            principal.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user {} (ID: {}, Role: {}) for URI: {}",
                            email, customer.getCustomerId(), effectiveRole, request.getRequestURI());

                } catch (Exception ex) {
                    log.warn("Authentication failed for request {} : {}", request.getRequestURI(), ex.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @org.springframework.beans.factory.annotation.Value("${app.security.admin-emails:admin@hinchmart.com,admin@example.com}")
    private String configuredAdminEmails = "admin@hinchmart.com";

    private Role resolveEffectiveRole(Map<String, Object> claims, Customer customer, Integer sellerId) {
        // 1. Firebase Custom Claims take highest precedence
        if (claims != null && claims.containsKey("role")) {
            Object roleObj = claims.get("role");
            if (roleObj != null) {
                Role parsedRole = Role.fromString(roleObj.toString());
                if (parsedRole == Role.ADMIN) {
                    return Role.ADMIN;
                }
            }
        }
        if (claims != null && (Boolean.TRUE.equals(claims.get("admin")) || "ADMIN".equalsIgnoreCase(String.valueOf(claims.get("role"))))) {
            return Role.ADMIN;
        }

        // 2. MySQL database Admin table or Customer role is authoritative for ADMIN
        if (adminUserRepository != null && customer != null) {
            if (customer.getEmail() != null && adminUserRepository.findByEmailIgnoreCase(customer.getEmail().trim()).isPresent()) {
                return Role.ADMIN;
            }
            if (customer.getFirebaseUid() != null && adminUserRepository.findByFirebaseUid(customer.getFirebaseUid()).isPresent()) {
                return Role.ADMIN;
            }
        }
        if (customer != null && customer.getRole() != null && !customer.getRole().isBlank()) {
            if ("ADMIN".equalsIgnoreCase(customer.getRole()) || customer.getRoleEnum() == Role.ADMIN) {
                return Role.ADMIN;
            }
        }

        // 3. Configured Admin Email check or email containing 'admin'
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            String checkEmail = customer.getEmail().trim().toLowerCase();
            if (isAdminEmail(checkEmail)) {
                return Role.ADMIN;
            }
        }

        // 4. Firebase Custom Claims for SELLER / CUSTOMER
        if (claims != null && claims.containsKey("role")) {
            Object roleObj = claims.get("role");
            if (roleObj != null) {
                return Role.fromString(roleObj.toString());
            }
        }

        // 5. MySQL database Customer role for SELLER
        if (customer != null && customer.getRole() != null && !customer.getRole().isBlank()) {
            Role dbRole = customer.getRoleEnum();
            if (dbRole == Role.SELLER) {
                return Role.SELLER;
            }
        }

        // 6. Linked seller profile (only for non-admin accounts)
        if (sellerId != null) {
            return Role.SELLER;
        }

        // 7. Default customer role
        if (customer != null) {
            return customer.getRoleEnum();
        }
        return Role.CUSTOMER;
    }

    private boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String clean = email.trim().toLowerCase();
        if (clean.equals("admin@hinchmart.com") || clean.contains("admin")) {
            return true;
        }
        if (configuredAdminEmails != null && !configuredAdminEmails.isBlank()) {
            String[] admins = configuredAdminEmails.split(",");
            for (String adm : admins) {
                if (clean.equalsIgnoreCase(adm.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
