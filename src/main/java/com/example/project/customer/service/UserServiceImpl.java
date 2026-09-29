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

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.project.customer.repository.AdminUserRepository adminUserRepository;
    private final java.util.concurrent.locks.ReentrantLock[] stripedLocks = initStripedLocks(128);

    private static java.util.concurrent.locks.ReentrantLock[] initStripedLocks(int size) {
        java.util.concurrent.locks.ReentrantLock[] locks = new java.util.concurrent.locks.ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new java.util.concurrent.locks.ReentrantLock();
        }
        return locks;
    }

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

        String rawPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        String lockKey = (rawPhone != null && !rawPhone.isBlank())
                ? "phone:" + normalizePhoneDigits(rawPhone)
                : "uid:" + firebaseUid;

        int stripe = Math.abs(lockKey.hashCode() % stripedLocks.length);
        java.util.concurrent.locks.ReentrantLock lock = stripedLocks[stripe];
        lock.lock();
        try {
            return syncUserWithFirebaseInternal(firebaseUid, email, name, rawPhone, requestedRole);
        } finally {
            lock.unlock();
        }
    }

    private Customer syncUserWithFirebaseInternal(String firebaseUid, String email, String name, String rawPhone, String requestedRole) {
        String checkEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;

        // 1. Try finding existing customer by Firebase UID
        Optional<Customer> existingByUid = customerRepository.findByFirebaseUid(firebaseUid);
        if (existingByUid.isPresent()) {
            Customer customer = existingByUid.get();
            boolean updated = false;

            // Preserve real name: only update if customer has default/blank name and new real name is supplied
            if (name != null && !name.isBlank() && !isDefaultName(name) && isDefaultName(customer.getName())) {
                customer.setName(name.trim());
                updated = true;
            }
            if (rawPhone != null && (customer.getPhone() == null || customer.getPhone().isBlank())) {
                customer.setPhone(rawPhone);
                updated = true;
            }
            // Preserve real email: only update if customer has dummy/missing email and new real email is supplied
            if (checkEmail != null && !checkEmail.endsWith("@firebase.user") 
                    && (customer.getEmail() == null || customer.getEmail().isBlank() || customer.getEmail().endsWith("@firebase.user"))) {
                customer.setEmail(checkEmail);
                updated = true;
            }

            if (updateRoleIfEligible(customer, requestedRole, checkEmail)) {
                updated = true;
            }

            if (updated) {
                log.info("Updated existing Customer (ID: {}) for Firebase UID: {}", customer.getCustomerId(), firebaseUid);
                return customerRepository.save(customer);
            }
            return customer;
        }

        // 2. Try finding existing customer by Verified Phone Number
        if (rawPhone != null) {
            String cleanedDigits = normalizePhoneDigits(rawPhone);
            String searchPattern = buildPhoneSearchPattern(cleanedDigits);

            List<Customer> matchingCustomers = customerRepository.findMatchingCustomersByPhone(rawPhone, cleanedDigits, searchPattern);

            if (matchingCustomers.size() > 1) {
                List<Integer> ids = matchingCustomers.stream().map(Customer::getCustomerId).toList();
                log.error("Duplicate customer records detected for phone {}: customer IDs {}", maskPhone(rawPhone), ids);
                throw new CustomerConflictException("Multiple customer accounts found associated with phone number: " + maskPhone(rawPhone) + ". Manual resolution required.");
            }

            if (matchingCustomers.size() == 1) {
                Customer customer = matchingCustomers.get(0);
                boolean updated = false;

                // Validate and associate Firebase UID
                if (customer.getFirebaseUid() == null || !customer.getFirebaseUid().equals(firebaseUid)) {
                    Optional<Customer> otherWithUid = customerRepository.findByFirebaseUid(firebaseUid);
                    if (otherWithUid.isPresent() && !otherWithUid.get().getCustomerId().equals(customer.getCustomerId())) {
                        log.error("Conflict: Firebase UID {} already linked to customer ID {}, cannot relink to phone customer ID {}",
                                firebaseUid, otherWithUid.get().getCustomerId(), customer.getCustomerId());
                        throw new CustomerConflictException("Firebase UID is already linked to another account.");
                    }

                    log.info("Relinking verified phone user: associating Customer ID {} (previous UID: {}) with Firebase UID: {}",
                            customer.getCustomerId(), customer.getFirebaseUid(), firebaseUid);
                    customer.setFirebaseUid(firebaseUid);
                    updated = true;
                }

                // Preserve real name
                if (name != null && !name.isBlank() && !isDefaultName(name) && isDefaultName(customer.getName())) {
                    customer.setName(name.trim());
                    updated = true;
                }

                // Preserve real email
                if (checkEmail != null && !checkEmail.endsWith("@firebase.user") 
                        && (customer.getEmail() == null || customer.getEmail().isBlank() || customer.getEmail().endsWith("@firebase.user"))) {
                    customer.setEmail(checkEmail);
                    updated = true;
                }

                // Ensure phone is normalized/stored if previously missing
                if (customer.getPhone() == null || customer.getPhone().isBlank()) {
                    customer.setPhone(rawPhone);
                    updated = true;
                }

                if (updateRoleIfEligible(customer, requestedRole, checkEmail)) {
                    updated = true;
                }

                log.info("Found existing Customer (ID: {}, Role: {}) by verified phone: {}",
                        customer.getCustomerId(), customer.getRole(), maskPhone(rawPhone));
                return updated ? customerRepository.save(customer) : customer;
            }
        }

        // 3. Try finding existing customer by Email (account linking for Google/Email auth)
        if (checkEmail != null && !checkEmail.endsWith("@firebase.user")) {
            Optional<Customer> existingByEmail = customerRepository.findByEmailIgnoreCase(checkEmail);
            if (existingByEmail.isPresent()) {
                Customer customer = existingByEmail.get();
                boolean updated = false;

                if (customer.getFirebaseUid() == null || !customer.getFirebaseUid().equals(firebaseUid)) {
                    customer.setFirebaseUid(firebaseUid);
                    updated = true;
                }
                if (name != null && !name.isBlank() && !isDefaultName(name) && isDefaultName(customer.getName())) {
                    customer.setName(name.trim());
                    updated = true;
                }
                if (rawPhone != null && (customer.getPhone() == null || customer.getPhone().isBlank())) {
                    customer.setPhone(rawPhone);
                    updated = true;
                }

                if (updateRoleIfEligible(customer, requestedRole, checkEmail)) {
                    updated = true;
                }

                log.info("Linked existing Customer (ID: {}, Role: {}) with Firebase UID: {} via email",
                        customer.getCustomerId(), customer.getRole(), firebaseUid);
                return updated ? customerRepository.save(customer) : customer;
            }
        }

        // 4. Create new Customer record with appropriate role
        // SECURITY: Role.ADMIN can NEVER be granted simply because requestedRole is "ADMIN".
        // It must be verified against configured admin emails or AdminUserRepository.
        String resolvedRole = Role.CUSTOMER.name();
        if (isAdminEmail(checkEmail)) {
            resolvedRole = Role.ADMIN.name();
        } else if ("SELLER".equalsIgnoreCase(requestedRole) || (checkEmail != null && sellerRepository.findFirstByEmailIgnoreCase(checkEmail).isPresent())) {
            resolvedRole = Role.SELLER.name();
        }

        String resolvedName = (name != null && !name.isBlank()) ? name.trim() : (checkEmail != null && !checkEmail.endsWith("@firebase.user") ? checkEmail.split("@")[0] : "Customer User");
        String resolvedEmail = checkEmail != null ? checkEmail : (firebaseUid + "@firebase.user");
        String resolvedPhone = rawPhone;

        Customer newCustomer = Customer.builder()
                .firebaseUid(firebaseUid)
                .name(resolvedName)
                .email(resolvedEmail)
                .phone(resolvedPhone)
                .role(resolvedRole)
                .active(true)
                .build();

        Customer saved = customerRepository.save(newCustomer);
        log.info("Created new Customer profile (ID: {}, Role: {}) for Firebase UID: {} with phone: {}",
                saved.getCustomerId(), saved.getRole(), firebaseUid, maskPhone(resolvedPhone));
        return saved;
    }

@org.springframework.beans.factory.annotation.Value("${app.security.admin-emails:admin@hinchmart.com,admin@example.com}")
private String configuredAdminEmails = "admin@hinchmart.com";

private boolean isAdminEmail(String email) {
    if (email == null || email.isBlank()) return false;
    String clean = email.trim().toLowerCase();
    // SECURITY: Only exact-match comparisons are permitted.
    // Do NOT use contains/startsWith/endsWith checks — they allow privilege escalation
    // (e.g., "notanadmin@gmail.com" or "myadmin@evil.com" would incorrectly match).
    if (configuredAdminEmails != null && !configuredAdminEmails.isBlank()) {
        for (String adm : configuredAdminEmails.split(",")) {
            if (clean.equalsIgnoreCase(adm.trim())) {
                return true;
            }
        }
    }
    if (adminUserRepository != null && adminUserRepository.findByEmailIgnoreCase(clean).isPresent()) {
        return true;
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

    @Override
    @Transactional(readOnly = true)
    public com.example.project.customer.dto.CheckPhoneResponse checkPhoneExists(String phone) {
        if (phone == null || phone.trim().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String rawPhone = phone.trim();
        String cleanedDigits = normalizePhoneDigits(rawPhone);
        String searchPattern = buildPhoneSearchPattern(cleanedDigits);

        // 1. Try finding in Customer repository
        java.util.List<Customer> customers = customerRepository.findMatchingCustomersByPhone(rawPhone, cleanedDigits, searchPattern);
        if (!customers.isEmpty()) {
            Customer customer = customers.get(0);
            return com.example.project.customer.dto.CheckPhoneResponse.builder()
                    .exists(true)
                    .phone(customer.getPhone() != null ? customer.getPhone() : rawPhone)
                    .name(customer.getName())
                    .role(customer.getRole())
                    .build();
        }

        // 2. Try finding in Seller repository if not found in Customer
        java.util.List<Seller> sellers = sellerRepository.findAllByPhone(rawPhone);
        if (sellers.isEmpty() && !rawPhone.equals(cleanedDigits)) {
            sellers = sellerRepository.findAllByPhone(cleanedDigits);
        }
        if (sellers.isEmpty() && cleanedDigits.length() >= 10) {
            String last10 = cleanedDigits.substring(cleanedDigits.length() - 10);
            sellers = sellerRepository.findAllByPhone(last10);
        }

        if (!sellers.isEmpty()) {
            Seller seller = sellers.get(0);
            return com.example.project.customer.dto.CheckPhoneResponse.builder()
                    .exists(true)
                    .phone(seller.getPhone() != null ? seller.getPhone() : rawPhone)
                    .name(seller.getName())
                    .role("SELLER")
                    .build();
        }

        return com.example.project.customer.dto.CheckPhoneResponse.builder()
                .exists(false)
                .phone(rawPhone)
                .build();
    }

    @Override
    public boolean isProfileComplete(Customer customer) {
        if (customer == null) return false;
        String name = customer.getName();

        return name != null && !name.trim().isBlank()
                && !"Customer User".equalsIgnoreCase(name.trim())
                && !"User".equalsIgnoreCase(name.trim());
    }

    private boolean isDefaultName(String name) {
        if (name == null || name.isBlank()) return true;
        String trimmed = name.trim();
        return "Customer User".equalsIgnoreCase(trimmed) || "User".equalsIgnoreCase(trimmed);
    }

    private boolean updateRoleIfEligible(Customer customer, String requestedRole, String checkEmail) {
        String currentRole = customer.getRole();
        // SECURITY: Role.ADMIN can NEVER be granted simply because requestedRole is "ADMIN".
        if (isAdminEmail(checkEmail)) {
            if (!Role.ADMIN.name().equalsIgnoreCase(currentRole)) {
                customer.setRole(Role.ADMIN.name());
                return true;
            }
        } else if ("SELLER".equalsIgnoreCase(requestedRole) || (checkEmail != null && sellerRepository.findFirstByEmailIgnoreCase(checkEmail).isPresent())) {
            if (!Role.ADMIN.name().equalsIgnoreCase(currentRole) && !Role.SELLER.name().equalsIgnoreCase(currentRole)) {
                customer.setRole(Role.SELLER.name());
                return true;
            }
        }
        return false;
    }

    private String normalizePhoneDigits(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    private String buildPhoneSearchPattern(String cleanedDigits) {
        if (cleanedDigits == null || cleanedDigits.isBlank()) return null;
        return (cleanedDigits.length() >= 10)
                ? "%" + cleanedDigits.substring(cleanedDigits.length() - 10)
                : "%" + cleanedDigits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "[NONE]";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) return "****";
        String last4 = digits.substring(digits.length() - 4);
        return "****" + last4;
    }
}
