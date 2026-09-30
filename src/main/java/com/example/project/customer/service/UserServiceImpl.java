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
        String cleanUid = firebaseUid.trim();

        String cleanEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;
        if (cleanEmail != null && isPlaceholderEmail(cleanEmail)) {
            cleanEmail = null;
        }

        String cleanName = (name != null && !name.isBlank()) ? name.trim() : null;
        if (cleanName != null && isPlaceholderName(cleanName)) {
            cleanName = null;
        }

        String cleanPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        if (cleanPhone != null && isPlaceholderPhone(cleanPhone)) {
            cleanPhone = null;
        }

        // 1. Try finding existing customer by Firebase UID
        Optional<Customer> existingByUid = customerRepository.findByFirebaseUid(cleanUid);
        if (existingByUid.isPresent()) {
            Customer customer = existingByUid.get();
            boolean updated = false;

            // Name update rules:
            // - If incoming real name is available:
            //   - If DB name is null, blank, or placeholder: update it.
            //   - If DB name is already real: update it if different.
            // - If incoming name is null or placeholder: DO NOT overwrite DB name.
            if (cleanName != null) {
                if (customer.getName() == null || customer.getName().isBlank() || isPlaceholderName(customer.getName())) {
                    customer.setName(cleanName);
                    updated = true;
                } else if (!customer.getName().equals(cleanName)) {
                    customer.setName(cleanName);
                    updated = true;
                }
            }

            // Phone update rules:
            // - If incoming real phone is available:
            //   - If DB phone is null, blank, or placeholder: update it if not duplicate.
            //   - If DB phone is already real: update it if different and not duplicate.
            // - If incoming phone is null or placeholder: DO NOT overwrite DB phone.
            if (cleanPhone != null) {
                if (customer.getPhone() == null || customer.getPhone().isBlank() || isPlaceholderPhone(customer.getPhone())) {
                    if (!customerRepository.existsByPhoneAndCustomerIdNot(cleanPhone, customer.getCustomerId())) {
                        customer.setPhone(cleanPhone);
                        updated = true;
                    } else {
                        log.warn("Phone {} is already in use by another customer. Skipping phone update for customerId {}", cleanPhone, customer.getCustomerId());
                    }
                } else if (!customer.getPhone().equals(cleanPhone)) {
                    if (!customerRepository.existsByPhoneAndCustomerIdNot(cleanPhone, customer.getCustomerId())) {
                        customer.setPhone(cleanPhone);
                        updated = true;
                    } else {
                        log.warn("Phone {} is already in use by another customer. Skipping phone update for customerId {}", cleanPhone, customer.getCustomerId());
                    }
                }
            }

            // Email update rules:
            // - If incoming real email is available:
            //   - If DB email is null, blank, or placeholder: update it if not duplicate.
            //   - If DB email is already real: update it if different and not duplicate.
            // - If incoming email is null or placeholder: DO NOT overwrite DB email.
            if (cleanEmail != null) {
                if (customer.getEmail() == null || customer.getEmail().isBlank() || isPlaceholderEmail(customer.getEmail())) {
                    if (!customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot(cleanEmail, customer.getCustomerId())) {
                        customer.setEmail(cleanEmail);
                        updated = true;
                    } else {
                        log.warn("Email {} is already in use by another customer. Skipping email update for customerId {}", cleanEmail, customer.getCustomerId());
                    }
                } else if (!customer.getEmail().equalsIgnoreCase(cleanEmail)) {
                    if (!customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot(cleanEmail, customer.getCustomerId())) {
                        customer.setEmail(cleanEmail);
                        updated = true;
                    } else {
                        log.warn("Email {} is already in use by another customer. Skipping email update for customerId {}", cleanEmail, customer.getCustomerId());
                    }
                }
            }

            // Role update
            String currentRole = customer.getRole();
            String effectiveEmail = customer.getEmail() != null ? customer.getEmail() : cleanEmail;
            if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(effectiveEmail)) {
                if (!Role.ADMIN.name().equalsIgnoreCase(currentRole)) {
                    customer.setRole(Role.ADMIN.name());
                    updated = true;
                }
            } else if ("SELLER".equalsIgnoreCase(requestedRole) || (effectiveEmail != null && sellerRepository.findFirstByEmailIgnoreCase(effectiveEmail).isPresent())) {
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
        if (cleanEmail != null) {
            Optional<Customer> existingByEmail = customerRepository.findByEmailIgnoreCase(cleanEmail);
            if (existingByEmail.isPresent()) {
                Customer customer = existingByEmail.get();
                customer.setFirebaseUid(cleanUid);

                if (cleanName != null) {
                    if (customer.getName() == null || customer.getName().isBlank() || isPlaceholderName(customer.getName())) {
                        customer.setName(cleanName);
                    }
                }
                if (cleanPhone != null) {
                    if (customer.getPhone() == null || customer.getPhone().isBlank() || isPlaceholderPhone(customer.getPhone())) {
                        if (!customerRepository.existsByPhoneAndCustomerIdNot(cleanPhone, customer.getCustomerId())) {
                            customer.setPhone(cleanPhone);
                        } else {
                            log.warn("Phone {} is already in use by another customer. Skipping phone update for customerId {}", cleanPhone, customer.getCustomerId());
                        }
                    }
                }

                String currentRole = customer.getRole();
                if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(cleanEmail)) {
                    if (!Role.ADMIN.name().equalsIgnoreCase(currentRole)) {
                        customer.setRole(Role.ADMIN.name());
                    }
                } else if ("SELLER".equalsIgnoreCase(requestedRole) || sellerRepository.findFirstByEmailIgnoreCase(cleanEmail).isPresent()) {
                    if (!Role.ADMIN.name().equalsIgnoreCase(currentRole) && !Role.SELLER.name().equalsIgnoreCase(currentRole)) {
                        customer.setRole(Role.SELLER.name());
                    }
                }

                log.info("Linked existing Customer (ID: {}, Role: {}) with Firebase UID: {}", customer.getCustomerId(), customer.getRole(), cleanUid);
                return customerRepository.save(customer);
            }
        }

        // 3. Create new Customer record with appropriate role and real values
        String resolvedRole = Role.BUYER.name();
        if ("ADMIN".equalsIgnoreCase(requestedRole) || isAdminEmail(cleanEmail)) {
            resolvedRole = Role.ADMIN.name();
        } else if ("SELLER".equalsIgnoreCase(requestedRole) || (cleanEmail != null && sellerRepository.findFirstByEmailIgnoreCase(cleanEmail).isPresent())) {
            resolvedRole = Role.SELLER.name();
        } else if (requestedRole != null && !requestedRole.isBlank()) {
            resolvedRole = Role.fromString(requestedRole).name();
        }

        String finalPhone = cleanPhone;
        if (finalPhone != null && customerRepository.existsByPhone(finalPhone)) {
            log.warn("Phone number {} is already associated with another customer. Keeping phone as null for new customer UID {}",
                    finalPhone, cleanUid);
            finalPhone = null;
        }

        Customer newCustomer = Customer.builder()
                .firebaseUid(cleanUid)
                .name(cleanName)
                .email(cleanEmail)
                .phone(finalPhone)
                .role(resolvedRole)
                .active(true)
                .build();

        Customer saved = customerRepository.save(newCustomer);
        log.info("Created new Customer profile (ID: {}, Role: {}) for Firebase UID: {}", saved.getCustomerId(), saved.getRole(), cleanUid);
        return saved;
    }

    private boolean isPlaceholderName(String name) {
        if (name == null || name.isBlank()) return true;
        String trimmed = name.trim().toLowerCase();
        return trimmed.equals("unknown user") || trimmed.equals("customer user") || trimmed.equals("user");
    }

    private boolean isPlaceholderEmail(String email) {
        if (email == null || email.isBlank()) return true;
        String trimmed = email.trim().toLowerCase();
        return trimmed.contains("@firebase.user") || trimmed.equals("temp@example.com");
    }

    private boolean isPlaceholderPhone(String phone) {
        if (phone == null || phone.isBlank()) return true;
        String trimmed = phone.trim();
        return trimmed.equals("0000000000") || trimmed.equals("1234567890");
    }

    @org.springframework.beans.factory.annotation.Value("${app.security.admin-emails:admin@hinchmart.com,admin@example.com}")
    private String configuredAdminEmails = "admin@hinchmart.com";

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
            String newPhone = request.getPhone().trim();
            if (!newPhone.isBlank()) {
                if (customerRepository.existsByPhoneAndCustomerIdNot(newPhone, userId)) {
                    throw new CustomerConflictException("Phone number is already in use by another account: " + newPhone);
                }
                customer.setPhone(newPhone);
            } else {
                customer.setPhone(null);
            }
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
