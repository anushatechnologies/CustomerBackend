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

    public FirebaseAuthenticationFilter(
            @Autowired(required = false) FirebaseAuthService firebaseAuthService,
            @Autowired(required = false) UserService userService
    ) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
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

                    // Determine Role: Priority: 1. Firebase Custom Claims -> 2. Database Role
                    Role effectiveRole = resolveEffectiveRole(claims, customer);

                    // Resolve internal sellerId if applicable
                    Integer sellerId = userService.resolveSellerIdForUser(customer);

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

    private Role resolveEffectiveRole(Map<String, Object> claims, Customer customer) {
        if (claims != null && claims.containsKey("role")) {
            Object roleObj = claims.get("role");
            if (roleObj != null) {
                return Role.fromString(roleObj.toString());
            }
        }
        if (claims != null && Boolean.TRUE.equals(claims.get("admin"))) {
            return Role.ADMIN;
        }
        if (customer != null) {
            return customer.getRoleEnum();
        }
        return Role.CUSTOMER;
    }
}
