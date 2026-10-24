package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.settlement.CommissionRuleCreateRequest;
import com.example.project.customer.dto.settlement.CommissionRuleResponse;
import com.example.project.customer.dto.settlement.CommissionRuleUpdateRequest;
import com.example.project.customer.service.CommissionRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/commission-rules", "/api/admin/holding-fees"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionRuleController {

    private final CommissionRuleService commissionRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommissionRuleResponse>>> listRules(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<CommissionRuleResponse> rules = commissionRuleService.getRules(categoryId, activeOnly);
        return ResponseEntity.ok(ApiResponse.ok("Commission rules retrieved successfully", rules));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> getRuleById(@PathVariable Integer id) {
        CommissionRuleResponse rule = commissionRuleService.getRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Commission rule retrieved successfully", rule));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> createRule(
            @Valid @RequestBody CommissionRuleCreateRequest request) {
        CommissionRuleResponse created = commissionRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Commission rule created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> updateRule(
            @PathVariable Integer id,
            @Valid @RequestBody CommissionRuleUpdateRequest request) {
        CommissionRuleResponse updated = commissionRuleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Commission rule updated successfully", updated));
    }

    @PatchMapping({"/{id}/status", "/{id}/toggle-status"})
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> toggleStatus(
            @PathVariable Integer id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) Map<String, Boolean> body) {
        Boolean resolvedActive = active;
        if (resolvedActive == null && body != null && body.containsKey("isActive")) {
            resolvedActive = body.get("isActive");
        }
        CommissionRuleResponse updated = commissionRuleService.toggleRuleStatus(id, resolvedActive);
        return ResponseEntity.ok(ApiResponse.ok("Commission rule status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Integer id) {
        commissionRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.ok("Commission rule deactivated successfully", null));
    }
}
