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
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID cannot be empty");
        }

        // 1. Try finding existing customer by Firebase UID
        Optional<Customer> existingByUid = customerRepository.findByFirebaseUid(firebaseUid);
        if (existingByUid.isPresent()) {
            Customer customer = existingByUid.get();
            boolean updated = false;
            if (name != null && !name.isBlank() && (customer.getName() == null || "User".equals(customer.getName()))) {
                customer.setName(name.trim());
                updated = true;
            }
            if (phone != null && !phone.isBlank() && customer.getPhone() == null) {
                customer.setPhone(phone.trim());
                updated = true;
            }
            if (updated) {
                return customerRepository.save(customer);
            }
            return customer;
        }

        // 2. Try finding existing customer by Email (account linking)
        if (email != null && !email.isBlank()) {
            Optional<Customer> existingByEmail = customerRepository.findByEmailIgnoreCase(email.trim());
            if (existingByEmail.isPresent()) {
                Customer customer = existingByEmail.get();
                customer.setFirebaseUid(firebaseUid);
                if (name != null && !name.isBlank() && (customer.getName() == null || customer.getName().isBlank())) {
                    customer.setName(name.trim());
                }
                log.info("Linked existing Customer (ID: {}) with Firebase UID: {}", customer.getCustomerId(), firebaseUid);
                return customerRepository.save(customer);
            }
        }

        // 3. Create new Customer record with default role CUSTOMER
        String resolvedName = (name != null && !name.isBlank()) ? name.trim() : (email != null ? email.split("@")[0] : "Customer User");
        String resolvedEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : (firebaseUid + "@firebase.user");
        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

        Customer newCustomer = Customer.builder()
                .firebaseUid(firebaseUid)
                .name(resolvedName)
                .email(resolvedEmail)
                .phone(resolvedPhone)
                .role(Role.CUSTOMER.name())
                .active(true)
                .build();

        Customer saved = customerRepository.save(newCustomer);
        log.info("Created new Customer profile (ID: {}, Role: {}) for Firebase UID: {}", saved.getCustomerId(), saved.getRole(), firebaseUid);
        return saved;
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
        Optional<Seller> seller = sellerRepository.findFirstByEmailIgnoreCase(customer.getEmail());
        return seller.map(Seller::getSellerId).orElse(null);
    }
}
