package com.example.project.customer.repository;

import com.example.project.customer.entity.RfqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RfqQuestionRepository extends JpaRepository<RfqQuestion, Integer> {
    List<RfqQuestion> findByRfq_RfqIdOrderByCreatedAtAsc(Integer rfqId);
}
