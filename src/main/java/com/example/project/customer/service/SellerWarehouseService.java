package com.example.project.customer.service;

import com.example.project.customer.dto.InventoryAdjustmentRequest;
import com.example.project.customer.dto.WarehouseRequest;
import com.example.project.customer.entity.Warehouse;

import java.util.List;
import java.util.Map;

public interface SellerWarehouseService {

    List<Warehouse> getWarehouses(Integer sellerId);

    Warehouse createWarehouse(Integer sellerId, WarehouseRequest request);

    Map<String, Object> adjustInventory(Integer sellerId, InventoryAdjustmentRequest request);
}
