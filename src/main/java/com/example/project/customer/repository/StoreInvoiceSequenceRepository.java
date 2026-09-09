package com.example.project.customer.repository;

import com.example.project.customer.entity.StoreInvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreInvoiceSequenceRepository extends JpaRepository<StoreInvoiceSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreInvoiceSequence s WHERE s.store.storeId = :storeId AND s.financialYear = :fy")
    Optional<StoreInvoiceSequence> findByStoreIdAndFinancialYearForUpdate(@Param("storeId") Integer storeId, @Param("fy") String financialYear);

    Optional<StoreInvoiceSequence> findByStore_StoreIdAndFinancialYear(Integer storeId, String financialYear);
}
