package com.example.project.customer.repository;

import com.example.project.customer.entity.RentalEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalEquipmentRepository extends JpaRepository<RentalEquipment, Integer> {
    List<RentalEquipment> findByAvailableTrue();

    @Query("SELECT r FROM RentalEquipment r WHERE (:category IS NULL OR LOWER(r.category) = LOWER(:category)) " +
           "AND (:location IS NULL OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:available IS NULL OR r.available = :available) ORDER BY r.equipmentId ASC")
    List<RentalEquipment> filterEquipment(
            @Param("category") String category,
            @Param("location") String location,
            @Param("available") Boolean available
    );
}
