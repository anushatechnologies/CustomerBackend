package com.hinchexpress.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Heavy construction material vehicle classification tiers with certified payload capacities.
 */
@Getter
@RequiredArgsConstructor
public enum VehicleTierCode {
    TIER_3W_ELECTRIC("3-Wheeler Cargo", new BigDecimal("500.000")),
    TIER_1_TON_PICKUP("1-Ton Pickup", new BigDecimal("1200.000")),
    TIER_3_5_TON_TRUCK("3.5-Ton Light Truck", new BigDecimal("3500.000")),
    TIER_10_TON_TIPPER("10-Ton Medium Tipper", new BigDecimal("10000.000")),
    TIER_25_TON_TRAILER("25-Ton Heavy Flatbed", new BigDecimal("25000.000"));

    private final String displayName;
    private final BigDecimal defaultMaxPayloadKg;
}
