package com.example.project.customer.repository;

import com.example.project.customer.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.customer.customerId = :customerId ORDER BY wt.timestamp DESC")
    List<WalletTransaction> findByWallet_Customer_CustomerIdOrderByTimestampDesc(@Param("customerId") Integer customerId);

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.customer.customerId = :customerId ORDER BY wt.timestamp DESC")
    Page<WalletTransaction> findByWallet_Customer_CustomerIdOrderByTimestampDesc(@Param("customerId") Integer customerId, Pageable pageable);
}
