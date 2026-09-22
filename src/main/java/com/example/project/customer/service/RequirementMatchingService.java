package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.CandidateProductSummary;
import com.example.project.customer.dto.estimation.ExtractedRequirementItem;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.Product;

import java.util.List;

public interface RequirementMatchingService {

    void matchRequirementItem(EstimationItem item, ExtractedRequirementItem req);

    void applySelectedProduct(EstimationItem item, Product product);

    List<CandidateProductSummary> getCandidateSummaries(String candidateProductIds);

    CandidateProductSummary toCandidateSummary(Product product);
}
