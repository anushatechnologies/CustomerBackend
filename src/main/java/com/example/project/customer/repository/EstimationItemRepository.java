package com.example.project.customer.repository;

import com.example.project.customer.entity.EstimationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstimationItemRepository extends JpaRepository<EstimationItem, Long> {

    List<EstimationItem> findByEstimation_Id(Long estimationId);

    Optional<EstimationItem> findByIdAndEstimation_Id(Long itemId, Long estimationId);
}
