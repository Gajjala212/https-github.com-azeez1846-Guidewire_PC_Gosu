package com.guidewire.pc.service;

import com.guidewire.pc.model.PolicyPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Actuarial Rate Revision & Portfolio "What-If" Impact Simulator.
 */
public class PortfolioSimulationService {
    private static final Logger LOGGER = Logger.getLogger(PortfolioSimulationService.class.getName());
    private static final PortfolioSimulationService INSTANCE = new PortfolioSimulationService();

    public static PortfolioSimulationService getInstance() {
        return INSTANCE;
    }

    public static class SimulationResult {
        public int totalPoliciesSimulated;
        public BigDecimal currentInForcePremium;
        public BigDecimal projectedSimulatedPremium;
        public BigDecimal deltaPremiumVariance;
        public double aggregateRateChangePct;
        public double projectedRetentionRatePct;
        public double currentPortfolioLossRatioPct;
        public double projectedPortfolioLossRatioPct;
        public List<Map<String, Object>> lineItemBreakdown = new ArrayList<>();
        public Map<String, Object> quartileSummary = new HashMap<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalPoliciesSimulated", totalPoliciesSimulated);
            map.put("currentInForcePremium", currentInForcePremium);
            map.put("projectedSimulatedPremium", projectedSimulatedPremium);
            map.put("deltaPremiumVariance", deltaPremiumVariance);
            map.put("aggregateRateChangePct", aggregateRateChangePct);
            map.put("projectedRetentionRatePct", projectedRetentionRatePct);
            map.put("currentPortfolioLossRatioPct", currentPortfolioLossRatioPct);
            map.put("projectedPortfolioLossRatioPct", projectedPortfolioLossRatioPct);
            map.put("lineItemBreakdown", lineItemBreakdown);
            map.put("quartileSummary", quartileSummary);
            return map;
        }
    }

    public SimulationResult simulateRateChanges(double propertyRateDeltaPct,
                                                double generalLiabilityDeltaPct,
                                                double commercialAutoDeltaPct,
                                                double workersCompDeltaPct,
                                                double cyberDeltaPct) {
        LOGGER.log(Level.INFO, "Simulating portfolio rate impact: CP={0}%, GL={1}%, CA={2}%, WC={3}%, Cyber={4}%",
                new Object[]{propertyRateDeltaPct, generalLiabilityDeltaPct, commercialAutoDeltaPct, workersCompDeltaPct, cyberDeltaPct});

        SimulationResult res = new SimulationResult();
        List<PolicyPeriod> submissions = DataStoreService.getInstance().getSubmissions();

        BigDecimal curTotal = BigDecimal.ZERO;
        BigDecimal projTotal = BigDecimal.ZERO;
        int count = 0;

        // Populate sample baseline if store is fresh
        if (submissions == null || submissions.isEmpty()) {
            count = 250;
            curTotal = new BigDecimal("4500000.00");
        } else {
            count = submissions.size();
            for (PolicyPeriod p : submissions) {
                BigDecimal prem = p.getTotalPremium() != null ? p.getTotalPremium() : new BigDecimal("12500.00");
                curTotal = curTotal.add(prem);
            }
        }

        // Apply line-weighted simulation
        double avgRateDelta = (propertyRateDeltaPct * 0.35) +
                              (generalLiabilityDeltaPct * 0.25) +
                              (commercialAutoDeltaPct * 0.20) +
                              (workersCompDeltaPct * 0.15) +
                              (cyberDeltaPct * 0.05);

        BigDecimal multiplier = BigDecimal.valueOf(1.0 + (avgRateDelta / 100.0));
        projTotal = curTotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        res.totalPoliciesSimulated = count;
        res.currentInForcePremium = curTotal.setScale(2, RoundingMode.HALF_UP);
        res.projectedSimulatedPremium = projTotal;
        res.deltaPremiumVariance = projTotal.subtract(curTotal).setScale(2, RoundingMode.HALF_UP);
        res.aggregateRateChangePct = Math.round(avgRateDelta * 100.0) / 100.0;

        // Price elasticity of renewal retention: Retention = 90% * e^(-0.25 * DeltaRate)
        double rateImpactFrac = Math.max(0, avgRateDelta / 100.0);
        double retention = 92.0 * Math.exp(-0.35 * rateImpactFrac);
        res.projectedRetentionRatePct = Math.round(retention * 10.0) / 10.0;

        // Loss ratio projection: Current LR 58.5% -> Projected = 58.5% / (1 + delta)
        double curLR = 58.5;
        double projLR = curLR / (1.0 + (avgRateDelta / 100.0));
        res.currentPortfolioLossRatioPct = curLR;
        res.projectedPortfolioLossRatioPct = Math.round(projLR * 10.0) / 10.0;

        // Line Breakdown
        addLineItem(res, "Commercial Property", propertyRateDeltaPct, curTotal.multiply(new BigDecimal("0.35")));
        addLineItem(res, "General Liability", generalLiabilityDeltaPct, curTotal.multiply(new BigDecimal("0.25")));
        addLineItem(res, "Commercial Auto", commercialAutoDeltaPct, curTotal.multiply(new BigDecimal("0.20")));
        addLineItem(res, "Workers Compensation", workersCompDeltaPct, curTotal.multiply(new BigDecimal("0.15")));
        addLineItem(res, "Cyber Risk", cyberDeltaPct, curTotal.multiply(new BigDecimal("0.05")));

        res.quartileSummary.put("topDecilePremiumIncrease", res.deltaPremiumVariance.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP));
        res.quartileSummary.put("medianPolicyImpactDollar", res.deltaPremiumVariance.divide(BigDecimal.valueOf(Math.max(1, count)), 2, RoundingMode.HALF_UP));
        res.quartileSummary.put("projectedNetCarrierMargin", projTotal.multiply(BigDecimal.valueOf((100.0 - projLR) / 100.0)).setScale(2, RoundingMode.HALF_UP));

        return res;
    }

    private void addLineItem(SimulationResult res, String lineName, double deltaPct, BigDecimal currentLinePrem) {
        BigDecimal proj = currentLinePrem.multiply(BigDecimal.valueOf(1.0 + (deltaPct / 100.0))).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> item = new HashMap<>();
        item.put("lineName", lineName);
        item.put("rateChangePct", deltaPct);
        item.put("currentPremium", currentLinePrem.setScale(2, RoundingMode.HALF_UP));
        item.put("projectedPremium", proj);
        item.put("variance", proj.subtract(currentLinePrem).setScale(2, RoundingMode.HALF_UP));
        res.lineItemBreakdown.add(item);
    }
}
