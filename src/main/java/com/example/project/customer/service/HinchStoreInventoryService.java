package com.example.project.customer.service;

import com.example.project.customer.dto.consignment.HinchStoreInventoryRequest;
import com.example.project.customer.dto.consignment.HinchStoreInventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HinchStoreInventoryService {

    HinchStoreInventoryResponse mapOrUpdateInventory(HinchStoreInventoryRequest request);

    HinchStoreInventoryResponse updateStatus(Integer id, boolean isActive);

    Page<HinchStoreInventoryResponse> listInventory(Pageable pageable);
}
