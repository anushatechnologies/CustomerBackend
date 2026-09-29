package com.example.project.customer.repository;

import com.example.project.customer.entity.DeliveryRider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRiderRepository extends JpaRepository<DeliveryRider, Long> {

    Optional<DeliveryRider> findByPhone(String phone);

    List<DeliveryRider> findByIsOnlineTrueAndIsAvailableTrue();

    @Query("SELECT r FROM DeliveryRider r WHERE r.isOnline = true AND r.isAvailable = true AND r.currentLatitude IS NOT NULL AND r.currentLongitude IS NOT NULL")
    List<DeliveryRider> findAvailableOnlineRidersWithCoordinates();
}
