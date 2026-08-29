package com.example.project.customer.repository;

import com.example.project.customer.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Integer> {

    Optional<Seller> findByEmailIgnoreCase(String email);

    Optional<Seller> findByPanNumber(String panNumber);

    Optional<Seller> findByGstin(String gstin);
}
