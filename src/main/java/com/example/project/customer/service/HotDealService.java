package com.example.project.customer.service;

import com.example.project.customer.dto.HomeHotDealsResponse;
import com.example.project.customer.dto.HotDealReorderRequest;
import com.example.project.customer.dto.HotDealRequest;
import com.example.project.customer.dto.HotDealResponse;

import java.util.List;

public interface HotDealService {

    HotDealResponse addHotDeal(HotDealRequest request);

    List<HotDealResponse> getAdminHotDeals();

    HotDealResponse getById(Long id);

    HotDealResponse updateHotDeal(Long id, HotDealRequest request);

    HotDealResponse setStatus(Long id, boolean active);

    void deleteHotDeal(Long id);

    List<HotDealResponse> reorderHotDeals(HotDealReorderRequest request);

    HomeHotDealsResponse getActiveHotDealsForHome();
}
