package com.hinchexpress.trip.service;

import com.hinchexpress.trip.entity.Trip;

public interface OtpService {

    String generateOtp();

    boolean verifyPickupOtp(Trip trip, String submittedOtp);

    boolean verifyDeliveryOtp(Trip trip, String submittedOtp);
}

