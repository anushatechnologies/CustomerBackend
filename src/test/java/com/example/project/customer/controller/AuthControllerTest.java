package com.example.project.customer.controller;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.repository.AdminUserRepository;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("POST /api/auth/sync with valid Firebase token passes real values and saves successfully")
    void syncUser_withValidFirebaseToken_savesRealValues() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("fb-uid-controller-1");
        when(mockToken.getEmail()).thenReturn("controller@example.com");
        when(mockToken.getName()).thenReturn("Controller User");
        when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+919876543299"));

        when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(mockToken);

        Customer mockCustomer = Customer.builder()
                .customerId(100)
                .firebaseUid("fb-uid-controller-1")
                .name("Controller User")
                .email("controller@example.com")
                .phone("+919876543299")
                .role("BUYER")
                .active(true)
                .build();

        when(userService.syncUserWithFirebase(
                eq("fb-uid-controller-1"),
                eq("controller@example.com"),
                eq("Controller User"),
                eq("+919876543299"),
                isNull()
        )).thenReturn(mockCustomer);

        mockMvc.perform(post("/api/auth/sync")
                        .header("Authorization", "Bearer valid-id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(100))
                .andExpect(jsonPath("$.data.firebaseUid").value("fb-uid-controller-1"))
                .andExpect(jsonPath("$.data.email").value("controller@example.com"))
                .andExpect(jsonPath("$.data.name").value("Controller User"))
                .andExpect(jsonPath("$.data.phone").value("+919876543299"))
                .andExpect(jsonPath("$.data.role").value("BUYER"));
    }

    @Test
    @DisplayName("POST /api/auth/sync with missing name preserves null in response")
    void syncUser_withMissingName_keepsNull() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("fb-uid-no-name");
        when(mockToken.getEmail()).thenReturn("noname@example.com");
        when(mockToken.getName()).thenReturn(null);
        when(mockToken.getClaims()).thenReturn(Map.of());

        when(firebaseAuthService.verifyIdToken("token-no-name")).thenReturn(mockToken);

        Customer mockCustomer = Customer.builder()
                .customerId(101)
                .firebaseUid("fb-uid-no-name")
                .name(null)
                .email("noname@example.com")
                .phone(null)
                .role("BUYER")
                .active(true)
                .build();

        when(userService.syncUserWithFirebase(
                eq("fb-uid-no-name"),
                eq("noname@example.com"),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(mockCustomer);

        mockMvc.perform(post("/api/auth/sync")
                        .header("Authorization", "Bearer token-no-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.firebaseUid").value("fb-uid-no-name"))
                .andExpect(jsonPath("$.data.name").doesNotExist())
                .andExpect(jsonPath("$.data.email").value("noname@example.com"))
                .andExpect(jsonPath("$.data.role").value("BUYER"));
    }
}
