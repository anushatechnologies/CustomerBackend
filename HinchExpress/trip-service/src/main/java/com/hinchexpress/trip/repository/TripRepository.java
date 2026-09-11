package com.hinchexpress.trip.repository;

import com.hinchexpress.common.enums.TripStatus;
import com.hinchexpress.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByOrderId(Integer orderId);

    Optional<Trip> findByTripNumber(String tripNumber);

    boolean existsByOrderId(Integer orderId);

    List<Trip> findByDriverIdAndStatusIn(Long driverId, Collection<TripStatus> statuses);

    List<Trip> findByStatus(TripStatus status);
}