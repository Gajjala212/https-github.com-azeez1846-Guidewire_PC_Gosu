package com.guidewire.pc;

import com.guidewire.pc.service.OceanMarineRatingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ocean Marine & Open Cargo Rating Engine Tests")
public class OceanMarineRatingEngineTest {

    @Test
    @DisplayName("Should correctly calculate CIF+10% cargo value and Institute Clauses A rate")
    void testStandardCargoVoyageRating() {
        var res = OceanMarineRatingEngine.getInstance().rateVoyageCargo(
                "POL-MAR-5001",
                new BigDecimal("1000000.00"),
                "GENERAL_DRY",
                "CLAUSES_A_ALL_RISK",
                "MV Pacific Navigator",
                8,
                "MARSHALL_ISLANDS",
                true,   // War & Strikes
                false,  // Piracy
                new BigDecimal("2000000.00")
        );

        assertNotNull(res);
        assertEquals(new BigDecimal("1100000.00"), res.insuredCargoValue);
        assertTrue(res.ratedPremium.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.portAccumulationApproved);
    }

    @Test
    @DisplayName("Should detect port accumulation limit breach when total seaport cargo exceeds $5M")
    void testPortAccumulationBreach() {
        var res = OceanMarineRatingEngine.getInstance().rateVoyageCargo(
                "POL-MAR-5002",
                new BigDecimal("4000000.00"),
                "ELECTRONICS",
                "CLAUSES_A_ALL_RISK",
                "MV Ocean Titan",
                22, // Over-age vessel surcharge
                "LIBERIA",
                true,
                true, // Piracy corridor
                new BigDecimal("3000000.00") // Existing 3M + 4.4M = 7.4M > 5M limit
        );

        assertNotNull(res);
        assertFalse(res.portAccumulationApproved);
        assertTrue(res.surchargePct.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.underwritingNotes.contains("PORT ACCUMULATION ALERT"));
    }
}
