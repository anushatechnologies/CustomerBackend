package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.HomeHotDealsResponse;
import com.example.project.customer.service.HotDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/home/hot-deals", "/api/hot-deals"})
@RequiredArgsConstructor
public class HomeHotDealController {

    private final HotDealService hotDealService;

    @GetMapping
    public ResponseEntity<ApiResponse<HomeHotDealsResponse>> getHotDeals() {
        HomeHotDealsResponse response = hotDealService.getActiveHotDealsForHome();
        return ResponseEntity.ok(ApiResponse.ok("Hot Deals retrieved successfully", response));
    }
}
