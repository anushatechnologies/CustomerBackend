package com.example.project.customer.repository;

import com.example.project.customer.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    // No custom methods needed currently
}
