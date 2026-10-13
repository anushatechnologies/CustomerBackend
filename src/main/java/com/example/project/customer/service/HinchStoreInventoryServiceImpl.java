package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.consignment.HinchStoreInventoryRequest;
import com.example.project.customer.dto.consignment.HinchStoreInventoryResponse;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HinchStoreInventoryServiceImpl implements HinchStoreInventoryService {

    private final HinchStoreVendorInventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PlatformMarketplaceProperties platformMarketplaceProperties;

    @Override
    public HinchStoreInventoryResponse mapOrUpdateInventory(HinchStoreInventoryRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (product.getStore() == null || !platformMarketplaceProperties.isPlatformStore(product.getStore().getStoreId())) {
            throw new IllegalArgumentException("Product ID " + request.getProductId() + " does not belong to the HinchStore platform store.");
        }

        Store vendorStore = storeRepository.findById(request.getVendorStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor store not found with id: " + request.getVendorStoreId()));

        if (platformMarketplaceProperties.isPlatformStore(vendorStore.getStoreId())) {
            throw new IllegalArgumentException("Vendor store cannot be the platform store.");
        }

        HinchStoreVendorInventory inv = inventoryRepository
                .findByProduct_ProductIdAndVendorStore_StoreId(request.getProductId(), request.getVendorStoreId())
                .orElseGet(() -> HinchStoreVendorInventory.builder()
                        .product(product)
                        .vendorStore(vendorStore)
                        .reservedQty(0)
                        .build());

        inv.setQty(request.getQty());
        if (request.getIsActive() != null) {
            inv.setIsActive(request.getIsActive());
        }

        HinchStoreVendorInventory saved = inventoryRepository.save(inv);
        log.info("Mapped consignment inventory ID={} for product ID={} at vendor store ID={} with qty={}",
                saved.getId(), product.getProductId(), vendorStore.getStoreId(), saved.getQty());

        return mapToResponse(saved);
    }

    @Override
    public HinchStoreInventoryResponse updateStatus(Integer id, boolean isActive) {
        HinchStoreVendorInventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment inventory not found with id: " + id));
        inv.setIsActive(isActive);
        return mapToResponse(inventoryRepository.save(inv));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HinchStoreInventoryResponse> listInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::mapToResponse);
    }

    private HinchStoreInventoryResponse mapToResponse(HinchStoreVendorInventory inv) {
        return HinchStoreInventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProduct() != null ? inv.getProduct().getProductId() : null)
                .productTitle(inv.getProduct() != null ? inv.getProduct().getTitle() : null)
                .vendorStoreId(inv.getVendorStore() != null ? inv.getVendorStore().getStoreId() : null)
                .vendorStoreName(inv.getVendorStore() != null ? inv.getVendorStore().getName() : null)
                .vendorSellerId(inv.getVendorStore() != null && inv.getVendorStore().getSeller() != null
                        ? inv.getVendorStore().getSeller().getSellerId() : null)
                .vendorSellerName(inv.getVendorStore() != null && inv.getVendorStore().getSeller() != null
                        ? inv.getVendorStore().getSeller().getName() : null)
                .qty(inv.getQty())
                .reservedQty(inv.getReservedQty())
                .availableQty(inv.getAvailableQty())
                .isActive(inv.getIsActive())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}
