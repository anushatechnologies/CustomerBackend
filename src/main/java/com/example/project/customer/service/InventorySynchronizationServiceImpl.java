package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySynchronizationServiceImpl implements InventorySynchronizationService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

    private PlatformMarketplaceProperties platformMarketplaceProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPlatformMarketplaceProperties(PlatformMarketplaceProperties platformMarketplaceProperties) {
        this.platformMarketplaceProperties = platformMarketplaceProperties;
    }

    @Override
    @Transactional
    public void syncProductInventory(Product product, Integer newStockQty, Integer sellerId) {
        if (product == null || product.getProductId() == null) {
            return;
        }

        int effectiveStock = newStockQty != null ? Math.max(0, newStockQty) : 0;
        product.setStockQty(effectiveStock);
        productRepository.save(product);

        boolean isPlatformStore = isPlatformProduct(product);
        if (!isPlatformStore) {
            // Independent seller product: stock is isolated strictly to products.stock_qty.
            // Do NOT touch hinch_store_vendor_inventory.
            log.debug("Product #{} belongs to independent store #{}. Stock ({}) maintained in products.stock_qty.",
                    product.getProductId(),
                    product.getStore() != null ? product.getStore().getStoreId() : null,
                    effectiveStock);
            return;
        }

        // Platform / HinchStore shared catalog product: physical availability is driven by hinch_store_vendor_inventory.
        Store vendorStore = resolveSupplyingVendorStore(product, sellerId);
        if (vendorStore == null) {
            log.warn("Unable to resolve supplying vendor store for HinchStore product #{}. Inventory sync skipped.", product.getProductId());
            return;
        }

        if (vendorStore.getStatus() != StoreStatus.ACTIVE) {
            log.info("Vendor store #{} is not ACTIVE (status={}). Inventory sync skipped for platform product #{}.",
                    vendorStore.getStoreId(), vendorStore.getStatus(), product.getProductId());
            return;
        }

        Optional<HinchStoreVendorInventory> existingOpt = hinchStoreVendorInventoryRepository
                .findByProduct_ProductIdAndVendorStore_StoreId(product.getProductId(), vendorStore.getStoreId());

        if (existingOpt.isPresent()) {
            HinchStoreVendorInventory existing = existingOpt.get();
            // Preserve existing reserved quantity from active uncommitted orders
            existing.setQty(effectiveStock);
            existing.setIsActive(true);
            hinchStoreVendorInventoryRepository.save(existing);
            log.info("Synchronized existing HinchStore inventory #{} for product #{} at vendor store #{}: qty={}, reservedQty={}",
                    existing.getId(), product.getProductId(), vendorStore.getStoreId(), effectiveStock, existing.getReservedQty());
        } else {
            HinchStoreVendorInventory newInv = HinchStoreVendorInventory.builder()
                    .product(product)
                    .vendorStore(vendorStore)
                    .qty(effectiveStock)
                    .reservedQty(0)
                    .isActive(true)
                    .build();
            hinchStoreVendorInventoryRepository.save(newInv);
            log.info("Created new HinchStore inventory record for product #{} at vendor store #{}: qty={}",
                    product.getProductId(), vendorStore.getStoreId(), effectiveStock);
        }
    }

    @Override
    @Transactional
    public void reconcileExistingPlatformInventories() {
        Integer platformStoreId = getPlatformStoreId();
        List<Product> platformProducts = productRepository.findByStore_StoreId(platformStoreId);
        if (platformProducts == null || platformProducts.isEmpty()) {
            return;
        }

        int reconciledCount = 0;
        for (Product p : platformProducts) {
            if (p == null || p.getProductId() == null) {
                continue;
            }
            try {
                int stock = p.getStockQty() != null ? p.getStockQty() : 0;
                if (stock <= 0) {
                    continue;
                }

                Store vendorStore = resolveSupplyingVendorStore(p, p.getSeller() != null ? p.getSeller().getSellerId() : null);
                if (vendorStore == null) {
                    continue;
                }

                // Verify inactive stores are not treated as active inventory suppliers
                if (vendorStore.getStatus() != StoreStatus.ACTIVE) {
                    log.info("Skipping platform product #{} inventory reconciliation: vendor store #{} is not ACTIVE (status={})",
                            p.getProductId(), vendorStore.getStoreId(), vendorStore.getStatus());
                    continue;
                }

                Optional<HinchStoreVendorInventory> existingOpt = hinchStoreVendorInventoryRepository
                        .findByProduct_ProductIdAndVendorStore_StoreId(p.getProductId(), vendorStore.getStoreId());

                if (existingOpt.isEmpty()) {
                    HinchStoreVendorInventory newInv = HinchStoreVendorInventory.builder()
                            .product(p)
                            .vendorStore(vendorStore)
                            .qty(stock)
                            .reservedQty(0)
                            .isActive(true)
                            .build();
                    hinchStoreVendorInventoryRepository.save(newInv);
                    reconciledCount++;
                    log.info("Reconciled missing HinchStore vendor inventory for product #{} ('{}') at vendor store #{}: qty={}",
                            p.getProductId(), p.getTitle(), vendorStore.getStoreId(), stock);
                }
            } catch (Exception ex) {
                // Ensure one inactive store or error cannot abort the entire reconciliation process
                log.error("Error reconciling inventory for platform product #{}: {}", p.getProductId(), ex.getMessage(), ex);
            }
        }

        if (reconciledCount > 0) {
            log.info("Completed startup inventory reconciliation: {} missing HinchStore inventory records backfilled.", reconciledCount);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        try {
            reconcileExistingPlatformInventories();
        } catch (Exception ex) {
            log.warn("ApplicationReady inventory reconciliation failed: {}", ex.getMessage());
        }
    }

    private Store resolveSupplyingVendorStore(Product product, Integer sellerId) {
        // 1. Try resolving store from the authenticated seller
        if (sellerId != null) {
            Optional<Store> sellerStore = storeRepository.findBySellerSellerId(sellerId);
            if (sellerStore.isPresent()) {
                return sellerStore.get();
            }
        }

        // 2. Try resolving store from the product's associated seller
        if (product.getSeller() != null && product.getSeller().getSellerId() != null) {
            Optional<Store> productSellerStore = storeRepository.findBySellerSellerId(product.getSeller().getSellerId());
            if (productSellerStore.isPresent()) {
                return productSellerStore.get();
            }
        }

        // 3. Try the product's assigned store
        if (product.getStore() != null) {
            return product.getStore();
        }

        // 4. Fallback to platform store
        Integer platformStoreId = getPlatformStoreId();
        return storeRepository.findById(platformStoreId).orElse(null);
    }

    private boolean isPlatformProduct(Product product) {
        if (product == null || product.getStore() == null || product.getStore().getStoreId() == null) {
            return false;
        }
        if (platformMarketplaceProperties != null) {
            return platformMarketplaceProperties.isPlatformStore(product.getStore().getStoreId());
        }
        return Integer.valueOf(1).equals(product.getStore().getStoreId());
    }

    private Integer getPlatformStoreId() {
        if (platformMarketplaceProperties != null && platformMarketplaceProperties.getPlatformStoreId() != null) {
            return platformMarketplaceProperties.getPlatformStoreId();
        }
        return 1;
    }
}
