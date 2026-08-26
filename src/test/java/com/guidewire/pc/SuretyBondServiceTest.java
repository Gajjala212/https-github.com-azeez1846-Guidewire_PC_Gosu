package com.guidewire.pc;

import com.guidewire.pc.service.SuretyBondService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Surety Bonds & Contractor Capacity Engine Tests")
public class SuretyBondServiceTest {

    @Test
    @DisplayName("Should approve performance bond when contractor meets single and aggregate capacity")
    void testApprovedBondCapacity() {
        var res = SuretyBondService.getInstance().evaluateBondAndCapacity(
                "Apex General Contractors",
                "PERFORMANCE",
                new BigDecimal("1500000.00"),
                new BigDecimal("450000.00"),  // WC -> 4.5M single
                new BigDecimal("900000.00"),  // NW -> 3.6M single -> Single Job Cap = 3.6M
                new BigDecimal("1000000.00"), // Active WIP
                BigDecimal.ZERO,
                "State DOT",
                "PUBLIC_STATE_MUNICIPAL"
        );

        assertNotNull(res);
        assertTrue(res.capacityApproved);
        assertEquals("APPROVED_STP", res.underwritingStatus);
        assertEquals(new BigDecimal("3600000.00"), res.singleJobCapacity);
        assertTrue(res.calculatedPremium.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should flag referral when contract sum exceeds available bonding backlog")
    void testCapacityExceededReferral() {
        var res = SuretyBondService.getInstance().evaluateBondAndCapacity(
                "Small Builder LLC",
                "PERFORMANCE",
                new BigDecimal("5000000.00"), // 5M contract exceeds 1M cap
                new BigDecimal("100000.00"),  // WC -> 1M single
                new BigDecimal("250000.00"),  // NW -> 1M single
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                "City Water Authority",
                "PUBLIC_STATE_MUNICIPAL"
        );

        assertNotNull(res);
        assertFalse(res.capacityApproved);
        assertEquals("REFERRAL_CAPACITY_EXCEEDED", res.underwritingStatus);
    }
}
