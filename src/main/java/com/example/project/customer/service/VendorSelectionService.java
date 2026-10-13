package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Store;

import java.util.List;
import java.util.Optional;

public interface VendorSelectionService {

    /**
     * Identifies and ranks all candidate vendor stores capable of fulfilling all items in the order.
     * Ranked by:
     * 1. Distance (Haversine) from vendor store to delivery address
     * 2. Total available stock across order items (descending)
     * 3. Lowest storeId (ascending)
     */
    List<Store> getRankedCandidateVendors(List<CartItemResponse> items, Address deliveryAddress);

    /**
     * Selects the single best candidate vendor store.
     */
    Optional<Store> selectFulfillingVendor(List<CartItemResponse> items, Address deliveryAddress);
}
