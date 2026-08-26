package com.guidewire.pc;

import com.guidewire.pc.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Guidewire PolicyCenter 2026 Enterprise Extensions Master Integration Suite")
public class EnterpriseSuite2026MasterIntegrationTest {

    @Test
    @DisplayName("Should verify end-to-end functionality of all 10 new enterprise engines")
    void testAllTenEnterpriseFeaturesEndToEnd() {
        // 1. Cyber Accumulation Engine
        var cyber = CyberAccumulationEngine.getInstance().evaluateAndRate(
                "POL-CYBER-MASTER", new BigDecimal("3000000.00"), true, true, true, 96.0, true, "AWS", new BigDecimal("25000000.00")
        );
        assertNotNull(cyber);
        assertEquals("PREFERRED_TIER_1", cyber.postureTier);

        // 2. Surety Bond Engine
        var surety = SuretyBondService.getInstance().evaluateBondAndCapacity(
                "Master Contractors Corp", "PERFORMANCE", new BigDecimal("2000000.00"),
                new BigDecimal("600000.00"), new BigDecimal("1200000.00"), new BigDecimal("1500000.00"),
                BigDecimal.ZERO, "Federal Highway Admin", "PUBLIC_FEDERAL"
        );
        assertNotNull(surety);
        assertTrue(surety.capacityApproved);

        // 3. Ocean Marine Rating Engine
        var marine = OceanMarineRatingEngine.getInstance().rateVoyageCargo(
                "POL-MAR-MASTER", new BigDecimal("850000.00"), "GENERAL_DRY", "CLAUSES_A_ALL_RISK",
                "MV Atlantic Voyager", 6, "PANAMA", true, false, new BigDecimal("1500000.00")
        );
        assertNotNull(marine);
        assertTrue(marine.portAccumulationApproved);

        // 4. Portfolio Simulation Engine
        var sim = PortfolioSimulationService.getInstance().simulateRateChanges(7.5, 3.0, 10.0, -1.5, 12.0);
        assertNotNull(sim);
        assertTrue(sim.projectedSimulatedPremium.compareTo(sim.currentInForcePremium) > 0);

        // 5. ACORD Loss Run Parser 2.0
        var intake = AcordLossRunParserService.getInstance().parseLossRunsAndTriage(
                "Apex Freight Master Inc", "COMMERCIAL_AUTO", new BigDecimal("60000.00"),
                List.of(
                        Map.of("year", 2022, "claims", 1, "paid", 2000.0, "reserves", 0.0),
                        Map.of("year", 2023, "claims", 0, "paid", 0.0, "reserves", 0.0),
                        Map.of("year", 2024, "claims", 1, "paid", 4500.0, "reserves", 500.0),
                        Map.of("year", 2025, "claims", 0, "paid", 0.0, "reserves", 0.0),
                        Map.of("year", 2026, "claims", 0, "paid", 0.0, "reserves", 0.0)
                )
        );
        assertNotNull(intake);
        assertEquals("STRAIGHT_THROUGH_AUTO_BIND", intake.triageDecision);

        // 6. Parametric Catastrophe Oracle
        var oracle = ParametricOracleEngine.getInstance().evaluatePerilTrigger(
                "POL-PARAM-MASTER", "EARTHQUAKE_USGS", "Oakland Seaport, CA",
                37.8044, -122.2712, new BigDecimal("5000000.00"), 0.85
        );
        assertNotNull(oracle);
        assertTrue(oracle.triggerBreached);
        assertEquals(100.0, oracle.payoutPercentage);

        // 7. EffDated Timeline Visualizer
        var eff = EffDatedTimelineVisualizerService.getInstance().generateTimelineSlices(
                "POL-EFF-MASTER", "2026-01-01", "2027-01-01", new BigDecimal("36000.00"), true
        );
        assertNotNull(eff);
        assertEquals(3, eff.totalSlices);

        // 8. UW Deal Room
        var deal = UWDealRoomService.getInstance().createOrEscalateDeal(
                "POL-DEAL-MASTER", "Mega Enterprise Holdings", new BigDecimal("800000.00"),
                new BigDecimal("12000000.00"), "Referral for $12M multi-location umbrella.", null
        );
        assertNotNull(deal);
        var approvedDeal = UWDealRoomService.getInstance().performAction(
                "POL-DEAL-MASTER", "APPROVE", "Chief Underwriter", "CUO", "Approved by Executive Committee"
        );
        assertEquals("APPROVED_WITH_CONDITIONS", approvedDeal.currentStatus);

        // 9. Premium Finance Engine
        var pfc = PremiumFinanceEngine.getInstance().createFinanceAgreementAndNOA(
                "POL-PFC-MASTER", new BigDecimal("75000.00"), "IPFS Corporation", 25.0, 9.25, 10
        );
        assertNotNull(pfc);
        assertEquals(10, pfc.installmentCount);

        // 10. Gosu REPL Engine
        var repl = GosuReplEngine.getInstance().evaluateGosuScript("return 42;", new BigDecimal("500000.00"), 200);
        assertNotNull(repl);
        assertEquals("SUCCESS", repl.evaluationStatus);
    }
}
