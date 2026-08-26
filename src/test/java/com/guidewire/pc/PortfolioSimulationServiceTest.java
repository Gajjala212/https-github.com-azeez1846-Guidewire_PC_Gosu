package com.guidewire.pc;

import com.guidewire.pc.service.PortfolioSimulationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Actuarial Rate Revision & Portfolio Impact Simulator Tests")
public class PortfolioSimulationServiceTest {

    @Test
    @DisplayName("Should project earned premium variance, price elasticity retention, and loss ratios")
    void testRateSimulation() {
        var res = PortfolioSimulationService.getInstance().simulateRateChanges(
                8.0,   // Property +8%
                4.5,   // GL +4.5%
                12.0,  // Auto +12%
                -2.0,  // WC -2%
                15.0   // Cyber +15%
        );

        assertNotNull(res);
        assertTrue(res.totalPoliciesSimulated > 0);
        assertTrue(res.currentInForcePremium.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.projectedSimulatedPremium.compareTo(res.currentInForcePremium) > 0);
        assertTrue(res.deltaPremiumVariance.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.projectedRetentionRatePct > 70.0 && res.projectedRetentionRatePct <= 100.0);
        assertEquals(5, res.lineItemBreakdown.size());
    }
}
