package com.hinchexpress.trip.service;

import com.hinchexpress.trip.entity.Trip;
import com.hinchexpress.trip.exception.InvalidOtpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp() {
        int otpNum = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otpNum);
    }

    @Override
    public boolean verifyPickupOtp(Trip trip, String submittedOtp) {
        if (trip.isPickupOtpVerified()) {
            return true;
        }

        if (trip.getOtpFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded for trip " + trip.getTripNumber());
        }

        if (trip.getPickupOtp() != null && trip.getPickupOtp().equals(submittedOtp)) {
            trip.setPickupOtpVerified(true);
            log.info("Pickup OTP successfully verified for trip: {}", trip.getTripNumber());
            return true;
        }

        trip.setOtpFailedAttempts(trip.getOtpFailedAttempts() + 1);
        log.warn("Invalid pickup OTP attempt ({}) for trip: {}", trip.getOtpFailedAttempts(), trip.getTripNumber());
        throw new InvalidOtpException("Invalid pickup OTP provided");
    }

    @Override
    public boolean verifyDeliveryOtp(Trip trip, String submittedOtp) {
        if (trip.isDeliveryOtpVerified()) {
            return true;
        }

        if (trip.getOtpFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded for trip " + trip.getTripNumber());
        }

        if (trip.getDeliveryOtp() != null && trip.getDeliveryOtp().equals(submittedOtp)) {
            trip.setDeliveryOtpVerified(true);
            log.info("Delivery OTP successfully verified for trip: {}", trip.getTripNumber());
            return true;
        }

        trip.setOtpFailedAttempts(trip.getOtpFailedAttempts() + 1);
        log.warn("Invalid delivery OTP attempt ({}) for trip: {}", trip.getOtpFailedAttempts(), trip.getTripNumber());
        throw new InvalidOtpException("Invalid delivery OTP provided");
    }
}

