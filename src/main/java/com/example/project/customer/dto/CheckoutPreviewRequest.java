package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;

public class CheckoutPreviewRequest {

    @NotNull
    private Integer addressId;

    private String deliverySlot = "TOMORROW_MORNING";

    private boolean requiresCraneUnloading = false;

    public CheckoutPreviewRequest() {
    }

    public CheckoutPreviewRequest(Integer addressId, String deliverySlot, boolean requiresCraneUnloading) {
        this.addressId = addressId;
        this.deliverySlot = deliverySlot;
        this.requiresCraneUnloading = requiresCraneUnloading;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public String getDeliverySlot() {
        return deliverySlot;
    }

    public void setDeliverySlot(String deliverySlot) {
        this.deliverySlot = deliverySlot;
    }

    public boolean isRequiresCraneUnloading() {
        return requiresCraneUnloading;
    }

    public void setRequiresCraneUnloading(boolean requiresCraneUnloading) {
        this.requiresCraneUnloading = requiresCraneUnloading;
    }
}
