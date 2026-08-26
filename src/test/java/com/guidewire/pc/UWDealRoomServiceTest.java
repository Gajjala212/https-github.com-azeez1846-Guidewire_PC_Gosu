package com.guidewire.pc;

import com.guidewire.pc.service.UWDealRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Underwriter Collaborative Deal Room Service Tests")
public class UWDealRoomServiceTest {

    @Test
    @DisplayName("Should route high-capacity commercial deals to CUO and approve with digital audit signature")
    void testDealRoomEscalationAndApproval() {
        var deal = UWDealRoomService.getInstance().createOrEscalateDeal(
                "POL-DEAL-9999",
                "Titan Heavy Manufacturing Corp",
                new BigDecimal("1500000.00"), // High premium -> CUO authority
                new BigDecimal("15000000.00"),
                "Escalated for senior review due to $15M aggregate limits.",
                "Subjective: Satisfactory Loss Control survey within 45 days."
        );

        assertNotNull(deal);
        assertEquals("CHIEF_UNDERWRITING_OFFICER", deal.requiredAuthorityLevel);
        assertEquals("PENDING_AUTHORITY_REVIEW", deal.currentStatus);
        assertFalse(deal.subjectiveConditions.isEmpty());

        // Perform CUO Approval
        var updated = UWDealRoomService.getInstance().performAction(
                "POL-DEAL-9999",
                "APPROVE",
                "Victoria Vance",
                "CHIEF_UNDERWRITING_OFFICER",
                "Sign-off approved under CUO delegated authority."
        );

        assertEquals("APPROVED_WITH_CONDITIONS", updated.currentStatus);
        assertNotNull(updated.approvedBy);
        assertTrue(updated.digitalAuditStamp.startsWith("AUDIT-SIG-"));
    }
}
