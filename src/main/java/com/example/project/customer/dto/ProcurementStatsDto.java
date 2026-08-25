package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementStatsDto {
    private long totalOrders;
    private long activeRfqs;
    private long wishlistItems;
    private long savedAddresses;

    public ProcurementStatsDto() {
    }

    public ProcurementStatsDto(long totalOrders, long activeRfqs, long wishlistItems, long savedAddresses) {
        this.totalOrders = totalOrders;
        this.activeRfqs = activeRfqs;
        this.wishlistItems = wishlistItems;
        this.savedAddresses = savedAddresses;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getActiveRfqs() {
        return activeRfqs;
    }

    public void setActiveRfqs(long activeRfqs) {
        this.activeRfqs = activeRfqs;
    }

    public long getWishlistItems() {
        return wishlistItems;
    }

    public void setWishlistItems(long wishlistItems) {
        this.wishlistItems = wishlistItems;
    }

    public long getSavedAddresses() {
        return savedAddresses;
    }

    public void setSavedAddresses(long savedAddresses) {
        this.savedAddresses = savedAddresses;
    }
}
