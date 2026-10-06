package com.example.project.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ImageUrlConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageUrl {

    String message() default "Invalid image URL. Must be a valid S3 URL or relative key, and cannot be a blob URL or base64 data.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
