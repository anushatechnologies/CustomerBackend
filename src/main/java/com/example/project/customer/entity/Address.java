package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(name = "house_flat_no")
    private String houseFlatNo;

    @Column(name = "area_locality")
    private String areaLocality;

    private String landmark;

    @Column(nullable = false)
    @Builder.Default
    private String country = "India";

    private Double latitude;

    private Double longitude;

    @Column(name = "address_type")
    @Builder.Default
    private String addressType = "OTHER";

    // Using Boolean (wrapper) so Lombok generates getIsDefault()/setIsDefault()
    // instead of the confusing isDefault()/setDefault() from primitive boolean
    @Column(name = "is_default")
    @Builder.Default
    @JsonProperty("isDefault")
    private Boolean isDefault = false;

    @Column(name = "has_heavy_vehicle_access")
    @Builder.Default
    @JsonProperty("hasHeavyVehicleAccess")
    private Boolean hasHeavyVehicleAccess = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
