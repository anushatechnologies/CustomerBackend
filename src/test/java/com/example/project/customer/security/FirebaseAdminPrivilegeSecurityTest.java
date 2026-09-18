package com.example.project.customer.security;

import com.example.project.customer.controller.AuthController;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.AdminUserRepository;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseAdminPrivilegeSecurityTest {

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private UserService userService;

    @Mock
    private AdminUserRepository adminUserRepository;

    private FirebaseAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new FirebaseAuthenticationFilter(firebaseAuthService, userService, adminUserRepository);
        ReflectionTestUtils.setField(filter, "configuredAdminEmails", "superadmin@hinchmart.com,admin@hinchmart.com");
    }

    @Test
    @DisplayName("Security: Email containing 'admin' (admin_buyer@gmail.com) MUST NOT receive ADMIN role")
    void customerWithAdminInEmail_DoesNotGetAdminRole() {
        Customer customer = Customer.builder()
                .customerId(101)
                .email("admin_buyer@gmail.com")
                .role("CUSTOMER")
                .build();

        Role role = filter.resolveEffectiveRole(Collections.emptyMap(), customer, null);

        assertNotEquals(Role.ADMIN, role, "Email containing 'admin' must never be granted ADMIN role");
        assertEquals(Role.CUSTOMER, role, "Customer with admin in email should remain CUSTOMER");
    }

    @Test
    @DisplayName("Security: Normal customer remains CUSTOMER")
    void normalCustomer_RemainsCustomer() {
        Customer customer = Customer.builder()
                .customerId(102)
                .email("john.doe@example.com")
                .role("CUSTOMER")
                .build();

        Role role = filter.resolveEffectiveRole(Collections.emptyMap(), customer, null);
        assertEquals(Role.CUSTOMER, role);
    }

    @Test
    @DisplayName("Security: Seller user with linked seller profile remains SELLER")
    void seller_RemainsSeller() {
        Customer customer = Customer.builder()
                .customerId(103)
                .email("vendor_admin@supplies.com")
                .role("SELLER")
                .build();

        Role role = filter.resolveEffectiveRole(Collections.emptyMap(), customer, 55);
        assertEquals(Role.SELLER, role);
    }

    @Test
    @DisplayName("Security: Legitimate admin with exact configured email receives ADMIN role")
    void legitimateAdmin_ByEmail_ReceivesAdmin() {
        Customer customer = Customer.builder()
                .customerId(1)
                .email("admin@hinchmart.com")
                .build();

        Role role = filter.resolveEffectiveRole(Collections.emptyMap(), customer, null);
        assertEquals(Role.ADMIN, role);
    }

    @Test
    @DisplayName("Security: Legitimate admin with Firebase custom claims receives ADMIN role")
    void legitimateAdmin_ByCustomClaim_ReceivesAdmin() {
        Customer customer = Customer.builder()
                .customerId(2)
                .email("operations@company.com")
                .build();

        Map<String, Object> claims = Map.of("admin", true);

        Role role = filter.resolveEffectiveRole(claims, customer, null);
        assertEquals(Role.ADMIN, role);
    }

    @Test
    @DisplayName("Security: Unauthenticated request sets no security context and cannot receive ADMIN")
    void unauthenticatedRequest_NoAdminPrivilege() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Unauthenticated request must not populate SecurityContext");
        assertFalse(SecurityUtils.isAdmin(), "Unauthenticated user cannot be admin");
    }

    @Test
    @DisplayName("Security: AuthController.claimAdminRole rejects arbitrary email containing admin")
    void claimAdminRole_RejectsArbitraryAdminEmail() throws Exception {
        AuthController authController = new AuthController(userService, firebaseAuthService, adminUserRepository);
        ReflectionTestUtils.setField(authController, "configuredAdminEmails", "admin@hinchmart.com");

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("buyer-uid-123");
        when(mockToken.getEmail()).thenReturn("admin_buyer@gmail.com");
        when(mockToken.getName()).thenReturn("Admin Buyer");
        when(firebaseAuthService.verifyIdToken("fake-token")).thenReturn(mockToken);

        assertThrows(ForbiddenException.class, () -> authController.claimAdminRole("Bearer fake-token"),
                "claimAdminRole must reject unauthorized email even if it contains 'admin'");
    }
}
