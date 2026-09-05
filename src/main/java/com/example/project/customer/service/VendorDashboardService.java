package com.example.project.customer.service;

import com.example.project.customer.dto.VendorDashboardResponse;
import com.example.project.customer.dto.VendorPaymentsResponse;
import com.example.project.customer.dto.VendorPerformanceResponse;

public interface VendorDashboardService {
    VendorDashboardResponse getDashboard(Integer sellerId);
    VendorPerformanceResponse getPerformance(Integer sellerId);
    VendorPaymentsResponse getPayments(Integer sellerId);
}
