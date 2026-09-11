package com.hinchexpress.fleet.repository;

import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.fleet.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByPhone(String phone);

    List<Driver> findByStatusAndActiveTrue(DriverStatus status);
}
