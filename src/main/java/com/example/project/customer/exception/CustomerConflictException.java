package com.example.project.customer.exception;

public class CustomerConflictException extends RuntimeException {

    public CustomerConflictException(String message) {
        super(message);
    }
}
