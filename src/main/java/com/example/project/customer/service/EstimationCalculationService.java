package com.example.project.customer.service;

import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;

public interface EstimationCalculationService {

    void calculateItemPricing(EstimationItem item);

    void recalculateEstimationTotals(Estimation estimation);
}
