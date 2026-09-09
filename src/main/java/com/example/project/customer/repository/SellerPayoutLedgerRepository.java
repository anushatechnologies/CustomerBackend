package com.example.project.customer.repository;

import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.SellerPayoutLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerPayoutLedgerRepository extends JpaRepository<SellerPayoutLedger, Integer> {

    List<SellerPayoutLedger> findByStore_StoreIdOrderByCreatedAtDesc(Integer storeId);

    Page<SellerPayoutLedger> findByStore_StoreId(Integer storeId, Pageable pageable);

    Optional<SellerPayoutLedger> findByOrder_OrderId(Integer orderId);

    Page<SellerPayoutLedger> findByStatus(PayoutLedgerStatus status, Pageable pageable);
}
