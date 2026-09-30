package com.example.project.customer.repository;

import com.example.project.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByFirebaseUid(String firebaseUid);

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndCustomerIdNot(String email, Integer customerId);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findFirstByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndCustomerIdNot(String phone, Integer customerId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.phone IS NOT NULL AND (" +
            "c.phone = :rawPhone OR " +
            "c.phone = :cleanedPhone OR " +
            "(:searchPattern IS NOT NULL AND c.phone LIKE :searchPattern)) " +
            "ORDER BY c.customerId ASC")
    java.util.List<Customer> findMatchingCustomersByPhone(
            @org.springframework.data.repository.query.Param("rawPhone") String rawPhone,
            @org.springframework.data.repository.query.Param("cleanedPhone") String cleanedPhone,
            @org.springframework.data.repository.query.Param("searchPattern") String searchPattern
    );
}
