package com.hinchexpress.trip.service;

import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.entity.Trip;

public interface TripStateMachineService {

    Trip assignDriver(Trip trip, AssignDriverRequest request);

    Trip markArrivedAtPickup(Trip trip);

    Trip verifyPickupOtpAndConfirmPickup(Trip trip, String otp);

    Trip startTransit(Trip trip);

    Trip markArrivedAtDelivery(Trip trip);

    Trip verifyDeliveryOtpAndComplete(Trip trip, String otp);

    Trip cancelTrip(Trip trip, String reason);
}

