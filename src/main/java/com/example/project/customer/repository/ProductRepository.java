package com.example.project.customer.repository;

import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
	List<Product> findByApprovalStatusAndActive(ApprovalStatus approvalStatus, boolean active);
	List<Product> findByApprovalStatus(ApprovalStatus approvalStatus);
	java.util.Optional<Product> findByProductIdAndApprovalStatusAndActive(Integer productId,
																		   ApprovalStatus approvalStatus,
																		   boolean active);
}