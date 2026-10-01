package com.example.project.customer.service;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.controller.AuthController;
import com.example.project.customer.controller.UserProfileController;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.AuthSyncRequest;
import com.example.project.customer.dto.AuthUserResponse;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.exception.CustomerConflictException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.security.FirebaseAuthenticationToken;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerAuthenticationArchitectureTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private UserContextUtil userContextUtil;

    private UserServiceImpl userService;
    private AuthController authController;
    private UserProfileController userProfileController;
    private MockMvc userProfileMockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(customerRepository, sellerRepository);
        authController = new AuthController(userService, firebaseAuthService, null);
        userProfileController = new UserProfileController(userService, userContextUtil);

        userProfileMockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // TEST 1: New Firebase UID + new phone -> creates exactly one customer
    // =========================================================================
    @Test
    @DisplayName("TEST 1: New Firebase UID + new phone -> creates exactly one customer with fallback credentials")
    void test1_newFirebaseUidAndPhone_createsCustomer() {
        String uid = "new-firebase-uid-101";
        String phone = "+919951949353";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findMatchingCustomersByPhone(eq(phone), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        when(customerRepository.save(customerCaptor.capture())).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(140);
            return c;
        });

        Customer result = userService.syncUserWithFirebase(uid, null, null, phone);

        assertNotNull(result);
        assertEquals(140, result.getCustomerId());
        assertEquals(uid, result.getFirebaseUid());
        assertNull(result.getName());
        assertNull(result.getEmail());
        assertEquals(phone, result.getPhone());
        assertEquals(Role.CUSTOMER.name(), result.getRole());
        assertTrue(result.isActive());

        verify(customerRepository).save(any(Customer.class));
    }

    // =========================================================================
    // TEST 2: Existing Firebase UID -> returns existing customer
    // =========================================================================
    @Test
    @DisplayName("TEST 2: Existing Firebase UID -> returns existing customer without creating duplicate")
    void test2_existingFirebaseUid_returnsExistingCustomer() {
        String uid = "existing-uid-202";
        Customer existing = Customer.builder()
                .customerId(140)
                .firebaseUid(uid)
                .name("Rishi Kottala")
                .email("rishi@example.com")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existing));

        Customer result = userService.syncUserWithFirebase(uid, null, null, "+919951949353");

        assertNotNull(result);
        assertEquals(140, result.getCustomerId());
        assertEquals("Rishi Kottala", result.getName());
        assertEquals("rishi@example.com", result.getEmail());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    // =========================================================================
    // TEST 3: New Firebase UID + existing unique phone -> finds existing customer -> no duplicate
    // =========================================================================
    @Test
    @DisplayName("TEST 3: New Firebase UID + existing unique phone -> finds existing customer and associates UID")
    void test3_newFirebaseUidWithExistingPhone_associatesCustomer() {
        String newUid = "fresh-reauthenticated-uid-303";
        String phone = "+919951949353";

        Customer existing = Customer.builder()
                .customerId(140)
                .firebaseUid("old-stale-uid-101")
                .name("Rishi Kottala")
                .email("rishi@example.com")
                .phone(phone)
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(newUid)).thenReturn(Optional.empty());
        when(customerRepository.findMatchingCustomersByPhone(eq(phone), anyString(), anyString()))
                .thenReturn(List.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(newUid, null, null, phone);

        assertNotNull(result);
        assertEquals(140, result.getCustomerId());
        assertEquals(newUid, result.getFirebaseUid());
        assertEquals("Rishi Kottala", result.getName());
        assertEquals("rishi@example.com", result.getEmail());

        verify(customerRepository).save(existing);
    }

    // =========================================================================
    // TEST 4: Existing customer with real name/email + token with null name/email -> preserves real profile
    // =========================================================================
    @Test
    @DisplayName("TEST 4: Existing customer with real profile + token with null name/email -> preserves real name and email")
    void test4_existingRealProfile_preservedWhenTokenHasNull() {
        String uid = "uid-existing-404";
        String phone = "+919951949353";

        Customer existing = Customer.builder()
                .customerId(140)
                .firebaseUid(uid)
                .name("Rishi Kottala")
                .email("rishi@example.com")
                .phone(phone)
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existing));

        Customer result = userService.syncUserWithFirebase(uid, null, null, phone);

        assertEquals("Rishi Kottala", result.getName());
        assertEquals("rishi@example.com", result.getEmail());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    // =========================================================================
    // TEST 5: Existing customer with real profile -> /api/auth/me returns real profile and isProfileComplete=true
    // =========================================================================
    @Test
    @DisplayName("TEST 5: Existing customer with real profile -> /api/auth/me returns real profile and isProfileComplete=true")
    void test5_existingRealCustomer_authMeReturnsRealProfileComplete() {
        Customer realCustomer = Customer.builder()
                .customerId(140)
                .firebaseUid("uid-test-505")
                .name("Rishi Kottala")
                .email("rishi@example.com")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                140, "uid-test-505", "rishi@example.com", "Rishi Kottala",
                Role.CUSTOMER, null, true, Map.of("phone_number", "+919951949353")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthenticationToken(principal, "mock-token", principal.getAuthorities())
        );

        when(customerRepository.findById(140)).thenReturn(Optional.of(realCustomer));

        ResponseEntity<ApiResponse<AuthUserResponse>> response = authController.getCurrentUser();

        assertNotNull(response.getBody());
        AuthUserResponse authUser = response.getBody().getData();
        assertEquals(140, authUser.getUserId());
        assertEquals("Rishi Kottala", authUser.getName());
        assertEquals("rishi@example.com", authUser.getEmail());
        assertEquals("+919951949353", authUser.getPhone());
        assertTrue(authUser.getIsProfileComplete());
    }

    // =========================================================================
    // TEST 6: New customer -> fallback Customer User has isProfileComplete=false;
    //         Real name with optional dummy email has isProfileComplete=true
    // =========================================================================
    @Test
    @DisplayName("TEST 6: Fallback name has isProfileComplete=false; real name with optional email has isProfileComplete=true")
    void test6_isProfileComplete_requiresOnlyRealName_emailOptional() {
        Customer fallbackCustomer = Customer.builder()
                .customerId(140)
                .firebaseUid("uid-fallback-606")
                .name("Customer User")
                .email("uid-fallback-606@firebase.user")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        assertFalse(userService.isProfileComplete(fallbackCustomer));

        Customer realNameOnlyCustomer = Customer.builder()
                .customerId(140)
                .firebaseUid("uid-fallback-606")
                .name("Rishi Kottala")
                .email("uid-fallback-606@firebase.user") // Email is optional in HinchMart registration
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        // Must be true because name is real and email is optional in HinchMart registration
        assertTrue(userService.isProfileComplete(realNameOnlyCustomer));
    }

    // =========================================================================
    // TEST 7: PUT /api/user/profile with valid name -> saves actual name
    // =========================================================================
    @Test
    @DisplayName("TEST 7: PUT /api/user/profile with valid name -> saves actual name and email")
    void test7_putUserProfile_withValidName_savesActualProfile() {
        Customer customer = Customer.builder()
                .customerId(140)
                .firebaseUid("uid-707")
                .name("Customer User")
                .email("uid-707@firebase.user")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findById(140)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("rishi.new@example.com", 140)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileUpdateRequest updateReq = UserProfileUpdateRequest.builder()
                .name("Rishi Kottala")
                .email("rishi.new@example.com")
                .phone("+919951949353")
                .build();

        UserProfileResponse res = userService.updateUserProfile(140, updateReq);

        assertNotNull(res);
        assertEquals("Rishi Kottala", res.getName());
        assertEquals("rishi.new@example.com", res.getEmail());
        assertEquals("+919951949353", res.getPhone());
    }

    // =========================================================================
    // TEST 8: PUT /api/user/profile with blank name -> returns 400 Name is required
    // =========================================================================
    @Test
    @DisplayName("TEST 8: PUT /api/user/profile with blank name -> returns 400 Name is required")
    void test8_putUserProfile_withBlankName_returns400ValidationFailed() throws Exception {
        UserProfileUpdateRequest blankNameRequest = UserProfileUpdateRequest.builder()
                .name("   ")
                .email("valid@example.com")
                .phone("+919951949353")
                .build();

        userProfileMockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankNameRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));
    }

    // =========================================================================
    // TEST 9: Multiple customers with same phone -> throws CustomerConflictException
    // =========================================================================
    @Test
    @DisplayName("TEST 9: Multiple customers with same phone -> throws CustomerConflictException (fails safely)")
    void test9_multipleCustomersWithSamePhone_throwsConflictException() {
        String uid = "uid-duplicate-909";
        String phone = "+919951949353";

        Customer c1 = Customer.builder().customerId(101).phone(phone).build();
        Customer c2 = Customer.builder().customerId(140).phone(phone).build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findMatchingCustomersByPhone(eq(phone), anyString(), anyString()))
                .thenReturn(List.of(c1, c2));

        CustomerConflictException ex = assertThrows(
                CustomerConflictException.class,
                () -> userService.syncUserWithFirebase(uid, null, null, phone)
        );

        assertTrue(ex.getMessage().contains("Multiple customer accounts found"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    // =========================================================================
    // TEST 10: Unauthenticated access -> throws UnauthorizedException
    // =========================================================================
    @Test
    @DisplayName("TEST 10: Unauthenticated user -> getCurrentUserId throws UnauthorizedException")
    void test10_unauthenticatedAccess_throwsUnauthorized() {
        SecurityContextHolder.clearContext();
        UserContextUtil contextUtil = new UserContextUtil();

        assertThrows(UnauthorizedException.class, contextUtil::getCurrentUserId);
    }

    // =========================================================================
    // TEST 11: Client-supplied phone cannot override verified Firebase phone
    // =========================================================================
    @Test
    @DisplayName("TEST 11: /api/auth/sync client-supplied phone cannot override verified token phone")
    void test11_authSync_clientSuppliedPhoneCannotOverrideVerifiedTokenPhone() {
        String verifiedPhone = "+919951949353";
        String maliciousPhone = "+911111111111";
        String uid = "uid-auth-sync-111";

        FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                140, uid, "user@example.com", "Rishi",
                Role.CUSTOMER, null, true, Map.of("phone_number", verifiedPhone)
        );
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthenticationToken(principal, "mock-token", principal.getAuthorities())
        );

        Customer syncedCustomer = Customer.builder()
                .customerId(140)
                .firebaseUid(uid)
                .phone(verifiedPhone)
                .name("Rishi")
                .email("user@example.com")
                .role("CUSTOMER")
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(syncedCustomer));

        AuthSyncRequest request = new AuthSyncRequest();
        request.setPhone(maliciousPhone); // Client tries to override phone

        ResponseEntity<ApiResponse<AuthUserResponse>> response = authController.syncUser(null, request);

        assertNotNull(response.getBody());
        assertEquals(verifiedPhone, response.getBody().getData().getPhone());
    }

    // =========================================================================
    // TEST 12: Existing real profile must never be overwritten by fallback
    // =========================================================================
    @Test
    @DisplayName("TEST 12: Existing real profile must never be overwritten by fallback during phone-based linking")
    void test12_existingRealProfile_neverOverwrittenByFallback() {
        String phone = "+919951949353";
        String newUid = "uid-relinquished-1212";

        Customer existingCustomer = Customer.builder()
                .customerId(140)
                .firebaseUid("old-uid")
                .name("Rishi Kottala")
                .email("rishi@hinchmart.com")
                .phone(phone)
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(newUid)).thenReturn(Optional.empty());
        when(customerRepository.findMatchingCustomersByPhone(eq(phone), anyString(), anyString()))
                .thenReturn(List.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        // Incoming token has no name, no email
        Customer result = userService.syncUserWithFirebase(newUid, null, null, phone);

        // Real profile credentials MUST remain untouched
        assertEquals("Rishi Kottala", result.getName());
        assertEquals("rishi@hinchmart.com", result.getEmail());
        assertEquals(newUid, result.getFirebaseUid());
    }

    // =========================================================================
    // TEST 13: Concurrency protection for syncUserWithFirebase
    // =========================================================================
    @Test
    @DisplayName("TEST 13: Concurrent syncUserWithFirebase calls for same verified phone create exactly one customer")
    void test13_concurrentSyncUserWithFirebase_createsSingleCustomer() throws Exception {
        String phone = "+919951949353";
        String uid1 = "concurrent-uid-1";
        String uid2 = "concurrent-uid-2";

        java.util.concurrent.atomic.AtomicInteger newCustomerInsertCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicReference<Customer> savedCustomer = new java.util.concurrent.atomic.AtomicReference<>();

        when(customerRepository.findByFirebaseUid(anyString())).thenAnswer(inv -> {
            Customer c = savedCustomer.get();
            if (c != null && c.getFirebaseUid() != null && c.getFirebaseUid().equals(inv.getArgument(0))) {
                return Optional.of(c);
            }
            return Optional.empty();
        });

        when(customerRepository.findMatchingCustomersByPhone(eq(phone), anyString(), anyString())).thenAnswer(inv -> {
            Customer c = savedCustomer.get();
            return c != null ? List.of(c) : Collections.emptyList();
        });

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            if (c.getCustomerId() == null) {
                c.setCustomerId(140);
                newCustomerInsertCount.incrementAndGet();
            }
            savedCustomer.set(c);
            // Simulate brief database latency
            Thread.sleep(50);
            return c;
        });

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Future<Customer> f1 = executor.submit(() -> userService.syncUserWithFirebase(uid1, null, null, phone));
        java.util.concurrent.Future<Customer> f2 = executor.submit(() -> userService.syncUserWithFirebase(uid2, null, null, phone));

        Customer r1 = f1.get(5, java.util.concurrent.TimeUnit.SECONDS);
        Customer r2 = f2.get(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(140, r1.getCustomerId());
        assertEquals(140, r2.getCustomerId());
        // Exactly 1 new customer row was created in the database!
        assertEquals(1, newCustomerInsertCount.get());
    }
}
