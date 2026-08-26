package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AI Autonomous Submission Intake & ACORD Loss Run Parser 2.0 (5-Year Loss Triangles & Triage).
 */
public class AcordLossRunParserService {
    private static final Logger LOGGER = Logger.getLogger(AcordLossRunParserService.class.getName());
    private static final AcordLossRunParserService INSTANCE = new AcordLossRunParserService();

    public static AcordLossRunParserService getInstance() {
        return INSTANCE;
    }

    public static class LossRunYearData {
        public int policyYear;
        public int claimCount;
        public BigDecimal paidLosses;
        public BigDecimal outstandingReserves;
        public BigDecimal totalIncurred;
        public BigDecimal annualExposure;
        public double lossRatioPct;
    }

    public static class IntakeTriageResult {
        public String insuredName;
        public String lineOfBusiness;
        public int totalHistoricalClaims;
        public BigDecimal cumulativePaidLosses;
        public BigDecimal cumulativeOutstandingReserves;
        public BigDecimal cumulativeIncurredLosses;
        public double fiveYearLossRatioPct;
        public int largeLossCountOver50k;
        public int appetiteMatchScore;
        public String triageDecision;
        public String underwritingRouting;
        public List<LossRunYearData> fiveYearLossTriangle = new ArrayList<>();
        public List<String> underwritingFlags = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("insuredName", insuredName);
            map.put("lineOfBusiness", lineOfBusiness);
            map.put("totalHistoricalClaims", totalHistoricalClaims);
            map.put("cumulativePaidLosses", cumulativePaidLosses);
            map.put("cumulativeOutstandingReserves", cumulativeOutstandingReserves);
            map.put("cumulativeIncurredLosses", cumulativeIncurredLosses);
            map.put("fiveYearLossRatioPct", fiveYearLossRatioPct);
            map.put("largeLossCountOver50k", largeLossCountOver50k);
            map.put("appetiteMatchScore", appetiteMatchScore);
            map.put("triageDecision", triageDecision);
            map.put("underwritingRouting", underwritingRouting);
            map.put("fiveYearLossTriangle", fiveYearLossTriangle);
            map.put("underwritingFlags", underwritingFlags);
            return map;
        }
    }

    public IntakeTriageResult parseLossRunsAndTriage(String insuredName,
                                                     String lineOfBusiness,
                                                     BigDecimal estimatedAnnualPremium,
                                                     List<Map<String, Object>> annualLossData) {
        LOGGER.log(Level.INFO, "Parsing ACORD loss runs for insured: {0}, LOB: {1}", new Object[]{insuredName, lineOfBusiness});

        IntakeTriageResult res = new IntakeTriageResult();
        res.insuredName = (insuredName != null && !insuredName.isBlank()) ? insuredName : "Horizon Logistics & Warehousing Inc";
        res.lineOfBusiness = (lineOfBusiness != null && !lineOfBusiness.isBlank()) ? lineOfBusiness : "COMMERCIAL_AUTO";

        BigDecimal annualPrem = (estimatedAnnualPremium != null && estimatedAnnualPremium.compareTo(BigDecimal.ZERO) > 0)
                ? estimatedAnnualPremium : new BigDecimal("45000.00");

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalReserves = BigDecimal.ZERO;
        int totalClaims = 0;
        int largeLosses = 0;

        int currentYear = 2026;

        if (annualLossData == null || annualLossData.isEmpty()) {
            // Generate standard 5-year sample loss run history
            annualLossData = new ArrayList<>();
            annualLossData.add(Map.of("year", 2022, "claims", 1, "paid", 4200.0, "reserves", 0.0));
            annualLossData.add(Map.of("year", 2023, "claims", 0, "paid", 0.0, "reserves", 0.0));
            annualLossData.add(Map.of("year", 2024, "claims", 2, "paid", 12500.0, "reserves", 1500.0));
            annualLossData.add(Map.of("year", 2025, "claims", 1, "paid", 8000.0, "reserves", 2000.0));
            annualLossData.add(Map.of("year", 2026, "claims", 0, "paid", 0.0, "reserves", 0.0));
        }

        BigDecimal totalHistoricalPremium = annualPrem.multiply(BigDecimal.valueOf(annualLossData.size()));

        for (Map<String, Object> yr : annualLossData) {
            LossRunYearData d = new LossRunYearData();
            d.policyYear = yr.get("year") instanceof Number ? ((Number) yr.get("year")).intValue() : currentYear;
            d.claimCount = yr.get("claims") instanceof Number ? ((Number) yr.get("claims")).intValue() : 0;
            double p = yr.get("paid") instanceof Number ? ((Number) yr.get("paid")).doubleValue() : 0.0;
            double r = yr.get("reserves") instanceof Number ? ((Number) yr.get("reserves")).doubleValue() : 0.0;

            d.paidLosses = BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP);
            d.outstandingReserves = BigDecimal.valueOf(r).setScale(2, RoundingMode.HALF_UP);
            d.totalIncurred = d.paidLosses.add(d.outstandingReserves);
            d.annualExposure = annualPrem;

            if (annualPrem.compareTo(BigDecimal.ZERO) > 0) {
                d.lossRatioPct = d.totalIncurred.divide(annualPrem, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }

            if (d.totalIncurred.compareTo(new BigDecimal("50000.00")) >= 0) {
                largeLosses++;
            }

            totalClaims += d.claimCount;
            totalPaid = totalPaid.add(d.paidLosses);
            totalReserves = totalReserves.add(d.outstandingReserves);

            res.fiveYearLossTriangle.add(d);
        }

        res.totalHistoricalClaims = totalClaims;
        res.cumulativePaidLosses = totalPaid;
        res.cumulativeOutstandingReserves = totalReserves;
        res.cumulativeIncurredLosses = totalPaid.add(totalReserves);
        res.largeLossCountOver50k = largeLosses;

        if (totalHistoricalPremium.compareTo(BigDecimal.ZERO) > 0) {
            res.fiveYearLossRatioPct = Math.round(res.cumulativeIncurredLosses.divide(totalHistoricalPremium, 4, RoundingMode.HALF_UP).doubleValue() * 1000.0) / 10.0;
        }

        // Triage Scoring (0 - 100)
        int score = 100;
        if (res.fiveYearLossRatioPct > 70.0) {
            score -= 40;
            res.underwritingFlags.add("High 5-year Loss Ratio (" + res.fiveYearLossRatioPct + "% exceeds 70% threshold)");
        } else if (res.fiveYearLossRatioPct > 45.0) {
            score -= 20;
            res.underwritingFlags.add("Moderate Loss Ratio (" + res.fiveYearLossRatioPct + "%)");
        }

        if (largeLosses > 0) {
            score -= (largeLosses * 15);
            res.underwritingFlags.add(largeLosses + " Large Loss Claim(s) exceeding $50k severity");
        }

        if (totalClaims > 6) {
            score -= 20;
            res.underwritingFlags.add("Elevated Claim Frequency: " + totalClaims + " claims in 5-year window");
        }

        score = Math.max(10, Math.min(100, score));
        res.appetiteMatchScore = score;

        if (score >= 80) {
            res.triageDecision = "STRAIGHT_THROUGH_AUTO_BIND";
            res.underwritingRouting = "Standard Underwriting Desk (Fast-Track Approved)";
        } else if (score >= 60) {
            res.triageDecision = "STANDARD_UNDERWRITER_REVIEW";
            res.underwritingRouting = "Senior Commercial Underwriter (Level 2 Review)";
        } else {
            res.triageDecision = "MANDATORY_UW_REFERRAL_HIGH_HAZARD";
            res.underwritingRouting = "Regional Underwriting Director / Special Risk Committee";
        }

        return res;
    }
}
