package com.example.project.customer.service;

import com.example.project.customer.entity.Product;

public interface InventorySynchronizationService {

    /**
     * Synchronizes product inventory across catalog stock and vendor consignment inventory.
     *
     * - For independent seller products (storeId != platformStoreId), inventory is strictly maintained
     *   in products.stock_qty, keeping multi-tenant seller stores completely isolated.
     * - For platform store products (HinchStore / storeId == platformStoreId), physical availability is
     *   driven by hinch_store_vendor_inventory. This method transactionally resolves the supplying vendor store
     *   and synchronizes the corresponding vendor inventory record while strictly preserving any existing reserved stock.
     *
     * @param product the product entity
     * @param newStockQty the target stock quantity
     * @param sellerId optional authenticated seller ID (or null if admin)
     */
    void syncProductInventory(Product product, Integer newStockQty, Integer sellerId);

    /**
     * Reconciles existing platform products that have positive stock in products.stock_qty
     * but are missing a corresponding hinch_store_vendor_inventory row.
     */
    void reconcileExistingPlatformInventories();
}
