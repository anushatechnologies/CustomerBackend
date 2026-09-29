package com.example.project.customer.repository;

import com.example.project.customer.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Integer> {

    Optional<VehicleType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<VehicleType> findByActiveTrueOrderBySortOrderAsc();

    List<VehicleType> findAllByOrderBySortOrderAsc();
}
