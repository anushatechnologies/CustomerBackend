package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_warehouses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @Column(name = "warehouse_id", length = 20)
    private String id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "capacity_tons")
    private Long capacityTons;

    @Column(name = "current_load_tons")
    private Long currentLoadTons;

    @Column(name = "status", length = 20)
    private String status; // Active, Inactive

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
