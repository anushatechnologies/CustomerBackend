package com.example.project.customer.repository;

import com.example.project.customer.entity.RfqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfqQuestionRepository extends JpaRepository<RfqQuestion, Integer> {
    List<RfqQuestion> findByRfq_RfqIdOrderByCreatedAtAsc(Integer rfqId);
}
