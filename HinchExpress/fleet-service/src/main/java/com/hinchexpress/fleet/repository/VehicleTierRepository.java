package com.hinchexpress.fleet.repository;

import com.hinchexpress.common.enums.VehicleTierCode;
import com.hinchexpress.fleet.entity.VehicleTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VehicleTierRepository extends JpaRepository<VehicleTier, VehicleTierCode> {

    List<VehicleTier> findByActiveTrueOrderByMaxPayloadKgAsc();

    @Query("SELECT vt FROM VehicleTier vt WHERE vt.active = true AND vt.maxPayloadKg >= :weightKg ORDER BY vt.maxPayloadKg ASC")
    List<VehicleTier> findEligibleTiersForWeight(@Param("weightKg") BigDecimal weightKg);
}
