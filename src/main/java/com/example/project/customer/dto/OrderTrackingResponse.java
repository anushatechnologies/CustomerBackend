package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTrackingResponse {

    private Integer orderId;
    private String orderNumber;
    private String carrierName;
    private String vehicleNumber;
    private String driverName;
    private String trackingNumber;
    private String currentStatus;
    private LocalDateTime estimatedDelivery;
    private List<CheckpointDto> checkpoints;

    public OrderTrackingResponse() {
    }

    public OrderTrackingResponse(Integer orderId, String orderNumber, String carrierName, String vehicleNumber,
                                 String driverName, String trackingNumber, String currentStatus,
                                 LocalDateTime estimatedDelivery, List<CheckpointDto> checkpoints) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.carrierName = carrierName;
        this.vehicleNumber = vehicleNumber;
        this.driverName = driverName;
        this.trackingNumber = trackingNumber;
        this.currentStatus = currentStatus;
        this.estimatedDelivery = estimatedDelivery;
        this.checkpoints = checkpoints;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public List<CheckpointDto> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<CheckpointDto> checkpoints) {
        this.checkpoints = checkpoints;
    }
}
