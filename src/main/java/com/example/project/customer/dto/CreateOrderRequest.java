package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {

    @NotNull
    private Integer addressId;

    private String paymentMethod = "RAZORPAY";

    private String deliverySlot = "TOMORROW_MORNING";

    private String deliveryInstructions;

    private String poNumber;

    private boolean requiresCraneUnloading = false;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(Integer addressId, String paymentMethod, String deliverySlot,
                              String deliveryInstructions, String poNumber, boolean requiresCraneUnloading) {
        this.addressId = addressId;
        this.paymentMethod = paymentMethod;
        this.deliverySlot = deliverySlot;
        this.deliveryInstructions = deliveryInstructions;
        this.poNumber = poNumber;
        this.requiresCraneUnloading = requiresCraneUnloading;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliverySlot() {
        return deliverySlot;
    }

    public void setDeliverySlot(String deliverySlot) {
        this.deliverySlot = deliverySlot;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public boolean isRequiresCraneUnloading() {
        return requiresCraneUnloading;
    }

    public void setRequiresCraneUnloading(boolean requiresCraneUnloading) {
        this.requiresCraneUnloading = requiresCraneUnloading;
    }
}
