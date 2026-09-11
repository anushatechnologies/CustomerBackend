package com.hinchexpress.trip.repository;

import com.hinchexpress.trip.entity.TripCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripCheckpointRepository extends JpaRepository<TripCheckpoint, Long> {

    List<TripCheckpoint> findByTripIdOrderByRecordedAtAsc(Long tripId);
}