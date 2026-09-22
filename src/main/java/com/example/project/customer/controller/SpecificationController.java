package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SpecificationOptionRequest;
import com.example.project.customer.dto.SpecificationOptionResponse;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;
import com.example.project.customer.service.SpecificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specifications")
@RequiredArgsConstructor
public class SpecificationController {

    private final SpecificationService specificationService;

    // =========================================================
    // SPECIFICATION MASTER ENDPOINTS
    // =========================================================

    @PostMapping
    public ResponseEntity<ApiResponse<SpecificationResponse>> create(@Valid @RequestBody SpecificationRequest request) {
        SpecificationResponse created = specificationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Specification created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecificationResponse>>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search
    ) {
        List<SpecificationResponse> list = specificationService.getAll(active, search);
        return ResponseEntity.ok(ApiResponse.ok("Specifications retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecificationResponse>> getById(@PathVariable Integer id) {
        SpecificationResponse spec = specificationService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Specification retrieved successfully", spec));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecificationResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody SpecificationRequest request
    ) {
        SpecificationResponse updated = specificationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Specification updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        specificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Specification deactivated successfully", null));
    }

    // =========================================================
    // SPECIFICATION OPTIONS ENDPOINTS
    // =========================================================

    @PostMapping("/{id}/options")
    public ResponseEntity<ApiResponse<SpecificationOptionResponse>> addOption(
            @PathVariable Integer id,
            @Valid @RequestBody SpecificationOptionRequest request
    ) {
        SpecificationOptionResponse created = specificationService.addOption(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Option added successfully", created));
    }

    @GetMapping("/{id}/options")
    public ResponseEntity<ApiResponse<List<SpecificationOptionResponse>>> getOptions(@PathVariable Integer id) {
        List<SpecificationOptionResponse> options = specificationService.getOptions(id);
        return ResponseEntity.ok(ApiResponse.ok("Options retrieved successfully", options));
    }

    @PutMapping("/{id}/options/{optionId}")
    public ResponseEntity<ApiResponse<SpecificationOptionResponse>> updateOption(
            @PathVariable Integer id,
            @PathVariable Integer optionId,
            @Valid @RequestBody SpecificationOptionRequest request
    ) {
        SpecificationOptionResponse updated = specificationService.updateOption(id, optionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Option updated successfully", updated));
    }

    @DeleteMapping("/{id}/options/{optionId}")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable Integer id,
            @PathVariable Integer optionId
    ) {
        specificationService.deleteOption(id, optionId);
        return ResponseEntity.ok(ApiResponse.ok("Option deleted successfully", null));
    }
}
