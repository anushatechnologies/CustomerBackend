package com.example.project.customer.exception;

public class EstimationNotFoundException extends RuntimeException {
    public EstimationNotFoundException(String message) {
        super(message);
    }
}
