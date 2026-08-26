package com.example.project.customer.repository;

import com.example.project.customer.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {
    List<Banner> findByActiveTrueOrderBySortOrderAsc();
    List<Banner> findByPositionAndActiveTrueOrderBySortOrderAsc(String position);
    List<Banner> findByPositionOrderBySortOrderAsc(String position);
    List<Banner> findAllByOrderBySortOrderAsc();
}
