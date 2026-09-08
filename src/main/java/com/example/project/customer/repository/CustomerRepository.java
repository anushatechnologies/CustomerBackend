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
}
