package com.example.project.customer.validation;

import com.example.project.customer.exception.InvalidImageException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ImageUrlConstraintValidator implements ConstraintValidator<ValidImageUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // null/empty allowed (optional image)
        }

        try {
            ImageUrlValidator.validateForSave(value);
            return true;
        } catch (InvalidImageException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
