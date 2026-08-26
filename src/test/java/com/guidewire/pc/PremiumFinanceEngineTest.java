package com.guidewire.pc;

import com.guidewire.pc.service.PremiumFinanceEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Commercial Premium Financing (PFC) & NOA Engine Tests")
public class PremiumFinanceEngineTest {

    @Test
    @DisplayName("Should generate down payment, APR interest schedule, and Notice of Assignment payload")
    void testPremiumFinanceAgreement() {
        var res = PremiumFinanceEngine.getInstance().createFinanceAgreementAndNOA(
                "POL-COMM-8001",
                new BigDecimal("60000.00"),
                "AFCO Premium Credit Corp",
                20.0, // 20% down ($12,000)
                9.00, // 9% APR
                9     // 9 installments
        );

        assertNotNull(res);
        assertEquals(new BigDecimal("12000.00"), res.downPaymentAmount);
        assertEquals(new BigDecimal("48000.00"), res.financedPrincipal);
        assertEquals(9, res.installmentCount);
        assertEquals(9, res.installmentSchedule.size());
        assertTrue(res.monthlyInstallmentAmount.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.noticeOfAssignmentId.startsWith("NOA-"));
        assertEquals("EXECUTED_ACTIVE_ASSIGNMENT", res.noaStatus);
        assertTrue(res.powerOfAttorneyGranted);
    }
}
