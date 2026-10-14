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

    List<SellerPayoutLedger> findAllByOrder_OrderId(Integer orderId);

    List<SellerPayoutLedger> findBySeller_SellerIdOrderByCreatedAtDesc(Integer sellerId);

    Page<SellerPayoutLedger> findByStatus(PayoutLedgerStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM SellerPayoutLedger l WHERE l.status = com.example.project.customer.entity.PayoutLedgerStatus.PENDING AND l.settlementDate IS NOT NULL AND l.settlementDate <= :cutoff AND l.clawbackReason IS NULL AND l.id > :lastId ORDER BY l.id ASC")
    List<SellerPayoutLedger> findPendingLedgersEligibleForSettlement(
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff,
            @org.springframework.data.repository.query.Param("lastId") Integer lastId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE SellerPayoutLedger l SET l.status = com.example.project.customer.entity.PayoutLedgerStatus.ELIGIBLE, l.updatedAt = CURRENT_TIMESTAMP WHERE l.id = :id AND l.status = com.example.project.customer.entity.PayoutLedgerStatus.PENDING AND l.settlementDate <= :cutoff AND l.clawbackReason IS NULL")
    int markLedgerEligibleAtomic(
            @org.springframework.data.repository.query.Param("id") Integer id,
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
