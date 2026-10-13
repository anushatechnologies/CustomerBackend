package com.example.project.customer.repository;

import com.example.project.customer.entity.HinchStoreVendorInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HinchStoreVendorInventoryRepository extends JpaRepository<HinchStoreVendorInventory, Integer> {

    Optional<HinchStoreVendorInventory> findByProduct_ProductIdAndVendorStore_StoreId(Integer productId, Integer storeId);

    List<HinchStoreVendorInventory> findByProduct_ProductIdInAndIsActiveTrue(Collection<Integer> productIds);

    List<HinchStoreVendorInventory> findByVendorStore_StoreIdAndIsActiveTrue(Integer storeId);

    Page<HinchStoreVendorInventory> findAll(Pageable pageable);

    @Query("SELECT i.product.productId, COALESCE(SUM(i.qty - i.reservedQty), 0) " +
           "FROM HinchStoreVendorInventory i " +
           "WHERE i.product.productId IN :productIds AND i.isActive = true " +
           "GROUP BY i.product.productId")
    List<Object[]> getAvailableStockForProducts(@Param("productIds") Collection<Integer> productIds);

    @Query("SELECT COALESCE(SUM(i.qty - i.reservedQty), 0) " +
           "FROM HinchStoreVendorInventory i " +
           "WHERE i.product.productId = :productId AND i.isActive = true")
    int getAvailableStockForProduct(@Param("productId") Integer productId);

    @Modifying
    @Query("UPDATE HinchStoreVendorInventory i " +
           "SET i.reservedQty = i.reservedQty + :delta, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.product.productId = :productId " +
           "  AND i.vendorStore.storeId = :storeId " +
           "  AND i.isActive = true " +
           "  AND (i.qty - i.reservedQty) >= :delta")
    int atomicReserve(@Param("productId") Integer productId,
                      @Param("storeId") Integer storeId,
                      @Param("delta") int delta);

    @Modifying
    @Query("UPDATE HinchStoreVendorInventory i " +
           "SET i.qty = i.qty - :delta, i.reservedQty = i.reservedQty - :delta, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.product.productId = :productId " +
           "  AND i.vendorStore.storeId = :storeId " +
           "  AND i.reservedQty >= :delta")
    int atomicCommit(@Param("productId") Integer productId,
                     @Param("storeId") Integer storeId,
                     @Param("delta") int delta);

    @Modifying
    @Query("UPDATE HinchStoreVendorInventory i " +
           "SET i.reservedQty = i.reservedQty - :delta, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.product.productId = :productId " +
           "  AND i.vendorStore.storeId = :storeId " +
           "  AND i.reservedQty >= :delta")
    int atomicRelease(@Param("productId") Integer productId,
                      @Param("storeId") Integer storeId,
                      @Param("delta") int delta);

    @Modifying
    @Query("UPDATE HinchStoreVendorInventory i " +
           "SET i.qty = i.qty + :delta, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.product.productId = :productId " +
           "  AND i.vendorStore.storeId = :storeId")
    int atomicRestock(@Param("productId") Integer productId,
                      @Param("storeId") Integer storeId,
                      @Param("delta") int delta);
}
