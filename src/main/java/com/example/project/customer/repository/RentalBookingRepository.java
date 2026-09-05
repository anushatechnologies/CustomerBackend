package com.example.project.customer.repository;

import com.example.project.customer.entity.RentalBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalBookingRepository extends JpaRepository<RentalBooking, Integer> {
    List<RentalBooking> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<RentalBooking> findByEquipment_EquipmentId(Integer equipmentId);

    @Query("SELECT b FROM RentalBooking b WHERE b.equipment.equipmentId = :equipmentId " +
           "AND b.status != 'CANCELLED' " +
           "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<RentalBooking> findConflictingBookings(
            @Param("equipmentId") Integer equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
