package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VendorSelectionServiceImpl implements VendorSelectionService {

    private final HinchStoreVendorInventoryRepository inventoryRepository;

    private static class CandidateEvaluation {
        Store store;
        Double distanceKm;
        int totalAvailableStock;

        CandidateEvaluation(Store store, Double distanceKm, int totalAvailableStock) {
            this.store = store;
            this.distanceKm = distanceKm;
            this.totalAvailableStock = totalAvailableStock;
        }
    }

    @Override
    public List<Store> getRankedCandidateVendors(List<CartItemResponse> items, Address deliveryAddress) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Set<Integer> productIds = items.stream()
                .map(CartItemResponse::getProductId)
                .collect(Collectors.toSet());

        List<HinchStoreVendorInventory> inventoryList = inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(productIds);

        // Group inventory by vendor store ID
        Map<Integer, List<HinchStoreVendorInventory>> storeInventoryMap = new HashMap<>();
        Map<Integer, Store> storeMap = new HashMap<>();

        for (HinchStoreVendorInventory inv : inventoryList) {
            Store store = inv.getVendorStore();
            if (store != null && store.getStatus() == StoreStatus.ACTIVE) {
                storeInventoryMap.computeIfAbsent(store.getStoreId(), k -> new ArrayList<>()).add(inv);
                storeMap.put(store.getStoreId(), store);
            }
        }

        List<CandidateEvaluation> candidates = new ArrayList<>();

        // Check each store: must satisfy ALL items at full quantity
        for (Map.Entry<Integer, List<HinchStoreVendorInventory>> entry : storeInventoryMap.entrySet()) {
            Integer storeId = entry.getKey();
            List<HinchStoreVendorInventory> storeInvs = entry.getValue();
            Store store = storeMap.get(storeId);

            Map<Integer, Integer> productAvailMap = new HashMap<>();
            int totalStockForOrder = 0;
            for (HinchStoreVendorInventory inv : storeInvs) {
                int avail = inv.getAvailableQty();
                productAvailMap.put(inv.getProduct().getProductId(), avail);
                totalStockForOrder += avail;
            }

            boolean canFulfillAll = true;
            for (CartItemResponse item : items) {
                int requiredQty = item.getQuantity();
                int available = productAvailMap.getOrDefault(item.getProductId(), 0);
                if (available < requiredQty) {
                    canFulfillAll = false;
                    break;
                }
            }

            if (canFulfillAll) {
                Double distanceKm = null;
                if (deliveryAddress != null && deliveryAddress.getLatitude() != null && deliveryAddress.getLongitude() != null
                        && store.getLatitude() != null && store.getLongitude() != null) {
                    distanceKm = calculateHaversineDistanceKm(
                            store.getLatitude(), store.getLongitude(),
                            deliveryAddress.getLatitude(), deliveryAddress.getLongitude()
                    );
                }
                candidates.add(new CandidateEvaluation(store, distanceKm, totalStockForOrder));
            }
        }

        // Rank candidates: 1) Distance (asc), 2) Total Stock (desc), 3) Lowest storeId (asc)
        candidates.sort((a, b) -> {
            if (a.distanceKm != null && b.distanceKm != null) {
                int diff = Double.compare(a.distanceKm, b.distanceKm);
                if (diff != 0) return diff;
            } else if (a.distanceKm != null) {
                return -1;
            } else if (b.distanceKm != null) {
                return 1;
            }

            int stockDiff = Integer.compare(b.totalAvailableStock, a.totalAvailableStock);
            if (stockDiff != 0) {
                return stockDiff;
            }

            return Integer.compare(a.store.getStoreId(), b.store.getStoreId());
        });

        return candidates.stream().map(c -> c.store).toList();
    }

    @Override
    public Optional<Store> selectFulfillingVendor(List<CartItemResponse> items, Address deliveryAddress) {
        List<Store> candidates = getRankedCandidateVendors(items, deliveryAddress);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
