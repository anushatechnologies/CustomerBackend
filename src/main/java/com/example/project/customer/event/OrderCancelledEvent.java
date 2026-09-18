package com.example.project.customer.event;

public record OrderCancelledEvent(Integer orderId, String reason) {
}
