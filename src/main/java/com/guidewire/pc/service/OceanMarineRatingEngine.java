package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Ocean Marine & Open Cargo Transit Rating Engine.
 */
public class OceanMarineRatingEngine {
    private static final Logger LOGGER = Logger.getLogger(OceanMarineRatingEngine.class.getName());
    private static final OceanMarineRatingEngine INSTANCE = new OceanMarineRatingEngine();

    public static OceanMarineRatingEngine getInstance() {
        return INSTANCE;
    }

    public static class MarineRatingResult {
        public String policyNumber;
        public String commodityType;
        public BigDecimal invoiceValue;
        public BigDecimal insuredCargoValue;
        public String clauseType;
        public String vesselName;
        public int vesselAge;
        public String flagState;
        public boolean warStrikesIncluded;
        public boolean piracyCorridorIncluded;
        public BigDecimal baseMarineRatePct;
        public BigDecimal surchargePct;
        public BigDecimal ratedPremium;
        public BigDecimal portAccumulationLimit;
        public boolean portAccumulationApproved;
        public String underwritingNotes;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("policyNumber", policyNumber);
            map.put("commodityType", commodityType);
            map.put("invoiceValue", invoiceValue);
            map.put("insuredCargoValue", insuredCargoValue);
            map.put("clauseType", clauseType);
            map.put("vesselName", vesselName);
            map.put("vesselAge", vesselAge);
            map.put("flagState", flagState);
            map.put("warStrikesIncluded", warStrikesIncluded);
            map.put("piracyCorridorIncluded", piracyCorridorIncluded);
            map.put("baseMarineRatePct", baseMarineRatePct);
            map.put("surchargePct", surchargePct);
            map.put("ratedPremium", ratedPremium);
            map.put("portAccumulationLimit", portAccumulationLimit);
            map.put("portAccumulationApproved", portAccumulationApproved);
            map.put("underwritingNotes", underwritingNotes);
            return map;
        }
    }

    public MarineRatingResult rateVoyageCargo(String policyNumber,
                                             BigDecimal invoiceValue,
                                             String commodityType,
                                             String clauseType,
                                             String vesselName,
                                             int vesselAge,
                                             String flagState,
                                             boolean warStrikes,
                                             boolean piracyCorridor,
                                             BigDecimal existingPortExposure) {
        LOGGER.log(Level.INFO, "Rating Ocean Marine Cargo voyage on vessel: {0}", vesselName);

        MarineRatingResult res = new MarineRatingResult();
        res.policyNumber = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-MAR-5001";
        res.invoiceValue = (invoiceValue != null && invoiceValue.compareTo(BigDecimal.ZERO) > 0) ? invoiceValue : new BigDecimal("750000.00");
        res.commodityType = (commodityType != null && !commodityType.isBlank()) ? commodityType.toUpperCase() : "GENERAL_DRY";
        res.clauseType = (clauseType != null && !clauseType.isBlank()) ? clauseType.toUpperCase() : "CLAUSES_A_ALL_RISK";
        res.vesselName = (vesselName != null && !vesselName.isBlank()) ? vesselName : "MV Pacific Navigator";
        res.vesselAge = (vesselAge > 0) ? vesselAge : 8;
        res.flagState = (flagState != null && !flagState.isBlank()) ? flagState.toUpperCase() : "MARSHALL_ISLANDS";
        res.warStrikesIncluded = warStrikes;
        res.piracyCorridorIncluded = piracyCorridor;
        res.portAccumulationLimit = new BigDecimal("5000000.00");

        // 1. Calculate Insured Cargo Value (CIF + 10%)
        res.insuredCargoValue = res.invoiceValue.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);

        // 2. Base Clause Rate %
        double baseRate;
        switch (res.clauseType) {
            case "CLAUSES_C_MAJOR_CASUALTY":
                baseRate = 0.0020; // 0.20%
                break;
            case "CLAUSES_B_NAMED":
                baseRate = 0.0030; // 0.30%
                break;
            case "CLAUSES_A_ALL_RISK":
            default:
                baseRate = 0.0045; // 0.45%
                break;
        }

        // 3. Commodity Factor
        double commodityMultiplier = 1.00;
        if ("PERISHABLES".equals(res.commodityType) || "REEFER".equals(res.commodityType)) {
            commodityMultiplier = 1.35;
        } else if ("ELECTRONICS".equals(res.commodityType) || "HIGH_THEFT".equals(res.commodityType)) {
            commodityMultiplier = 1.50;
        } else if ("CRUDE_OIL".equals(res.commodityType) || "HAZARDOUS_BULK".equals(res.commodityType)) {
            commodityMultiplier = 1.25;
        }

        double finalRate = baseRate * commodityMultiplier;
        double surcharges = 0.0;

        // 4. Over-age Vessel Surcharge (>20 years = +25%)
        if (res.vesselAge > 20) {
            surcharges += 0.25;
        }

        // 5. War & Strikes (+0.12%)
        if (warStrikes) {
            finalRate += 0.0012;
        }

        // 6. Piracy Corridor (+0.35%)
        if (piracyCorridor) {
            finalRate += 0.0035;
        }

        res.baseMarineRatePct = BigDecimal.valueOf(finalRate * 100).setScale(3, RoundingMode.HALF_UP);
        res.surchargePct = BigDecimal.valueOf(surcharges * 100).setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectiveRate = BigDecimal.valueOf(finalRate * (1.0 + surcharges));
        res.ratedPremium = res.insuredCargoValue.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        // 7. Port Accumulation Catastrophe Check
        BigDecimal portExp = (existingPortExposure != null) ? existingPortExposure : BigDecimal.ZERO;
        BigDecimal totalPortAccumulation = portExp.add(res.insuredCargoValue);
        res.portAccumulationApproved = totalPortAccumulation.compareTo(res.portAccumulationLimit) <= 0;

        if (res.portAccumulationApproved) {
            res.underwritingNotes = "Voyage rating approved within port accumulation limit.";
        } else {
            res.underwritingNotes = String.format("PORT ACCUMULATION ALERT: Total cargo exposure $%s exceeds port cap $%s.",
                    totalPortAccumulation, res.portAccumulationLimit);
        }

        return res;
    }
}
