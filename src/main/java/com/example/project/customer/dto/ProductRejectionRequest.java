package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRejectionRequest(@NotBlank String reason) {
}