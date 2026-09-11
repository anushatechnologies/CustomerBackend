package com.hinchexpress.trip.service;

import com.hinchexpress.common.dto.TripSummaryDto;
import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.dto.CheckpointDto;
import com.hinchexpress.trip.dto.CreateTripRequest;

import java.util.List;

public interface TripService {

    TripSummaryDto createTrip(CreateTripRequest request);

    TripSummaryDto assignDriver(Long tripId, AssignDriverRequest request);

    TripSummaryDto markArrivedAtPickup(Long tripId);

    TripSummaryDto verifyPickupOtp(Long tripId, String otp);

    TripSummaryDto startTransit(Long tripId);

    TripSummaryDto markArrivedAtDelivery(Long tripId);

    TripSummaryDto verifyDeliveryOtp(Long tripId, String otp);

    TripSummaryDto cancelTrip(Long tripId, String reason);

    TripSummaryDto getTripById(Long tripId);

    TripSummaryDto getTripByOrderId(Integer orderId);

    List<CheckpointDto> getTripCheckpoints(Long tripId);
}

