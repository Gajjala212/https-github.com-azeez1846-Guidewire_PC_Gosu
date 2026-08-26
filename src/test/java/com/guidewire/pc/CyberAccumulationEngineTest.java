package com.guidewire.pc;

import com.guidewire.pc.service.CyberAccumulationEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Commercial Cyber Risk & Cloud Outage Accumulation Engine Tests")
public class CyberAccumulationEngineTest {

    @Test
    @DisplayName("Should grant preferred tier and full ransomware limits for excellent posture")
    void testPreferredCyberPosture() {
        var res = CyberAccumulationEngine.getInstance().evaluateAndRate(
                "POL-CYBER-101",
                new BigDecimal("2000000.00"),
                true,   // MFA
                true,   // Offline backups
                true,   // EDR
                95.0,   // Phishing pass rate
                true,   // DNS filtering
                "AWS",
                new BigDecimal("15000000.00")
        );

        assertNotNull(res);
        assertEquals(100, res.securityPostureScore);
        assertEquals("PREFERRED_TIER_1", res.postureTier);
        assertEquals(0.80, res.postureModifier);
        assertEquals(new BigDecimal("2000000.00"), res.ransomwareSublimit);
        assertFalse(res.underwritingReferralRequired);
        assertTrue(res.finalRatedPremium.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should penalize and cap ransomware limits when MFA is absent")
    void testSubstandardCyberWithoutMFA() {
        var res = CyberAccumulationEngine.getInstance().evaluateAndRate(
                "POL-CYBER-102",
                new BigDecimal("2000000.00"),
                false,  // Missing MFA
                true,
                true,
                65.0,
                false,
                "ON_PREM",
                new BigDecimal("10000000.00")
        );

        assertNotNull(res);
        assertTrue(res.securityPostureScore < 60);
        assertTrue(res.underwritingReferralRequired);
        assertTrue(res.ransomwareSublimit.compareTo(new BigDecimal("2000000.00")) < 0);
    }
}
