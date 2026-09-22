package com.example.project.customer.repository;

import com.example.project.customer.entity.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, Integer>, JpaSpecificationExecutor<Specification> {

    Optional<Specification> findByKeyIgnoreCase(String key);

    Optional<Specification> findByNameIgnoreCase(String name);

    boolean existsByKeyIgnoreCase(String key);

    boolean existsByKeyIgnoreCaseAndSpecificationIdNot(String key, Integer specificationId);

    List<Specification> findByActiveTrueOrderByNameAsc();

    List<Specification> findAllByOrderByNameAsc();
}
