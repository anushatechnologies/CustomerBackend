package com.hinchexpress.fleet.config;

import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.common.enums.VehicleTierCode;
import com.hinchexpress.fleet.entity.Driver;
import com.hinchexpress.fleet.entity.Vehicle;
import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.repository.DriverRepository;
import com.hinchexpress.fleet.repository.VehicleRepository;
import com.hinchexpress.fleet.repository.VehicleTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDataSeeder implements CommandLineRunner {

    private final VehicleTierRepository vehicleTierRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        seedTiers();
        seedDriversAndVehicles();
    }

    private void seedTiers() {
        if (vehicleTierRepository.count() == 0) {
            List<VehicleTier> tiers = List.of(
                    VehicleTier.builder()
                            .code(VehicleTierCode.TIER_3W_ELECTRIC)
                            .name("3-Wheeler Cargo")
                            .description("Electrical fittings, paint buckets, small toolkits")
                            .maxPayloadKg(new BigDecimal("500.000"))
                            .volumetricCapacityM3(new BigDecimal("2.50"))
                            .baseFare(new BigDecimal("120.00"))
                            .perKmRate(new BigDecimal("18.00"))
                            .active(true)
                            .build(),
                    VehicleTier.builder()
                            .code(VehicleTierCode.TIER_1_TON_PICKUP)
                            .name("1-Ton Pickup")
                            .description("Cement bags, light tiles, sanitaryware batches")
                            .maxPayloadKg(new BigDecimal("1200.000"))
                            .volumetricCapacityM3(new BigDecimal("5.00"))
                            .baseFare(new BigDecimal("250.00"))
                            .perKmRate(new BigDecimal("25.00"))
                            .active(true)
                            .build(),
                    VehicleTier.builder()
                            .code(VehicleTierCode.TIER_3_5_TON_TRUCK)
                            .name("3.5-Ton Light Truck")
                            .description("Sand bags, brick bundles, medium civil loads")
                            .maxPayloadKg(new BigDecimal("3500.000"))
                            .volumetricCapacityM3(new BigDecimal("10.00"))
                            .baseFare(new BigDecimal("600.00"))
                            .perKmRate(new BigDecimal("38.00"))
                            .active(true)
                            .build(),
                    VehicleTier.builder()
                            .code(VehicleTierCode.TIER_10_TON_TIPPER)
                            .name("10-Ton Medium Tipper")
                            .description("Bulk blue metal aggregates, M-Sand, palletized cement")
                            .maxPayloadKg(new BigDecimal("10000.000"))
                            .volumetricCapacityM3(new BigDecimal("18.00"))
                            .baseFare(new BigDecimal("1500.00"))
                            .perKmRate(new BigDecimal("65.00"))
                            .active(true)
                            .build(),
                    VehicleTier.builder()
                            .code(VehicleTierCode.TIER_25_TON_TRAILER)
                            .name("25-Ton Heavy Flatbed")
                            .description("Structural steel TMT bundles, precast slabs, heavy industrial materials")
                            .maxPayloadKg(new BigDecimal("25000.000"))
                            .volumetricCapacityM3(new BigDecimal("35.00"))
                            .baseFare(new BigDecimal("3500.00"))
                            .perKmRate(new BigDecimal("110.00"))
                            .active(true)
                            .build()
            );
            vehicleTierRepository.saveAll(tiers);
            log.info("Seeded 5 heavy construction vehicle tiers in fleet-service.");
        }
    }

    private void seedDriversAndVehicles() {
        if (driverRepository.count() == 0) {
            // Driver 1: Rajesh Sharma (1-Ton Tata Ace) near Hyderabad Central / Abids
            Driver d1 = driverRepository.save(Driver.builder()
                    .name("Rajesh Sharma")
                    .phone("+91 9849011111")
                    .licenseNumber("TS09-2018-001234")
                    .status(DriverStatus.AVAILABLE)
                    .rating(new BigDecimal("4.85"))
                    .totalTripsCompleted(142)
                    .active(true)
                    .build());

            // Driver 2: Mohammed Ismail (1-Ton Bolero) near Kukatpally
            Driver d2 = driverRepository.save(Driver.builder()
                    .name("Mohammed Ismail")
                    .phone("+91 9849022222")
                    .licenseNumber("TS08-2019-005678")
                    .status(DriverStatus.AVAILABLE)
                    .rating(new BigDecimal("4.90"))
                    .totalTripsCompleted(210)
                    .active(true)
                    .build());

            // Driver 3: Venkat Rao (3.5-Ton Eicher) near Gachibowli
            Driver d3 = driverRepository.save(Driver.builder()
                    .name("Venkat Rao")
                    .phone("+91 9849033333")
                    .licenseNumber("TS07-2017-009876")
                    .status(DriverStatus.AVAILABLE)
                    .rating(new BigDecimal("4.75"))
                    .totalTripsCompleted(88)
                    .active(true)
                    .build());

            // Driver 4: Ramesh Yadav (25-Ton Flatbed) near Miyapur
            Driver d4 = driverRepository.save(Driver.builder()
                    .name("Ramesh Yadav")
                    .phone("+91 9849012345")
                    .licenseNumber("TS09-2015-004412")
                    .status(DriverStatus.AVAILABLE)
                    .rating(new BigDecimal("4.95"))
                    .totalTripsCompleted(320)
                    .active(true)
                    .build());

            VehicleTier tier1Ton = vehicleTierRepository.findById(VehicleTierCode.TIER_1_TON_PICKUP).orElseThrow();
            VehicleTier tier3_5Ton = vehicleTierRepository.findById(VehicleTierCode.TIER_3_5_TON_TRUCK).orElseThrow();
            VehicleTier tier25Ton = vehicleTierRepository.findById(VehicleTierCode.TIER_25_TON_TRAILER).orElseThrow();

            vehicleRepository.save(Vehicle.builder()
                    .vehicleNumber("TS 09 UB 1001")
                    .tier(tier1Ton)
                    .driver(d1)
                    .modelName("Tata Ace Gold Diesel")
                    .status(VehicleStatus.AVAILABLE)
                    .currentLatitude(17.3850)
                    .currentLongitude(78.4867)
                    .lastPingAt(LocalDateTime.now())
                    .active(true)
                    .build());

            vehicleRepository.save(Vehicle.builder()
                    .vehicleNumber("TS 08 AC 2002")
                    .tier(tier1Ton)
                    .driver(d2)
                    .modelName("Mahindra Bolero Maxi Truck Plus")
                    .status(VehicleStatus.AVAILABLE)
                    .currentLatitude(17.4933)
                    .currentLongitude(78.3914)
                    .lastPingAt(LocalDateTime.now())
                    .active(true)
                    .build());

            vehicleRepository.save(Vehicle.builder()
                    .vehicleNumber("TS 07 ED 3003")
                    .tier(tier3_5Ton)
                    .driver(d3)
                    .modelName("Eicher Pro 2049 Heavy Duty")
                    .status(VehicleStatus.AVAILABLE)
                    .currentLatitude(17.4483)
                    .currentLongitude(78.3915)
                    .lastPingAt(LocalDateTime.now())
                    .active(true)
                    .build());

            vehicleRepository.save(Vehicle.builder()
                    .vehicleNumber("TS 09 UB 4412")
                    .tier(tier25Ton)
                    .driver(d4)
                    .modelName("Ashok Leyland 2820 (22-Wheel Flatbed Trailer)")
                    .status(VehicleStatus.AVAILABLE)
                    .currentLatitude(17.4950)
                    .currentLongitude(78.3550)
                    .lastPingAt(LocalDateTime.now())
                    .active(true)
                    .build());

            log.info("Seeded 4 initial fleet vehicles and active drivers in fleet-service.");
        }
    }
}
