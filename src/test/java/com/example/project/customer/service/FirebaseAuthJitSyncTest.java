package com.example.project.customer.service;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthJitSyncTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // default lenient stubs if needed
    }

    @Test
    @DisplayName("1. New Firebase user with all real values saves real values and application defaults (BUYER, active=true)")
    void newFirebaseUser_withAllRealValues_savesRealValuesAndDefaults() {
        String uid = "fb-uid-101";
        String email = "alice@example.com";
        String name = "Alice Wonder";
        String phone = "+919876543210";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(customerRepository.existsByPhone(phone)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(1);
            return c;
        });

        Customer result = userService.syncUserWithFirebase(uid, email, name, phone);

        assertNotNull(result);
        assertEquals(uid, result.getFirebaseUid());
        assertEquals("Alice Wonder", result.getName());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("+919876543210", result.getPhone());
        assertEquals("CUSTOMER", result.getRole());
        assertTrue(result.isActive());

        // Verify save was called with the exact customer
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer captured = captor.getValue();
        assertEquals("Alice Wonder", captured.getName());
        assertEquals("alice@example.com", captured.getEmail());
        assertEquals("+919876543210", captured.getPhone());
        assertEquals("CUSTOMER", captured.getRole());
        assertTrue(captured.isActive());
    }

    @Test
    @DisplayName("2. New Firebase user with missing name saves NULL name without creating fake/default name")
    void newFirebaseUser_withMissingName_savesNullName() {
        String uid = "fb-uid-102";
        String email = "bob@example.com";
        String name = null;
        String phone = "+919876543211";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(customerRepository.existsByPhone(phone)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(uid, email, name, phone);

        assertNotNull(result);
        assertNull(result.getName(), "Name must be NULL rather than 'Customer User' or email prefix");
        assertEquals("bob@example.com", result.getEmail());
        assertEquals("+919876543211", result.getPhone());
        assertEquals("CUSTOMER", result.getRole());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("3. New Firebase user with missing email (e.g. Phone Auth) saves NULL email without dummy email")
    void newFirebaseUser_withMissingEmail_savesNullEmail() {
        String uid = "fb-uid-103";
        String email = null;
        String name = "Charlie";
        String phone = "+919876543212";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.existsByPhone(phone)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(uid, email, name, phone);

        assertNotNull(result);
        assertEquals("Charlie", result.getName());
        assertNull(result.getEmail(), "Email must be NULL rather than dummy '@firebase.user'");
        assertEquals("+919876543212", result.getPhone());
        assertEquals("CUSTOMER", result.getRole());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("4. Existing Firebase UID returns matching customer without altering real values")
    void existingFirebaseUid_returnsExistingCustomer() {
        String uid = "fb-uid-existing";
        Customer existing = Customer.builder()
                .customerId(10)
                .firebaseUid(uid)
                .name("Rishi")
                .email("rishi@example.com")
                .phone("+919999999999")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existing));

        Customer result = userService.syncUserWithFirebase(uid, "rishi@example.com", "Rishi", "+919999999999");

        assertNotNull(result);
        assertEquals(10, result.getCustomerId());
        assertEquals("Rishi", result.getName());
        assertEquals("rishi@example.com", result.getEmail());
        assertEquals("+919999999999", result.getPhone());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Existing customer linked by email associates Firebase UID while preserving existing DB values")
    void existingCustomer_linkedByEmail_preservesExistingValues() {
        String uid = "fb-uid-diana";
        String email = "diana@example.com";
        Customer existingInDb = Customer.builder()
                .customerId(5)
                .firebaseUid(null)
                .name("Diana Prince")
                .email(email)
                .phone("+919876543213")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingInDb));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Incoming token has no name or phone
        Customer result = userService.syncUserWithFirebase(uid, email, null, null);

        assertNotNull(result);
        assertEquals(uid, result.getFirebaseUid());
        assertEquals("Diana Prince", result.getName(), "Existing DB name must be preserved when incoming is null");
        assertEquals(email, result.getEmail());
        assertEquals("+919876543213", result.getPhone(), "Existing DB phone must be preserved when incoming is null");
        assertEquals("CUSTOMER", result.getRole());
        verify(customerRepository).save(existingInDb);
    }

    @Test
    @DisplayName("6. Existing real database values must NOT be overwritten with null")
    void existingRealDbValue_mustNotBeOverwrittenByNull() {
        String uid = "fb-uid-rishi";
        Customer existingInDb = Customer.builder()
                .customerId(20)
                .firebaseUid(uid)
                .name("Rishi")
                .email("rishi@example.com")
                .phone("+919876543214")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existingInDb));

        // Incoming sync has NULL for name, email, phone
        Customer result = userService.syncUserWithFirebase(uid, null, null, null);

        assertNotNull(result);
        assertEquals("Rishi", result.getName(), "DB name 'Rishi' must NOT be overwritten with null");
        assertEquals("rishi@example.com", result.getEmail(), "DB email must NOT be overwritten with null");
        assertEquals("+919876543214", result.getPhone(), "DB phone must NOT be overwritten with null");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Missing DB values get populated when real values become available later")
    void missingDbValue_getsPopulatedWhenRealValueBecomesAvailableLater() {
        String uid = "fb-uid-incomplete";
        Customer existingInDb = Customer.builder()
                .customerId(30)
                .firebaseUid(uid)
                .name(null)
                .email(null)
                .phone("+918888888888")
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existingInDb));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("rishi.kumar@example.com", 30)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Real name and email arrive later
        Customer result = userService.syncUserWithFirebase(uid, "rishi.kumar@example.com", "Rishi Kumar", null);

        assertNotNull(result);
        assertEquals("Rishi Kumar", result.getName(), "Null DB name must be updated when real name arrives");
        assertEquals("rishi.kumar@example.com", result.getEmail(), "Null DB email must be updated when real email arrives");
        assertEquals("+918888888888", result.getPhone(), "Existing phone must be retained");
        verify(customerRepository).save(existingInDb);
    }

    @Test
    @DisplayName("8. Legacy placeholder values get replaced when real values arrive")
    void legacyPlaceholderDbValue_getsReplacedWhenRealValueArrives() {
        String uid = "fb-uid-legacy";
        Customer legacy = Customer.builder()
                .customerId(40)
                .firebaseUid(uid)
                .name("Customer User")
                .email(uid + "@firebase.user")
                .phone(null)
                .role("CUSTOMER")
                .active(true)
                .build();

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(legacy));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("real.user@example.com", 40)).thenReturn(false);
        when(customerRepository.existsByPhoneAndCustomerIdNot("+919123456789", 40)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(uid, "real.user@example.com", "Real User", "+919123456789");

        assertNotNull(result);
        assertEquals("Real User", result.getName(), "Legacy 'Customer User' must be replaced by real name");
        assertEquals("real.user@example.com", result.getEmail(), "Legacy '@firebase.user' must be replaced by real email");
        assertEquals("+919123456789", result.getPhone());
        verify(customerRepository).save(legacy);
    }

    @Test
    @DisplayName("9. Duplicate phone protection prevents setting duplicate phone number on new customer")
    void duplicatePhoneProtection_skipsSettingDuplicatePhoneOnNewCustomer() {
        String uid = "fb-uid-new-dup-phone";
        String email = "fresh@example.com";
        String phone = "+919999999999";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        // Phone is already taken by another account
        when(customerRepository.existsByPhone(phone)).thenReturn(true);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(uid, email, "Fresh User", phone);

        assertNotNull(result);
        assertEquals("Fresh User", result.getName());
        assertEquals("fresh@example.com", result.getEmail());
        assertNull(result.getPhone(), "Phone must remain null when already registered with another customer");
        assertEquals("CUSTOMER", result.getRole());
    }

    @Test
    @DisplayName("10. Application defaults such as BUYER role and active=true are set when role is not supplied")
    void applicationDefaults_buyerRoleAndActiveStatus() {
        String uid = "fb-uid-defaults-check";

        when(customerRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = userService.syncUserWithFirebase(uid, null, null, null, null);

        assertNotNull(result);
        assertEquals("CUSTOMER", result.getRole());
        assertTrue(result.isActive());
        assertNull(result.getName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());
    }
}
