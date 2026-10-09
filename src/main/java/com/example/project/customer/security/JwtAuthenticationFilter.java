package com.example.project.customer.security;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.repository.AdminUserRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.service.JwtService;
import com.example.project.customer.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final AdminUserRepository adminUserRepository;

    @Value("${app.security.admin-emails:admin@hinchmart.com,admin@example.com}")
    private String configuredAdminEmails = "admin@hinchmart.com";

    public JwtAuthenticationFilter(
            @Autowired(required = false) JwtService jwtService,
            @Autowired(required = false) CustomerRepository customerRepository,
            @Autowired(required = false) UserService userService,
            @Autowired(required = false) AdminUserRepository adminUserRepository
    ) {
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.userService = userService;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip JWT inspection on /api/auth/sync where initial Firebase ID Token is exchanged
        return "/api/auth/sync".equals(path) || "/api/auth/check-phone".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (jwtService == null || customerRepository == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();

            if (!token.isEmpty()) {
                if (jwtService != null && jwtService.validateToken(token)) {
                    try {
                        Claims claims = jwtService.extractClaims(token);
                        Integer customerId = jwtService.extractCustomerId(token);

                        if (customerId != null) {
                            Optional<Customer> customerOpt = customerRepository.findById(customerId);
                            if (customerOpt.isPresent()) {
                                Customer customer = customerOpt.get();

                                Integer sellerId = (userService != null) ? userService.resolveSellerIdForUser(customer) : null;
                                Role effectiveRole = resolveEffectiveRole(claims, customer, sellerId);

                                FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                                        customer.getCustomerId(),
                                        customer.getFirebaseUid(),
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
                                log.debug("Authenticated customerId: {}, role: {} via HinchMart JWT for URI: {}",
                                        customerId, effectiveRole, request.getRequestURI());
                            } else {
                                log.warn("Customer ID {} from JWT not found in database", customerId);
                                SecurityContextHolder.clearContext();
                            }
                        } else {
                            log.warn("Unable to extract customerId from JWT claims for URI: {}", request.getRequestURI());
                            SecurityContextHolder.clearContext();
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to authenticate with JWT for URI {}: {}", request.getRequestURI(), ex.getMessage());
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    // Invalid, expired, or tampered token (including Firebase ID token sent to protected API)
                    log.debug("Token validation failed for URI: {}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    Role resolveEffectiveRole(Map<String, Object> claims, Customer customer, Integer sellerId) {
        // 1. Claims check for ADMIN
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

        // 2. Database Admin table or Customer role is authoritative for ADMIN
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

        // 3. Configured Admin Email check
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            String checkEmail = customer.getEmail().trim().toLowerCase();
            if (isAdminEmail(checkEmail)) {
                return Role.ADMIN;
            }
        }

        // 4. Claims for SELLER / CUSTOMER
        if (claims != null && claims.containsKey("role")) {
            Object roleObj = claims.get("role");
            if (roleObj != null) {
                Role parsedRole = Role.fromString(roleObj.toString());
                if (parsedRole != null) {
                    return parsedRole;
                }
            }
        }

        // 5. Database Customer role for SELLER
        if (customer != null && customer.getRole() != null && !customer.getRole().isBlank()) {
            Role dbRole = customer.getRoleEnum();
            if (dbRole == Role.SELLER) {
                return Role.SELLER;
            }
        }

        // 6. Linked seller profile
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
        if (clean.equals("admin@hinchmart.com")) {
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
