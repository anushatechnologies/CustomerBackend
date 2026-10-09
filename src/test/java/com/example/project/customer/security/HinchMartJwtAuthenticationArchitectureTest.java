package com.example.project.customer.security;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.controller.AddressController;
import com.example.project.customer.controller.AdminProductController;
import com.example.project.customer.controller.AuthController;
import com.example.project.customer.controller.CartController;
import com.example.project.customer.controller.OrderController;
import com.example.project.customer.controller.PaymentController;
import com.example.project.customer.controller.UserProfileController;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.PaymentOrderCreateResponse;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.repository.AdminUserRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.service.AddressService;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.JwtService;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.PaymentService;
import com.example.project.customer.service.ProductService;
import com.example.project.customer.service.SellerProductService;
import com.example.project.customer.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AuthController.class,
        CartController.class,
        OrderController.class,
        UserProfileController.class,
        AddressController.class,
        PaymentController.class,
        AdminProductController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        FirebaseAuthEntryPoint.class,
        FirebaseAccessDeniedHandler.class,
        UserContextUtil.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-safety-12345",
        "jwt.expiration=86400000"
})
@SuppressWarnings("null")
class HinchMartJwtAuthenticationArchitectureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private AdminUserRepository adminUserRepository;

    @MockBean
    private CartService cartService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private AddressService addressService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ProductService productService;

    @MockBean
    private SellerProductService sellerProductService;

    private Customer customerUser;
    private Customer adminUser;
    private String customerJwt;
    private String adminJwt;

    private static final String FAKE_FIREBASE_ID_TOKEN =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6ImZha2Uta2V5LWlkIiwidHlwIjoiSldUIn0." +
            "eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vZmFrZS1wcm9qZWN0IiwidWlkIjoiZmJfdXNlcl81MDAifQ." +
            "dGhpc0lzQUZha2VGaXJlYmFzZVNpZ25hdHVyZUZvclRlc3RpbmdQdXJwb3Nlc09ubHlOb3RWYWxpZEZvckpXVA";

    @BeforeEach
    void setUp() {
        customerUser = Customer.builder()
                .customerId(500)
                .firebaseUid("fb-uid-customer-500")
                .name("Rishi Customer")
                .email("customer@hinchmart.com")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        adminUser = Customer.builder()
                .customerId(1)
                .firebaseUid("fb-uid-admin-1")
                .name("HinchMart Admin")
                .email("admin@hinchmart.com")
                .phone("+919876543210")
                .role("ADMIN")
                .active(true)
                .build();

        customerJwt = jwtService.generateToken(customerUser);
        adminJwt = jwtService.generateToken(adminUser);

        when(customerRepository.findById(500)).thenReturn(Optional.of(customerUser));
        when(customerRepository.findById(1)).thenReturn(Optional.of(adminUser));
    }

    // =========================================================================
    // 1. JWT GENERATION
    // =========================================================================
    @Test
    @DisplayName("1. JWT Generation: produces well-formed 3-part HMAC-SHA256 token")
    void test1_jwtGeneration() {
        String token = jwtService.generateToken(customerUser);

        assertThat(token).isNotNull().isNotBlank();
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }

    // =========================================================================
    // 2. JWT CLAIM EXTRACTION
    // =========================================================================
    @Test
    @DisplayName("2. JWT Claim Extraction: extracts sub, customerId, uid, role without PII")
    void test2_jwtClaimExtraction() {
        Claims claims = jwtService.extractClaims(customerJwt);

        assertThat(claims.getSubject()).isEqualTo("500");
        assertThat(jwtService.extractCustomerId(customerJwt)).isEqualTo(500);
        assertThat(jwtService.extractUid(customerJwt)).isEqualTo("fb-uid-customer-500");
        assertThat(jwtService.extractRole(customerJwt)).isEqualTo("CUSTOMER");

        // Assure NO sensitive PII was embedded in the token payload
        assertThat(claims.get("phone")).isNull();
        assertThat(claims.get("email")).isNull();
        assertThat(claims.get("password")).isNull();
    }

    // =========================================================================
    // 3. VALID JWT
    // =========================================================================
    @Test
    @DisplayName("3. Valid JWT: validation returns true and isTokenExpired returns false")
    void test3_validJwt() {
        assertThat(jwtService.validateToken(customerJwt)).isTrue();
        assertThat(jwtService.isTokenExpired(customerJwt)).isFalse();
    }

    // =========================================================================
    // 4. EXPIRED JWT
    // =========================================================================
    @Test
    @DisplayName("4. Expired JWT: expired token fails validation and returns isTokenExpired=true")
    void test4_expiredJwt() {
        // JwtService configured with negative expiration ms simulates an expired token
        JwtService expiredService = new JwtService(
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-safety-12345",
                -1000L
        );
        String expiredToken = expiredService.generateToken(customerUser);

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
    }

    // =========================================================================
    // 5. TAMPERED JWT
    // =========================================================================
    @Test
    @DisplayName("5. Tampered JWT: modified payload or signature fails validation")
    void test5_tamperedJwt() {
        String[] parts = customerJwt.split("\\.");
        // Modify payload characters
        String tamperedToken = parts[0] + "." + parts[1] + "abc." + parts[2];

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }

    // =========================================================================
    // 6. MISSING JWT
    // =========================================================================
    @Test
    @DisplayName("6. Missing JWT: null or empty token fails validation")
    void test6_missingJwt() {
        assertThat(jwtService.validateToken(null)).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
        assertThat(jwtService.validateToken("   ")).isFalse();
    }

    // =========================================================================
    // 7. FIREBASE TOKEN SENT TO PROTECTED API -> 401
    // =========================================================================
    @Test
    @DisplayName("7. Security Rule: Firebase ID token sent to protected API (/api/cart) returns 401 Unauthorized")
    void test7_firebaseTokenSentToProtectedApi_returns401() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + FAKE_FIREBASE_ID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    // =========================================================================
    // 8. HINCHMART JWT SENT TO PROTECTED API -> ACCEPTED
    // =========================================================================
    @Test
    @DisplayName("8. HinchMart JWT sent to protected API (/api/cart) returns 200 OK")
    void test8_hinchmartJwtSentToProtectedApi_returns200() throws Exception {
        when(cartService.getCart(500)).thenReturn(CartResponse.builder().build());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 9. CUSTOMER JWT -> CUSTOMER FUNCTIONALITY
    // =========================================================================
    @Test
    @DisplayName("9. Customer JWT: allows access to customer business endpoints")
    void test9_customerJwt_accessesCustomerApis() throws Exception {
        when(cartService.getCart(500)).thenReturn(CartResponse.builder().build());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 10. CUSTOMER JWT -> ADMIN FUNCTIONALITY FORBIDDEN
    // =========================================================================
    @Test
    @DisplayName("10. Customer JWT: attempting to access admin endpoint (/api/admin/products) returns 403 Forbidden")
    void test10_customerJwt_adminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(403));
    }

    // =========================================================================
    // 11. ADMIN JWT -> ADMIN FUNCTIONALITY ALLOWED
    // =========================================================================
    @Test
    @DisplayName("11. Admin JWT: allows access to admin endpoints (/api/admin/products)")
    void test11_adminJwt_adminAllowed() throws Exception {
        when(productService.getAdminAll()).thenReturn(new ProductListResponse(Collections.emptyList(), 0));

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 12. CUSTOMER CORRECTLY IDENTIFIED FROM JWT
    // =========================================================================
    @Test
    @DisplayName("12. Customer correctly identified: UserContextUtil receives userId=500 from HinchMart JWT")
    void test12_customerCorrectlyIdentifiedFromJwt() throws Exception {
        when(userService.getUserProfile(500)).thenReturn(
                UserProfileResponse.builder()
                        .userId(500)
                        .name("Rishi Customer")
                        .email("customer@hinchmart.com")
                        .role("CUSTOMER")
                        .build()
        );

        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(500))
                .andExpect(jsonPath("$.data.name").value("Rishi Customer"));
    }

    // =========================================================================
    // 13. /api/auth/sync FIREBASE AUTHENTICATION FLOW
    // =========================================================================
    @Test
    @DisplayName("13. /api/auth/sync flow: receives Firebase ID token, verifies it, syncs customer")
    void test13_authSyncFirebaseAuthenticationFlow() throws Exception {
        FirebaseToken mockFirebaseToken = mock(FirebaseToken.class);
        when(mockFirebaseToken.getUid()).thenReturn("fb-uid-sync-test");
        when(mockFirebaseToken.getEmail()).thenReturn("sync.test@example.com");
        when(mockFirebaseToken.getName()).thenReturn("Sync User");
        when(mockFirebaseToken.getClaims()).thenReturn(Map.of("phone_number", "+919951949353"));

        when(firebaseAuthService.verifyIdToken("firebase-id-token-abc")).thenReturn(mockFirebaseToken);

        Customer syncedCustomer = Customer.builder()
                .customerId(501)
                .firebaseUid("fb-uid-sync-test")
                .name("Sync User")
                .email("sync.test@example.com")
                .phone("+919951949353")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(userService.syncUserWithFirebase(
                eq("fb-uid-sync-test"),
                eq("sync.test@example.com"),
                eq("Sync User"),
                eq("+919951949353"),
                isNull()
        )).thenReturn(syncedCustomer);

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firebaseIdToken\": \"firebase-id-token-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(501))
                .andExpect(jsonPath("$.data.firebaseUid").value("fb-uid-sync-test"));
    }

    // =========================================================================
    // 14. JWT RETURNED AFTER SYNC
    // =========================================================================
    @Test
    @DisplayName("14. JWT returned after sync: returns accessToken, tokenType=Bearer, expiresIn, customer object")
    void test14_jwtReturnedAfterSync() throws Exception {
        FirebaseToken mockFirebaseToken = mock(FirebaseToken.class);
        when(mockFirebaseToken.getUid()).thenReturn("fb-uid-500");
        when(mockFirebaseToken.getEmail()).thenReturn("customer@hinchmart.com");
        when(mockFirebaseToken.getName()).thenReturn("Rishi Customer");
        when(mockFirebaseToken.getClaims()).thenReturn(Map.of("phone_number", "+919951949353"));

        when(firebaseAuthService.verifyIdToken("valid-fb-token")).thenReturn(mockFirebaseToken);
        when(userService.syncUserWithFirebase(any(), any(), any(), any(), any())).thenReturn(customerUser);

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firebaseIdToken\": \"valid-fb-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400))
                .andExpect(jsonPath("$.data.customer.customerId").value(500))
                .andExpect(jsonPath("$.data.customer.role").value("CUSTOMER"));
    }

    // =========================================================================
    // 15. PROTECTED CART ENDPOINT
    // =========================================================================
    @Test
    @DisplayName("15. Protected /api/cart: requires HinchMart JWT, rejects unauthenticated")
    void test15_protectedCartEndpoint() throws Exception {
        // Without JWT -> 401
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        // With HinchMart JWT -> 200
        when(cartService.getCart(500)).thenReturn(CartResponse.builder().build());
        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 16. PROTECTED ORDERS ENDPOINT
    // =========================================================================
    @Test
    @DisplayName("16. Protected /api/orders: requires HinchMart JWT, rejects unauthenticated")
    void test16_protectedOrdersEndpoint() throws Exception {
        // Without JWT -> 401
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        // With HinchMart JWT -> 200
        when(orderService.getOrders(eq(500), any(), anyInt(), anyInt())).thenReturn(
                ApiResponse.ok("Orders retrieved", Collections.<OrderSummaryResponse>emptyList())
        );
        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 17. PROTECTED PROFILE ENDPOINT
    // =========================================================================
    @Test
    @DisplayName("17. Protected /api/user/profile: requires HinchMart JWT, rejects unauthenticated")
    void test17_protectedProfileEndpoint() throws Exception {
        // Without JWT -> 401
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());

        // With HinchMart JWT -> 200
        when(userService.getUserProfile(500)).thenReturn(UserProfileResponse.builder().userId(500).build());
        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 18. PROTECTED ADDRESSES ENDPOINT
    // =========================================================================
    @Test
    @DisplayName("18. Protected /api/addresses: requires HinchMart JWT, rejects unauthenticated")
    void test18_protectedAddressesEndpoint() throws Exception {
        // Without JWT -> 401
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isUnauthorized());

        // With HinchMart JWT -> 200
        when(addressService.getAddresses(500)).thenReturn(Collections.<AddressResponse>emptyList());
        mockMvc.perform(get("/api/addresses").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 19. PROTECTED PAYMENTS ENDPOINT
    // =========================================================================
    @Test
    @DisplayName("19. Protected /api/payments/create-order: requires HinchMart JWT, rejects unauthenticated")
    void test19_protectedPaymentsEndpoint() throws Exception {
        // Without JWT -> 401
        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1500, \"currency\": \"INR\"}"))
                .andExpect(status().isUnauthorized());

        // With HinchMart JWT -> 201
        when(paymentService.createPaymentOrder(eq(500), any())).thenReturn(
                PaymentOrderCreateResponse.builder()
                        .razorpayOrderId("order_test_123")
                        .amount(new BigDecimal("1500"))
                        .currency("INR")
                        .build()
        );

        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1500, \"currency\": \"INR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_test_123"));
    }
}
