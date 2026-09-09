package com.example.project.customer.repository;

import com.example.project.customer.entity.CategoryRequest;
import com.example.project.customer.entity.CategoryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Integer> {

    List<CategoryRequest> findBySeller_SellerIdOrderByCreatedAtDesc(Integer sellerId);

    Page<CategoryRequest> findByStatusOrderByCreatedAtDesc(CategoryRequestStatus status, Pageable pageable);

    Page<CategoryRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
