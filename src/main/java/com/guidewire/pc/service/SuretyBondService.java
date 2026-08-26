package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Surety Bond & Contractor Bonding Capacity Engine.
 */
public class SuretyBondService {
    private static final Logger LOGGER = Logger.getLogger(SuretyBondService.class.getName());
    private static final SuretyBondService INSTANCE = new SuretyBondService();

    public static SuretyBondService getInstance() {
        return INSTANCE;
    }

    public static class SuretyEvaluationResult {
        public String contractorName;
        public String bondType;
        public BigDecimal contractSum;
        public BigDecimal penalSum;
        public BigDecimal workingCapital;
        public BigDecimal netWorth;
        public BigDecimal singleJobCapacity;
        public BigDecimal aggregateCapacity;
        public BigDecimal activeWIP;
        public BigDecimal availableCapacity;
        public boolean capacityApproved;
        public BigDecimal calculatedPremium;
        public BigDecimal collateralHeld;
        public String obligeeName;
        public String obligeeType;
        public String underwritingStatus;
        public String notes;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("contractorName", contractorName);
            map.put("bondType", bondType);
            map.put("contractSum", contractSum);
            map.put("penalSum", penalSum);
            map.put("workingCapital", workingCapital);
            map.put("netWorth", netWorth);
            map.put("singleJobCapacity", singleJobCapacity);
            map.put("aggregateCapacity", aggregateCapacity);
            map.put("activeWIP", activeWIP);
            map.put("availableCapacity", availableCapacity);
            map.put("capacityApproved", capacityApproved);
            map.put("calculatedPremium", calculatedPremium);
            map.put("collateralHeld", collateralHeld);
            map.put("obligeeName", obligeeName);
            map.put("obligeeType", obligeeType);
            map.put("underwritingStatus", underwritingStatus);
            map.put("notes", notes);
            return map;
        }
    }

    public SuretyEvaluationResult evaluateBondAndCapacity(String contractorName,
                                                          String bondType,
                                                          BigDecimal contractSum,
                                                          BigDecimal workingCapital,
                                                          BigDecimal netWorth,
                                                          BigDecimal activeWIP,
                                                          BigDecimal collateralHeld,
                                                          String obligeeName,
                                                          String obligeeType) {
        LOGGER.log(Level.INFO, "Evaluating Surety Bond for contractor: {0}, type: {1}", new Object[]{contractorName, bondType});

        SuretyEvaluationResult res = new SuretyEvaluationResult();
        res.contractorName = (contractorName != null && !contractorName.isBlank()) ? contractorName : "Apex General Contractors LLC";
        res.bondType = (bondType != null && !bondType.isBlank()) ? bondType.toUpperCase() : "PERFORMANCE";
        res.contractSum = (contractSum != null && contractSum.compareTo(BigDecimal.ZERO) > 0) ? contractSum : new BigDecimal("1500000.00");
        res.penalSum = res.contractSum; // Standard 100% penal sum
        res.workingCapital = (workingCapital != null) ? workingCapital : new BigDecimal("350000.00");
        res.netWorth = (netWorth != null) ? netWorth : new BigDecimal("800000.00");
        res.activeWIP = (activeWIP != null) ? activeWIP : new BigDecimal("1200000.00");
        res.collateralHeld = (collateralHeld != null) ? collateralHeld : BigDecimal.ZERO;
        res.obligeeName = (obligeeName != null && !obligeeName.isBlank()) ? obligeeName : "State Dept of Transportation";
        res.obligeeType = (obligeeType != null && !obligeeType.isBlank()) ? obligeeType : "PUBLIC_STATE_MUNICIPAL";

        // 1. Calculate Single Job & Aggregate Capacity
        // Single Job = min(WC * 10, NW * 4)
        BigDecimal singleByWc = res.workingCapital.multiply(new BigDecimal("10"));
        BigDecimal singleByNw = res.netWorth.multiply(new BigDecimal("4"));
        res.singleJobCapacity = singleByWc.min(singleByNw).setScale(2, RoundingMode.HALF_UP);

        // Aggregate Capacity = min(WC * 15, NW * 7)
        BigDecimal aggByWc = res.workingCapital.multiply(new BigDecimal("15"));
        BigDecimal aggByNw = res.netWorth.multiply(new BigDecimal("7"));
        res.aggregateCapacity = aggByWc.min(aggByNw).setScale(2, RoundingMode.HALF_UP);

        res.availableCapacity = res.aggregateCapacity.subtract(res.activeWIP).setScale(2, RoundingMode.HALF_UP);

        // 2. Capacity Check
        boolean exceedsSingle = res.contractSum.compareTo(res.singleJobCapacity) > 0;
        boolean exceedsAgg = res.contractSum.compareTo(res.availableCapacity) > 0;

        if (exceedsSingle || exceedsAgg) {
            res.capacityApproved = false;
            res.underwritingStatus = "REFERRAL_CAPACITY_EXCEEDED";
            res.notes = String.format("Contract sum $%s exceeds limits (Single Max: $%s, Available Backlog: $%s). Collateral or UW override required.",
                    res.contractSum, res.singleJobCapacity, res.availableCapacity);
        } else {
            res.capacityApproved = true;
            res.underwritingStatus = "APPROVED_STP";
            res.notes = "Contractor capacity fully qualifies within underwriting parameters.";
        }

        // 3. Premium Rating
        BigDecimal premium = BigDecimal.ZERO;
        if ("BID".equals(res.bondType)) {
            premium = new BigDecimal("250.00"); // Flat bid bond filing charge
        } else if ("LICENSE_PERMIT".equals(res.bondType)) {
            // Flat 1.5% of penal sum, min $100
            premium = res.penalSum.multiply(new BigDecimal("0.015")).max(new BigDecimal("100.00"));
        } else {
            // Tiered Performance & Payment bond scale:
            // First $100k @ 2.5% ($25.00/k)
            // Next $400k @ 1.5% ($15.00/k)
            // Next $2M @ 1.0% ($10.00/k)
            // Over $2.5M @ 0.75% ($7.50/k)
            BigDecimal remaining = res.penalSum;
            BigDecimal t1 = remaining.min(new BigDecimal("100000.00"));
            premium = premium.add(t1.multiply(new BigDecimal("0.025")));
            remaining = remaining.subtract(t1);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal t2 = remaining.min(new BigDecimal("400000.00"));
                premium = premium.add(t2.multiply(new BigDecimal("0.015")));
                remaining = remaining.subtract(t2);
            }
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal t3 = remaining.min(new BigDecimal("2000000.00"));
                premium = premium.add(t3.multiply(new BigDecimal("0.010")));
                remaining = remaining.subtract(t3);
            }
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                premium = premium.add(remaining.multiply(new BigDecimal("0.0075")));
            }
        }

        // 4. Collateral Discount (15% credit if collateral >= 10% of contract)
        if (res.collateralHeld.compareTo(res.contractSum.multiply(new BigDecimal("0.10"))) >= 0) {
            premium = premium.multiply(new BigDecimal("0.85"));
            res.notes += " (15% Collateral Credit Applied)";
        }

        res.calculatedPremium = premium.setScale(2, RoundingMode.HALF_UP);
        return res;
    }
}
