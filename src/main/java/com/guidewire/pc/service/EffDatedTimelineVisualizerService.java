package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interactive EffDated Policy Period Slice Visualizer & Out-of-Sequence Time-Travel Explorer.
 */
public class EffDatedTimelineVisualizerService {
    private static final Logger LOGGER = Logger.getLogger(EffDatedTimelineVisualizerService.class.getName());
    private static final EffDatedTimelineVisualizerService INSTANCE = new EffDatedTimelineVisualizerService();

    public static EffDatedTimelineVisualizerService getInstance() {
        return INSTANCE;
    }

    public static class PolicySliceNode {
        public int sliceIndex;
        public String sliceStartDate;
        public String sliceEndDate;
        public long durationDays;
        public BigDecimal sliceAnnualizedPremium;
        public BigDecimal sliceProratedEarnedPremium;
        public int vehicleCount;
        public int locationCount;
        public BigDecimal liabilityLimit;
        public boolean isOutOfSequenceSlice;
        public String sliceDescription;
    }

    public static class EffDatedTimelineResult {
        public String policyNumber;
        public String termInceptionDate;
        public String termExpirationDate;
        public long totalTermDays;
        public int totalSlices;
        public boolean hasOutOfSequenceEndorsements;
        public BigDecimal totalTermEarnedPremium;
        public List<PolicySliceNode> slices = new ArrayList<>();
        public List<Map<String, Object>> transactionHistory = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("policyNumber", policyNumber);
            map.put("termInceptionDate", termInceptionDate);
            map.put("termExpirationDate", termExpirationDate);
            map.put("totalTermDays", totalTermDays);
            map.put("totalSlices", totalSlices);
            map.put("hasOutOfSequenceEndorsements", hasOutOfSequenceEndorsements);
            map.put("totalTermEarnedPremium", totalTermEarnedPremium);
            map.put("slices", slices);
            map.put("transactionHistory", transactionHistory);
            return map;
        }
    }

    public EffDatedTimelineResult generateTimelineSlices(String policyNumber,
                                                         String inceptionDateStr,
                                                         String expirationDateStr,
                                                         BigDecimal baseAnnualPremium,
                                                         boolean includeOosEndorsement) {
        LOGGER.log(Level.INFO, "Generating EffDated Timeline Slices for policy: {0}", policyNumber);

        EffDatedTimelineResult res = new EffDatedTimelineResult();
        res.policyNumber = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-COMM-9901";
        res.termInceptionDate = (inceptionDateStr != null && !inceptionDateStr.isBlank()) ? inceptionDateStr : "2026-01-01";
        res.termExpirationDate = (expirationDateStr != null && !expirationDateStr.isBlank()) ? expirationDateStr : "2027-01-01";

        LocalDate inc = LocalDate.parse(res.termInceptionDate);
        LocalDate exp = LocalDate.parse(res.termExpirationDate);
        res.totalTermDays = ChronoUnit.DAYS.between(inc, exp);
        if (res.totalTermDays <= 0) res.totalTermDays = 365;

        BigDecimal baseAnnual = (baseAnnualPremium != null && baseAnnualPremium.compareTo(BigDecimal.ZERO) > 0)
                ? baseAnnualPremium : new BigDecimal("24000.00");

        res.hasOutOfSequenceEndorsements = includeOosEndorsement;

        // Transaction 1: Inception New Business
        res.transactionHistory.add(Map.of(
                "jobType", "SUBMISSION",
                "jobEffectiveDate", "2026-01-01",
                "systemAppliedDate", "2026-01-01",
                "description", "Initial Policy Inception (Standard Commercial Terms)"
        ));

        // Transaction 2: Mid-Term Endorsement
        res.transactionHistory.add(Map.of(
                "jobType", "POLICY_CHANGE",
                "jobEffectiveDate", "2026-04-01",
                "systemAppliedDate", "2026-04-01",
                "description", "Endorsement #1: Added 2 Fleet Vehicles (+$4,000/yr)"
        ));

        if (includeOosEndorsement) {
            // Out of sequence: Applied on 2026-07-01 but effective retroactively on 2026-02-15
            res.transactionHistory.add(Map.of(
                    "jobType", "OOS_RETROACTIVE_CHANGE",
                    "jobEffectiveDate", "2026-02-15",
                    "systemAppliedDate", "2026-07-01",
                    "description", "OOS Endorsement #2: Retroactive Building Addition at Location 2 (Effective Feb 15, Applied Jul 1)"
            ));
        }

        // Build Continuous Date Slices
        BigDecimal totalEarned = BigDecimal.ZERO;

        if (!includeOosEndorsement) {
            // Slice 1: Jan 1 to Apr 1 (90 days)
            PolicySliceNode s1 = createSlice(1, "2026-01-01", "2026-04-01", res.totalTermDays, baseAnnual, 5, 2, new BigDecimal("1000000.00"), false, "Inception Baseline");
            // Slice 2: Apr 1 to Jan 1 (275 days)
            BigDecimal modifiedAnnual = baseAnnual.add(new BigDecimal("4000.00"));
            PolicySliceNode s2 = createSlice(2, "2026-04-01", "2027-01-01", res.totalTermDays, modifiedAnnual, 7, 2, new BigDecimal("1000000.00"), false, "Fleet Addition Slice");

            res.slices.add(s1);
            res.slices.add(s2);
            totalEarned = s1.sliceProratedEarnedPremium.add(s2.sliceProratedEarnedPremium);
        } else {
            // Slice 1: Jan 1 to Feb 15 (45 days)
            PolicySliceNode s1 = createSlice(1, "2026-01-01", "2026-02-15", res.totalTermDays, baseAnnual, 5, 2, new BigDecimal("1000000.00"), false, "Inception Initial Slice");
            // Slice 2: Feb 15 to Apr 1 (45 days) - Retroactive OOS
            BigDecimal retroAnnual = baseAnnual.add(new BigDecimal("3000.00"));
            PolicySliceNode s2 = createSlice(2, "2026-02-15", "2026-04-01", res.totalTermDays, retroAnnual, 5, 3, new BigDecimal("1000000.00"), true, "Retroactive OOS Building Addition Slice");
            // Slice 3: Apr 1 to Jan 1 (275 days) - Merged forward
            BigDecimal fullAnnual = baseAnnual.add(new BigDecimal("7000.00"));
            PolicySliceNode s3 = createSlice(3, "2026-04-01", "2027-01-01", res.totalTermDays, fullAnnual, 7, 3, new BigDecimal("1000000.00"), false, "Forward-Merged Multi-Exposure Slice");

            res.slices.add(s1);
            res.slices.add(s2);
            res.slices.add(s3);
            totalEarned = s1.sliceProratedEarnedPremium.add(s2.sliceProratedEarnedPremium).add(s3.sliceProratedEarnedPremium);
        }

        res.totalSlices = res.slices.size();
        res.totalTermEarnedPremium = totalEarned.setScale(2, RoundingMode.HALF_UP);

        return res;
    }

    private PolicySliceNode createSlice(int idx, String start, String end, long totalDays, BigDecimal annualPrem,
                                        int vehicles, int locations, BigDecimal limit, boolean oos, String desc) {
        PolicySliceNode n = new PolicySliceNode();
        n.sliceIndex = idx;
        n.sliceStartDate = start;
        n.sliceEndDate = end;
        n.durationDays = ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end));
        n.sliceAnnualizedPremium = annualPrem.setScale(2, RoundingMode.HALF_UP);

        BigDecimal proRataFraction = BigDecimal.valueOf((double) n.durationDays / (double) totalDays);
        n.sliceProratedEarnedPremium = annualPrem.multiply(proRataFraction).setScale(2, RoundingMode.HALF_UP);
        n.vehicleCount = vehicles;
        n.locationCount = locations;
        n.liabilityLimit = limit;
        n.isOutOfSequenceSlice = oos;
        n.sliceDescription = desc;
        return n;
    }
}
