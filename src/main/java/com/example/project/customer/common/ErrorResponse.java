package com.example.project.customer.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success = false;
    private String message;
    private int statusCode;
    private List<FieldErrorItem> errors;

    public ErrorResponse() {
        this.success = false;
    }

    public ErrorResponse(String message, int statusCode) {
        this.success = false;
        this.message = message;
        this.statusCode = statusCode;
    }

    public ErrorResponse(String message, int statusCode, List<FieldErrorItem> errors) {
        this.success = false;
        this.message = message;
        this.statusCode = statusCode;
        this.errors = errors;
    }

    public static ErrorResponse of(String message, int statusCode) {
        return new ErrorResponse(message, statusCode);
    }

    public static ErrorResponse of(String message, int statusCode, List<FieldErrorItem> errors) {
        return new ErrorResponse(message, statusCode, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public List<FieldErrorItem> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldErrorItem> errors) {
        this.errors = errors;
    }

    public void addError(String field, String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(new FieldErrorItem(field, message));
    }
}
