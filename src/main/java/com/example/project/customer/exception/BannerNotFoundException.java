package com.example.project.customer.exception;

public class BannerNotFoundException extends RuntimeException {

    public BannerNotFoundException(String message) {
        super(message);
    }
}
