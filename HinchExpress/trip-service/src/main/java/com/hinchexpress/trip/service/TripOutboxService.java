package com.hinchexpress.trip.service;

import com.hinchexpress.trip.entity.DeliveryOutboxEvent;
import com.hinchexpress.trip.entity.Trip;

public interface TripOutboxService {

    DeliveryOutboxEvent recordTripEvent(Trip trip, String eventType, String remarks);
}

