package com.guidewire.pc;

import com.guidewire.pc.service.AcordLossRunParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ACORD Loss Run Parser 2.0 & Loss Triangles Triage Tests")
public class AcordLossRunParserServiceTest {

    @Test
    @DisplayName("Should build 5-year loss triangle and triage clean loss history for straight-through binding")
    void testCleanLossRunTriage() {
        List<Map<String, Object>> lossHistory = List.of(
                Map.of("year", 2022, "claims", 0, "paid", 0.0, "reserves", 0.0),
                Map.of("year", 2023, "claims", 1, "paid", 2500.0, "reserves", 0.0),
                Map.of("year", 2024, "claims", 0, "paid", 0.0, "reserves", 0.0),
                Map.of("year", 2025, "claims", 1, "paid", 3000.0, "reserves", 0.0),
                Map.of("year", 2026, "claims", 0, "paid", 0.0, "reserves", 0.0)
        );

        var res = AcordLossRunParserService.getInstance().parseLossRunsAndTriage(
                "Metro Express Couriers",
                "COMMERCIAL_AUTO",
                new BigDecimal("50000.00"),
                lossHistory
        );

        assertNotNull(res);
        assertEquals(5, res.fiveYearLossTriangle.size());
        assertEquals(2, res.totalHistoricalClaims);
        assertEquals(new BigDecimal("5500.00"), res.cumulativePaidLosses);
        assertTrue(res.appetiteMatchScore >= 80);
        assertEquals("STRAIGHT_THROUGH_AUTO_BIND", res.triageDecision);
    }

    @Test
    @DisplayName("Should flag mandatory referral when loss history has high loss ratio and large losses")
    void testSevereLossHistoryTriage() {
        List<Map<String, Object>> lossHistory = List.of(
                Map.of("year", 2022, "claims", 3, "paid", 45000.0, "reserves", 10000.0),
                Map.of("year", 2023, "claims", 2, "paid", 65000.0, "reserves", 20000.0), // Large loss > 50k
                Map.of("year", 2024, "claims", 4, "paid", 35000.0, "reserves", 5000.0),
                Map.of("year", 2025, "claims", 3, "paid", 80000.0, "reserves", 25000.0), // Large loss > 50k
                Map.of("year", 2026, "claims", 1, "paid", 12000.0, "reserves", 3000.0)
        );

        var res = AcordLossRunParserService.getInstance().parseLossRunsAndTriage(
                "High Risk Heavy Haul Inc",
                "COMMERCIAL_AUTO",
                new BigDecimal("40000.00"),
                lossHistory
        );

        assertNotNull(res);
        assertTrue(res.largeLossCountOver50k >= 2);
        assertTrue(res.fiveYearLossRatioPct > 70.0);
        assertEquals("MANDATORY_UW_REFERRAL_HIGH_HAZARD", res.triageDecision);
    }
}
