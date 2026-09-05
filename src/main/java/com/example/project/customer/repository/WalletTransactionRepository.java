package com.example.project.customer.repository;

import com.example.project.customer.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {
    List<WalletTransaction> findByWallet_UserIdOrderByTimestampDesc(Integer userId);
    Page<WalletTransaction> findByWallet_UserIdOrderByTimestampDesc(Integer userId, Pageable pageable);
}
