package com.example.project.customer.service;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Security regression tests for admin email resolution in UserServiceImpl.
 *
 * Verifies the fix for the privilege-escalation vulnerability:
 * previously clean.contains("admin") granted ADMIN to any email containing
 * "admin". Now only exact matches against the configured allow-list work.
 *
 * Expected outcomes:
 *   admin@hinchmart.com       -> ADMIN   (exact configured email)
 *   ADMIN@HINCHMART.COM       -> ADMIN   (case-insensitive exact match)
 *   myadmin@gmail.com         -> CUSTOMER (substring, must NOT be admin)
 *   notanadmin@gmail.com      -> CUSTOMER (substring, must NOT be admin)
 *   customeradmin@gmail.com   -> CUSTOMER (substring, must NOT be admin)
 *   customer@gmail.com        -> CUSTOMER (normal customer)
 */
@ExtendWith(MockitoExtension.class)
class UserServiceAdminEmailSecurityTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(customerRepository, sellerRepository);
        ReflectionTestUtils.setField(userService, "configuredAdminEmails", "admin@hinchmart.com");
    }

    private void stubSave() {
        when(customerRepository.findByFirebaseUid(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        // lenient: for admin emails, isAdminEmail() short-circuits before the seller lookup,
        // so this stub may not be invoked. Using lenient() avoids UnnecessaryStubbingException.
        lenient().when(sellerRepository.findFirstByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("admin@hinchmart.com -> ADMIN (exact match, lowercase)")
    void exactAdminEmail_lowercase_getsAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid1", "admin@hinchmart.com", "Admin", null);
        assertEquals(Role.ADMIN.name(), result.getRole(),
                "Exact configured admin email must receive ADMIN role");
    }

    @Test
    @DisplayName("ADMIN@HINCHMART.COM -> ADMIN (exact match, case-insensitive)")
    void exactAdminEmail_uppercase_getsAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid2", "ADMIN@HINCHMART.COM", "Admin", null);
        assertEquals(Role.ADMIN.name(), result.getRole(),
                "Uppercase exact configured admin email must receive ADMIN role");
    }

    @Test
    @DisplayName("myadmin@gmail.com -> CUSTOMER (substring of admin, not exact match)")
    void emailContainingAdmin_prefix_doesNotGetAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid3", "myadmin@gmail.com", "My Admin", null);
        assertNotEquals(Role.ADMIN.name(), result.getRole(),
                "Email containing 'admin' as substring must NOT be granted ADMIN role");
        assertEquals(Role.CUSTOMER.name(), result.getRole());
    }

    @Test
    @DisplayName("notanadmin@gmail.com -> CUSTOMER (substring of admin, not exact match)")
    void emailContainingAdmin_suffix_doesNotGetAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid4", "notanadmin@gmail.com", "Not An Admin", null);
        assertNotEquals(Role.ADMIN.name(), result.getRole(),
                "Email 'notanadmin@gmail.com' must NOT be granted ADMIN role");
        assertEquals(Role.CUSTOMER.name(), result.getRole());
    }

    @Test
    @DisplayName("customeradmin@gmail.com -> CUSTOMER (substring of admin, not exact match)")
    void emailContainingAdmin_middle_doesNotGetAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid5", "customeradmin@gmail.com", "Customer Admin", null);
        assertNotEquals(Role.ADMIN.name(), result.getRole(),
                "Email 'customeradmin@gmail.com' must NOT be granted ADMIN role");
        assertEquals(Role.CUSTOMER.name(), result.getRole());
    }

    @Test
    @DisplayName("admin_buyer@gmail.com -> CUSTOMER (substring of admin, not exact match)")
    void emailContainingAdmin_withUnderscore_doesNotGetAdminRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid6", "admin_buyer@gmail.com", "Admin Buyer", null);
        assertNotEquals(Role.ADMIN.name(), result.getRole(),
                "Email 'admin_buyer@gmail.com' must NOT be granted ADMIN role");
        assertEquals(Role.CUSTOMER.name(), result.getRole());
    }

    @Test
    @DisplayName("customer@gmail.com -> CUSTOMER (normal user)")
    void normalCustomerEmail_getsCustomerRole() {
        stubSave();
        Customer result = userService.syncUserWithFirebase("uid7", "customer@gmail.com", "John Doe", null);
        assertEquals(Role.CUSTOMER.name(), result.getRole(),
                "Normal customer email must remain CUSTOMER");
    }
}
