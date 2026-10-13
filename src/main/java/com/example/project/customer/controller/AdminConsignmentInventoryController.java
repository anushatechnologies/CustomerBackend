package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.consignment.HinchStoreInventoryRequest;
import com.example.project.customer.dto.consignment.HinchStoreInventoryResponse;
import com.example.project.customer.service.HinchStoreInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/hinch-store/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConsignmentInventoryController {

    private final HinchStoreInventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<HinchStoreInventoryResponse>> mapOrUpdateInventory(
            @Valid @RequestBody HinchStoreInventoryRequest request) {
        HinchStoreInventoryResponse response = inventoryService.mapOrUpdateInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Consignment inventory mapped successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HinchStoreInventoryResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        boolean isActive = body.getOrDefault("isActive", true);
        HinchStoreInventoryResponse response = inventoryService.updateStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.ok("Consignment inventory status updated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HinchStoreInventoryResponse>>> listInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<HinchStoreInventoryResponse> pageResult = inventoryService.listInventory(pageable);

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ResponseEntity.ok(ApiResponse.paginated("Consignment inventories retrieved successfully",
                pageResult.getContent(), meta));
    }
}
