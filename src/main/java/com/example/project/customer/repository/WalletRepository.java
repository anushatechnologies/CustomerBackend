package com.example.project.customer.repository;

import com.example.project.customer.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Optional<Wallet> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
}
