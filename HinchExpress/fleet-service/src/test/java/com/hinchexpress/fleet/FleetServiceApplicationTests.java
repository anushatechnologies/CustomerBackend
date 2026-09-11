package com.hinchexpress.fleet;

import com.hinchexpress.common.enums.VehicleTierCode;
import com.hinchexpress.common.dto.VehicleCandidateDto;
import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.service.FleetMatchingQueryService;
import com.hinchexpress.fleet.service.VehicleTierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FleetServiceApplicationTests {

    @Autowired
    private VehicleTierService vehicleTierService;

    @Autowired
    private FleetMatchingQueryService fleetMatchingQueryService;

    @Test
    @DisplayName("1. Context loads and initial seed tiers exist")
    void contextLoads() {
        List<VehicleTier> tiers = vehicleTierService.getAllTiers();
        assertThat(tiers).hasSize(5);
    }

    @Test
    @DisplayName("2. Weight filtering: 221 kg order matches all tiers >= 221 kg")
    void testWeightFilteringLightLoad() {
        List<VehicleTier> eligible = vehicleTierService.getEligibleTiers(new BigDecimal("221.000"));
        assertThat(eligible).hasSize(5);
    }

    @Test
    @DisplayName("3. Weight filtering: 2,500 kg order eliminates 3W and 1-Ton")
    void testWeightFilteringMediumLoad() {
        List<VehicleTier> eligible = vehicleTierService.getEligibleTiers(new BigDecimal("2500.000"));
        assertThat(eligible).hasSize(3);
        assertThat(eligible.stream().map(VehicleTier::getCode))
                .containsExactly(
                        VehicleTierCode.TIER_3_5_TON_TRUCK,
                        VehicleTierCode.TIER_10_TON_TIPPER,
                        VehicleTierCode.TIER_25_TON_TRAILER
                );
    }

    @Test
    @DisplayName("4. Weight filtering: 20,000 kg heavy structural steel requires 25-Ton Flatbed")
    void testWeightFilteringHeavyLoad() {
        List<VehicleTier> eligible = vehicleTierService.getEligibleTiers(new BigDecimal("20000.000"));
        assertThat(eligible).hasSize(1);
        assertThat(eligible.get(0).getCode()).isEqualTo(VehicleTierCode.TIER_25_TON_TRAILER);
    }

    @Test
    @DisplayName("5. Fleet Matching Query: ranks eligible candidates by proximity and capacity")
    void testFleetMatchingQuery() {
        // Order at store coords: (17.385044, 78.486671) with weight 221 kg
        List<VehicleCandidateDto> candidates = fleetMatchingQueryService.findEligibleCandidatesForDispatch(
                new BigDecimal("221.000"),
                17.385044,
                78.486671,
                30.0
        );

        assertThat(candidates).isNotEmpty();
        // Closest vehicle should be Rajesh Sharma (1-Ton Tata Ace at ~17.3850, 78.4867 - almost 0 km distance)
        VehicleCandidateDto first = candidates.get(0);
        assertThat(first.getDriverName()).isEqualTo("Rajesh Sharma");
        assertThat(first.getDistanceKmToPickup()).isLessThan(1.0);
    }
}
