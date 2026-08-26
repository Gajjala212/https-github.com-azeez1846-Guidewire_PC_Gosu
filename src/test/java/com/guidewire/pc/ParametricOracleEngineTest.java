package com.guidewire.pc;

import com.guidewire.pc.service.ParametricOracleEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parametric Catastrophe Oracle Platform Tests")
public class ParametricOracleEngineTest {

    @Test
    @DisplayName("Should trigger 100% indemnity payout when USGS ShakeMap PGA exceeds 0.65g")
    void testEarthquakeMajorTriggerBreach() {
        var res = ParametricOracleEngine.getInstance().evaluatePerilTrigger(
                "POL-PARAMETRIC-7701",
                "EARTHQUAKE_USGS",
                "San Francisco Waterfront Port, CA",
                37.7749,
                -122.4194,
                new BigDecimal("2000000.00"),
                0.72 // 0.72g PGA breaches 0.65g threshold
        );

        assertNotNull(res);
        assertTrue(res.triggerBreached);
        assertEquals(100.0, res.payoutPercentage);
        assertEquals(new BigDecimal("2000000.00"), res.instantIndemnityPayout);
        assertEquals("SETTLED_INSTANT_FEDNOW_WIRE", res.settlementStatus);
        assertTrue(res.wireTransferReference.startsWith("FEDWIRE-"));
        assertEquals(new BigDecimal("1500000.00"), res.reinsuranceRecoveryAmount); // 75% treaty recovery
    }

    @Test
    @DisplayName("Should not trigger payout when NOAA Hurricane pressure is above threshold")
    void testHurricaneNoBreach() {
        var res = ParametricOracleEngine.getInstance().evaluatePerilTrigger(
                "POL-PARAMETRIC-7702",
                "HURRICANE_NOAA",
                "Miami Port, FL",
                25.7617,
                -80.1918,
                new BigDecimal("1000000.00"),
                980.0 // 980 mbar is mild, threshold is <= 960 mbar
        );

        assertNotNull(res);
        assertFalse(res.triggerBreached);
        assertEquals(0.0, res.payoutPercentage);
        assertEquals(new BigDecimal("0.00"), res.instantIndemnityPayout);
        assertEquals("MONITORING_ACTIVE_SENSOR_FEED", res.settlementStatus);
    }
}
