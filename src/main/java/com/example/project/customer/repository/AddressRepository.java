package com.example.project.customer.repository;

import com.example.project.customer.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findAllByOrderByIsDefaultDescCreatedAtDesc();
    Optional<Address> findByIsDefaultTrue();
}
