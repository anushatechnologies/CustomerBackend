package com.example.project.customer.service;

import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Role;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.exception.CustomerConflictException;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional
    public Customer syncUserWithFirebase(String firebaseUid, String email, String name, String phone) {
        return syncUserWithFirebase(firebaseUid, email, name, phone, null);
    }

    @Override
    @Transactional
    public Customer syncUserWithFirebase(String firebaseUid, String email, String name, String phone, String requestedRole) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID cannot be empty");
        }

        String checkEmail = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;

        // 1. Try finding existing customer by Firebase UID
        Optional<Customer> existingByUid = customerRepository.findByFirebaseUid(firebaseUid);
        if (existingByUid.isPresent()) {
            Customer customer = existingByUid.get();
            boolean updated = false;
            if (name != null && !name.isBlank() && (customer.getName() == null || "User".equals(customer.getName()) || customer.getName().isBlank())) {
                customer.setName(name.trim());
                updated = true;
            }
            if (phone != null && !phone.isBlank() && customer.getPhone() == null) {
                customer.setPhone(phone.trim());
                updated = true;
            }
            if (checkEmail != null && (customer.getEmail() == null || customer.getEmail().contains("@firebase.user"))) {
                customer.setEmail(checkEmail);
                updated = true;
            }

        String currentRole = customer.getRole();
        if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(checkEmail)) {
            if (!Role.ADMIN.name().equalsIgnoreCase(currentRole)) {
                customer.setRole(Role.ADMIN.name());
                updated = true;
            }
        } else if ("SELLER".equalsIgnoreCase(requestedRole) || (checkEmail != null && sellerRepository.findFirstByEmailIgnoreCase(checkEmail).isPresent())) {
            if (!Role.ADMIN.name().equalsIgnoreCase(currentRole) && !Role.SELLER.name().equalsIgnoreCase(currentRole)) {
                customer.setRole(Role.SELLER.name());
                updated = true;
            }
        }

        if (updated) {
            return customerRepository.save(customer);
        }
        return customer;
    }

    // 2. Try finding existing customer by Email (account linking)
    if (checkEmail != null) {
        Optional<Customer> existingByEmail = customerRepository.findByEmailIgnoreCase(checkEmail);
        if (existingByEmail.isPresent()) {
            Customer customer = existingByEmail.get();
            customer.setFirebaseUid(firebaseUid);
            if (name != null && !name.isBlank() && (customer.getName() == null || customer.getName().isBlank())) {
                customer.setName(name.trim());
            }
            if (phone != null && !phone.isBlank() && customer.getPhone() == null) {
                customer.setPhone(phone.trim());
            }

            String currentRole = customer.getRole();
            if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(checkEmail)) {
                if (!Role.ADMIN.name().equalsIgnoreCase(currentRole)) {
                    customer.setRole(Role.ADMIN.name());
                }
            } else if ("SELLER".equalsIgnoreCase(requestedRole) || sellerRepository.findFirstByEmailIgnoreCase(checkEmail).isPresent()) {
                if (!Role.ADMIN.name().equalsIgnoreCase(currentRole) && !Role.SELLER.name().equalsIgnoreCase(currentRole)) {
                    customer.setRole(Role.SELLER.name());
                }
            }

            log.info("Linked existing Customer (ID: {}, Role: {}) with Firebase UID: {}", customer.getCustomerId(), customer.getRole(), firebaseUid);
            return customerRepository.save(customer);
        }
    }

    // 3. Create new Customer record with appropriate role
    String resolvedRole = Role.CUSTOMER.name();
    if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(checkEmail)) {
        resolvedRole = Role.ADMIN.name();
    } else if ("SELLER".equalsIgnoreCase(requestedRole) || (checkEmail != null && sellerRepository.findFirstByEmailIgnoreCase(checkEmail).isPresent())) {
        resolvedRole = Role.SELLER.name();
    }

    String resolvedName = (name != null && !name.isBlank()) ? name.trim() : (checkEmail != null ? checkEmail.split("@")[0] : "Customer User");
    String resolvedEmail = checkEmail != null ? checkEmail : (firebaseUid + "@firebase.user");
    String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

    Customer newCustomer = Customer.builder()
            .firebaseUid(firebaseUid)
            .name(resolvedName)
            .email(resolvedEmail)
            .phone(resolvedPhone)
            .role(resolvedRole)
            .active(true)
            .build();

    Customer saved = customerRepository.save(newCustomer);
    log.info("Created new Customer profile (ID: {}, Role: {}) for Firebase UID: {}", saved.getCustomerId(), saved.getRole(), firebaseUid);
    return saved;
}

@org.springframework.beans.factory.annotation.Value("${app.security.admin-emails:admin@hinchmart.com,admin@example.com}")
private String configuredAdminEmails = "admin@hinchmart.com";

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

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerByFirebaseUid(String firebaseUid) {
        return customerRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new CustomerNotFoundException("User not found for Firebase UID: " + firebaseUid));
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Integer userId) {
        Customer customer = getCustomerById(userId);
        Integer sellerId = resolveSellerIdForUser(customer);

        return UserProfileResponse.builder()
                .userId(customer.getCustomerId())
                .firebaseUid(customer.getFirebaseUid())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .active(customer.isActive())
                .sellerId(sellerId)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(Integer userId, UserProfileUpdateRequest request) {
        Customer customer = getCustomerById(userId);

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot(newEmail, userId)) {
                throw new CustomerConflictException("Email is already in use by another account: " + newEmail);
            }
            customer.setEmail(newEmail);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            customer.setName(request.getName().trim());
        }

        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone().trim());
        }

        Customer saved = customerRepository.save(customer);
        Integer sellerId = resolveSellerIdForUser(saved);

        return UserProfileResponse.builder()
                .userId(saved.getCustomerId())
                .firebaseUid(saved.getFirebaseUid())
                .name(saved.getName())
                .email(saved.getEmail())
                .phone(saved.getPhone())
                .role(saved.getRole())
                .active(saved.isActive())
                .sellerId(sellerId)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer resolveSellerIdForUser(Customer customer) {
        if (customer == null || customer.getEmail() == null) {
            return null;
        }
        if ("ADMIN".equalsIgnoreCase(customer.getRole())) {
            return null;
        }
        Optional<Seller> seller = sellerRepository.findFirstByEmailIgnoreCase(customer.getEmail());
        return seller.map(Seller::getSellerId).orElse(null);
    }
}
