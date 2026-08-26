package com.guidewire.pc;

import com.guidewire.pc.service.GosuReplEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gosu Rules REPL & Virtual Thread Benchmark Tests")
public class GosuReplEngineTest {

    @Test
    @DisplayName("Should evaluate rating rule and benchmark Loom virtual thread performance")
    void testGosuReplEvaluationAndBenchmark() {
        var res = GosuReplEngine.getInstance().evaluateGosuScript(
                "var baseRate = 0.35;\nreturn period.TotalPremium * baseRate;",
                new BigDecimal("1000000.00"),
                500
        );

        assertNotNull(res);
        assertTrue(res.syntaxValid);
        assertEquals("SUCCESS", res.evaluationStatus);
        assertNotNull(res.computedOutput);
        assertTrue(res.virtualThreadExecutionMicros > 0);
        assertTrue(res.platformThreadExecutionMicros > 0);
        assertTrue(res.speedupRatio >= 0.0);
    }
}
