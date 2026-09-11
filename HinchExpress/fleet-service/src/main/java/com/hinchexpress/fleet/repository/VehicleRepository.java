package com.hinchexpress.fleet.repository;

import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.common.enums.VehicleTierCode;
import com.hinchexpress.fleet.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findByStatusAndActiveTrue(VehicleStatus status);

    Optional<Vehicle> findByDriver_IdAndActiveTrue(Long driverId);

    @Query("SELECT v FROM Vehicle v WHERE v.active = true AND v.status = :status AND v.tier.code IN :tierCodes AND v.driver IS NOT NULL")
    List<Vehicle> findAvailableVehiclesInTiers(
            @Param("status") VehicleStatus status,
            @Param("tierCodes") Collection<VehicleTierCode> tierCodes
    );
}
