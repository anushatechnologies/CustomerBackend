package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.HotDealReorderRequest;
import com.example.project.customer.dto.HotDealRequest;
import com.example.project.customer.dto.HotDealResponse;
import com.example.project.customer.service.HotDealService;
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
@RequestMapping("/api/admin/hot-deals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHotDealController {

    private final HotDealService hotDealService;

    @PostMapping
    public ResponseEntity<ApiResponse<HotDealResponse>> create(@Valid @RequestBody HotDealRequest request) {
        HotDealResponse created = hotDealService.addHotDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product added to Hot Deals successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotDealResponse>>> list() {
        List<HotDealResponse> list = hotDealService.getAdminHotDeals();
        return ResponseEntity.ok(ApiResponse.ok("Hot Deals retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotDealResponse>> getById(@PathVariable Long id) {
        HotDealResponse deal = hotDealService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deal retrieved successfully", deal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotDealResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody HotDealRequest request) {
        HotDealResponse updated = hotDealService.updateHotDeal(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deal updated successfully", updated));
    }

    @PatchMapping({"/{id}/status", "/{id}/toggle-status"})
    public ResponseEntity<ApiResponse<HotDealResponse>> setStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) Map<String, Object> body) {

        boolean resolvedActive;
        if (active != null) {
            resolvedActive = active;
        } else if (body != null && body.containsKey("active")) {
            resolvedActive = Boolean.parseBoolean(String.valueOf(body.get("active")));
        } else if (body != null && body.containsKey("isActive")) {
            resolvedActive = Boolean.parseBoolean(String.valueOf(body.get("isActive")));
        } else {
            throw new IllegalArgumentException("Query parameter 'active' or JSON body 'active' is required");
        }

        HotDealResponse updated = hotDealService.setStatus(id, resolvedActive);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deal status updated successfully", updated));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<List<HotDealResponse>>> reorderPatch(@Valid @RequestBody HotDealReorderRequest request) {
        List<HotDealResponse> updated = hotDealService.reorderHotDeals(request);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deals reordered successfully", updated));
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<List<HotDealResponse>>> reorderPut(@Valid @RequestBody HotDealReorderRequest request) {
        List<HotDealResponse> updated = hotDealService.reorderHotDeals(request);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deals reordered successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        hotDealService.deleteHotDeal(id);
        return ResponseEntity.ok(ApiResponse.ok("Hot Deal removed successfully", null));
    }
}
