package com.example.project.customer.repository;

import com.example.project.customer.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Optional<Wallet> findByCustomer_CustomerId(Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.customer.customerId = :customerId")
    Optional<Wallet> findByCustomer_CustomerIdForUpdate(@Param("customerId") Integer customerId);

    boolean existsByCustomer_CustomerId(Integer customerId);
}
