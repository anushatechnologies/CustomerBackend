package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.ProductAvailability;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAvailabilityServiceImpl implements ProductAvailabilityService {

    private final ProductRepository productRepository;
    private final HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;
    private final PlatformMarketplaceProperties platformMarketplaceProperties;

    @Override
    @Transactional(readOnly = true)
    public ProductAvailability getAvailability(Product product) {
        if (product == null) {
            return ProductAvailability.builder()
                    .productId(null)
                    .availableQty(0)
                    .inStock(false)
                    .reason("PRODUCT_NOT_FOUND")
                    .build();
        }
        Map<Integer, ProductAvailability> batch = getAvailabilityBatch(Collections.singletonList(product));
        return batch.getOrDefault(product.getProductId(), ProductAvailability.builder()
                .productId(product.getProductId())
                .availableQty(0)
                .inStock(false)
                .reason("OUT_OF_STOCK")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAvailability getAvailability(Integer productId) {
        if (productId == null) {
            return ProductAvailability.builder()
                    .productId(null)
                    .availableQty(0)
                    .inStock(false)
                    .reason("PRODUCT_NOT_FOUND")
                    .build();
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return getAvailability(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, ProductAvailability> getAvailabilityBatchByIds(Collection<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Product> products = productRepository.findAllById(productIds);
        return getAvailabilityBatch(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, ProductAvailability> getAvailabilityBatch(Collection<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, ProductAvailability> resultMap = new LinkedHashMap<>();
        List<Integer> platformProductIds = new ArrayList<>();

        for (Product p : products) {
            if (p == null || p.getProductId() == null) {
                continue;
            }

            boolean isPlatformStore = isPlatformProduct(p);
            if (isPlatformStore) {
                platformProductIds.add(p.getProductId());
            } else {
                // Vendor-own product: exactly as before, reads products.stock_qty
                int rawStock = p.getStockQty() != null ? p.getStockQty() : 0;
                boolean inStock = rawStock > 0 && p.isActive();
                String reason = !p.isActive() ? "PRODUCT_INACTIVE" : (rawStock <= 0 ? "OUT_OF_STOCK" : null);

                resultMap.put(p.getProductId(), ProductAvailability.builder()
                        .productId(p.getProductId())
                        .availableQty(Math.max(0, rawStock))
                        .inStock(inStock)
                        .reason(reason)
                        .build());
            }
        }

        if (!platformProductIds.isEmpty()) {
            boolean checkoutEnabled = platformMarketplaceProperties != null && platformMarketplaceProperties.isHinchStoreCheckoutEnabled();
            Map<Integer, Integer> stockSumMap = new HashMap<>();
            Map<Integer, Long> rowCountMap = new HashMap<>();

            if (hinchStoreVendorInventoryRepository != null) {
                List<Object[]> rows = hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(platformProductIds);
                for (Object[] r : rows) {
                    if (r != null && r.length >= 3 && r[0] != null) {
                        int pId = ((Number) r[0]).intValue();
                        int availableStock = r[1] != null ? ((Number) r[1]).intValue() : 0;
                        long count = r[2] != null ? ((Number) r[2]).longValue() : 0L;
                        stockSumMap.put(pId, availableStock);
                        rowCountMap.put(pId, count);
                    }
                }
            }

            for (Product p : products) {
                if (p == null || p.getProductId() == null || !platformProductIds.contains(p.getProductId())) {
                    continue;
                }
                int pId = p.getProductId();
                long vendorRows = rowCountMap.getOrDefault(pId, 0L);
                int availableStock = Math.max(0, stockSumMap.getOrDefault(pId, 0));

                int finalAvailableQty;
                boolean finalInStock;
                String finalReason;

                if (!p.isActive()) {
                    finalAvailableQty = availableStock;
                    finalInStock = false;
                    finalReason = "PRODUCT_INACTIVE";
                } else if (vendorRows == 0) {
                    // HinchStore with no vendor inventory rows: explicit reason UNAVAILABLE_NO_VENDOR_STOCK, not a silent 0
                    finalAvailableQty = 0;
                    finalInStock = false;
                    finalReason = "UNAVAILABLE_NO_VENDOR_STOCK";
                } else if (availableStock <= 0) {
                    finalAvailableQty = 0;
                    finalInStock = false;
                    finalReason = "OUT_OF_STOCK";
                } else {
                    // Positive eligible inventory: inStock is true; if checkout is disabled, retain CHECKOUT_DISABLED reason
                    finalAvailableQty = availableStock;
                    finalInStock = true;
                    finalReason = checkoutEnabled ? null : "CHECKOUT_DISABLED";
                }

                resultMap.put(pId, ProductAvailability.builder()
                        .productId(pId)
                        .availableQty(finalAvailableQty)
                        .inStock(finalInStock)
                        .reason(finalReason)
                        .build());
            }
        }

        return resultMap;
    }

    private boolean isPlatformProduct(Product product) {
        if (platformMarketplaceProperties == null) {
            return product.getStore() != null && Integer.valueOf(1).equals(product.getStore().getStoreId());
        }
        return product.getStore() != null && platformMarketplaceProperties.isPlatformStore(product.getStore().getStoreId());
    }
}
