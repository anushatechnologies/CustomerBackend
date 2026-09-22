package com.example.project.customer.repository;

import com.example.project.customer.entity.SpecificationOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecificationOptionRepository extends JpaRepository<SpecificationOption, Integer> {

    List<SpecificationOption> findBySpecification_SpecificationIdOrderByDisplayOrderAscOptionIdAsc(Integer specificationId);

    Optional<SpecificationOption> findBySpecification_SpecificationIdAndOptionId(Integer specificationId, Integer optionId);

    void deleteBySpecification_SpecificationId(Integer specificationId);
}
