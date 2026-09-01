package com.example.project.customer.repository;

import com.example.project.customer.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Integer> {

    Optional<Seller> findFirstByEmailIgnoreCase(String email);

    List<Seller> findAllByPhone(String phone);

    List<Seller> findAllByPanNumber(String panNumber);

    List<Seller> findAllByAadhaarNumber(String aadhaarNumber);

    List<Seller> findAllByGstin(String gstin);

    List<Seller> findAllByAccountNumber(String accountNumber);
}
