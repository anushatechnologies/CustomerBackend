package com.example.project.customer.service;

import com.example.project.customer.dto.InventoryAdjustmentRequest;
import com.example.project.customer.dto.WarehouseRequest;
import com.example.project.customer.entity.InventoryAdjustment;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Warehouse;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.InventoryAdjustmentRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerWarehouseServiceImpl implements SellerWarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Override
    public List<Warehouse> getWarehouses(Integer sellerId) {
        List<Warehouse> warehouses = warehouseRepository.findBySellerId(sellerId);
        if (warehouses.isEmpty()) {
            // Seed a default warehouse if none exists
            Warehouse defaultWh = Warehouse.builder()
                    .sellerId(sellerId)
                    .name("Bhiwandi Central Logistics Yard")
                    .isDefault(true)
                    .contactPerson("Logistics Manager")
                    .phone("+91 98201 11223")
                    .city("Bhiwandi")
                    .state("Maharashtra")
                    .pincode("421302")
                    .address("Plot C-14, Mankoli Logistics Hub, Bhiwandi")
                    .capacityTons(15000)
                    .status("Active")
                    .build();
            warehouses = List.of(warehouseRepository.save(defaultWh));
        }
        return warehouses;
    }

    @Override
    public Warehouse createWarehouse(Integer sellerId, WarehouseRequest request) {
        if (Boolean.TRUE.equals(request.isDefault())) {
            // Unset previous default
            List<Warehouse> existing = warehouseRepository.findBySellerId(sellerId);
            for (Warehouse wh : existing) {
                if (Boolean.TRUE.equals(wh.getIsDefault())) {
                    wh.setIsDefault(false);
                    warehouseRepository.save(wh);
                }
            }
        }

        Warehouse warehouse = Warehouse.builder()
                .sellerId(sellerId)
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .address(request.getAddress())
                .capacityTons(request.getCapacityTons())
                .isDefault(request.isDefault())
                .status(request.getStatus() != null ? request.getStatus() : "Active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return warehouseRepository.save(warehouse);
    }

    @Override
    public Map<String, Object> adjustInventory(Integer sellerId, InventoryAdjustmentRequest request) {
        Integer productId = request.getNumericProductId();
        if (productId == null) {
            throw new IllegalArgumentException("Invalid productId in request");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int prevStock = product.getStockQty() != null ? product.getStockQty() : 0;
        int qty = request.getQuantity() != null ? request.getQuantity() : 0;
        int newStock = prevStock;

        String type = request.getAdjustmentType() != null ? request.getAdjustmentType().trim().toLowerCase() : "add";
        switch (type) {
            case "add":
            case "inward":
            case "received":
                newStock = prevStock + qty;
                break;
            case "subtract":
            case "dispatched":
            case "damaged":
            case "waste":
                newStock = Math.max(0, prevStock - qty);
                break;
            case "set":
            case "audit":
            case "physical_audit":
                newStock = Math.max(0, qty);
                break;
            default:
                newStock = prevStock + qty;
                break;
        }

        product.setStockQty(newStock);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        InventoryAdjustment logEntry = InventoryAdjustment.builder()
                .sellerId(sellerId)
                .productId(productId)
                .warehouseId(request.getNumericWarehouseId())
                .adjustmentType(type)
                .quantity(qty)
                .previousStock(prevStock)
                .newStock(newStock)
                .reason(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();
        inventoryAdjustmentRepository.save(logEntry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", "sp_" + productId);
        result.put("newStock", newStock);
        return result;
    }
}
