package com.guidewire.pc;

import com.guidewire.pc.service.EffDatedTimelineVisualizerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EffDated Policy Period Timeline Slice Visualizer Tests")
public class EffDatedTimelineVisualizerTest {

    @Test
    @DisplayName("Should generate continuous date slices and forward-merge out-of-sequence endorsement")
    void testEffDatedTimelineGeneration() {
        var res = EffDatedTimelineVisualizerService.getInstance().generateTimelineSlices(
                "POL-COMM-9901",
                "2026-01-01",
                "2027-01-01",
                new BigDecimal("24000.00"),
                true // Include Out-of-Sequence retroactive change
        );

        assertNotNull(res);
        assertEquals(3, res.totalSlices);
        assertTrue(res.hasOutOfSequenceEndorsements);
        assertEquals(3, res.transactionHistory.size());
        assertTrue(res.totalTermEarnedPremium.compareTo(BigDecimal.ZERO) > 0);

        // Check slice 2 is marked as Out-of-Sequence
        var s2 = res.slices.get(1);
        assertTrue(s2.isOutOfSequenceSlice);
        assertEquals("2026-02-15", s2.sliceStartDate);
        assertEquals("2026-04-01", s2.sliceEndDate);
    }
}
