package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Cyber Risk Assessment, Systemic Cloud Accumulation & Ransomware Engine.
 */
public class CyberAccumulationEngine {
    private static final Logger LOGGER = Logger.getLogger(CyberAccumulationEngine.class.getName());
    private static final CyberAccumulationEngine INSTANCE = new CyberAccumulationEngine();

    public static CyberAccumulationEngine getInstance() {
        return INSTANCE;
    }

    public static class CyberEvaluationResult {
        public String policyNumber;
        public int securityPostureScore;
        public String postureTier;
        public BigDecimal basePremium;
        public double postureModifier;
        public String primaryCloudProvider;
        public double cloudAccumulationLoad;
        public BigDecimal aggregateLimit;
        public BigDecimal ransomwareSublimit;
        public BigDecimal contingentBILimit;
        public BigDecimal finalRatedPremium;
        public boolean underwritingReferralRequired;
        public String referralReason;
        public Map<String, Object> controlsSummary = new HashMap<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("policyNumber", policyNumber);
            map.put("securityPostureScore", securityPostureScore);
            map.put("postureTier", postureTier);
            map.put("basePremium", basePremium);
            map.put("postureModifier", postureModifier);
            map.put("primaryCloudProvider", primaryCloudProvider);
            map.put("cloudAccumulationLoad", cloudAccumulationLoad);
            map.put("aggregateLimit", aggregateLimit);
            map.put("ransomwareSublimit", ransomwareSublimit);
            map.put("contingentBILimit", contingentBILimit);
            map.put("finalRatedPremium", finalRatedPremium);
            map.put("underwritingReferralRequired", underwritingReferralRequired);
            map.put("referralReason", referralReason);
            map.put("controlsSummary", controlsSummary);
            return map;
        }
    }

    public CyberEvaluationResult evaluateAndRate(String policyNumber,
                                                BigDecimal requestedLimit,
                                                boolean mfaEnforced,
                                                boolean offlineBackupsDaily,
                                                boolean edrDeployed,
                                                double phishingPassRatePct,
                                                boolean dnsFilteringActive,
                                                String primaryCloudProvider,
                                                BigDecimal annualRevenue) {
        LOGGER.log(Level.INFO, "Evaluating Cyber Risk Accumulation for policy: {0}", policyNumber);

        CyberEvaluationResult res = new CyberEvaluationResult();
        res.policyNumber = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-CYBER-8801";
        res.aggregateLimit = (requestedLimit != null && requestedLimit.compareTo(BigDecimal.ZERO) > 0) ? requestedLimit : new BigDecimal("2000000.00");
        res.primaryCloudProvider = (primaryCloudProvider != null && !primaryCloudProvider.isBlank()) ? primaryCloudProvider.toUpperCase() : "AWS";

        BigDecimal revenue = (annualRevenue != null && annualRevenue.compareTo(BigDecimal.ZERO) > 0) ? annualRevenue : new BigDecimal("10000000.00");

        // 1. Calculate Security Posture Score (0 - 100)
        int score = 0;
        if (mfaEnforced) score += 30;
        if (offlineBackupsDaily) score += 25;
        if (edrDeployed) score += 20;
        if (phishingPassRatePct >= 85.0) score += 15;
        else if (phishingPassRatePct >= 70.0) score += 8;
        if (dnsFilteringActive) score += 10;
        res.securityPostureScore = score;

        res.controlsSummary.put("mfaEnforced", mfaEnforced);
        res.controlsSummary.put("offlineBackupsDaily", offlineBackupsDaily);
        res.controlsSummary.put("edrDeployed", edrDeployed);
        res.controlsSummary.put("phishingPassRatePct", phishingPassRatePct);
        res.controlsSummary.put("dnsFilteringActive", dnsFilteringActive);

        // 2. Posture Tier & Modifier
        if (score >= 85) {
            res.postureTier = "PREFERRED_TIER_1";
            res.postureModifier = 0.80; // -20% credit
            res.ransomwareSublimit = res.aggregateLimit; // Full limit allowed
            res.underwritingReferralRequired = false;
            res.referralReason = "None. Risk conforms to preferred cyber guidelines.";
        } else if (score >= 65) {
            res.postureTier = "STANDARD_TIER_2";
            res.postureModifier = 1.00;
            res.ransomwareSublimit = res.aggregateLimit.min(new BigDecimal("1000000.00"));
            res.underwritingReferralRequired = false;
            res.referralReason = "Standard security controls met.";
        } else if (score >= 45) {
            res.postureTier = "SUBSTANDARD_TIER_3";
            res.postureModifier = 1.35; // +35% surcharge
            res.ransomwareSublimit = res.aggregateLimit.min(new BigDecimal("500000.00"));
            res.underwritingReferralRequired = !mfaEnforced;
            res.referralReason = !mfaEnforced ? "MFA not enforced across privileged accounts. UW approval required." : "Substandard posture score.";
        } else {
            res.postureTier = "HIGH_RISK_TIER_4";
            res.postureModifier = 1.75; // +75% surcharge
            res.ransomwareSublimit = res.aggregateLimit.min(new BigDecimal("250000.00"));
            res.underwritingReferralRequired = true;
            res.referralReason = "Critical vulnerability: Missing multiple core controls (MFA, Backups, EDR). Manual CUO override required.";
        }

        // 3. Cloud Dependency Accumulation Factor
        switch (res.primaryCloudProvider) {
            case "AWS":
                res.cloudAccumulationLoad = 1.10;
                break;
            case "AZURE":
                res.cloudAccumulationLoad = 1.08;
                break;
            case "GCP":
                res.cloudAccumulationLoad = 1.05;
                break;
            case "MULTI_CLOUD":
                res.cloudAccumulationLoad = 0.95; // Resilient multi-cloud discount
                break;
            case "ON_PREM":
            default:
                res.cloudAccumulationLoad = 1.15; // Higher on-prem physical risk
                break;
        }

        // 4. Rate Calculation
        // Base rate: $0.35 per $1,000 limit + $0.05 per $1,000 revenue
        BigDecimal limitRate = res.aggregateLimit.multiply(new BigDecimal("0.0035"));
        BigDecimal revenueRate = revenue.multiply(new BigDecimal("0.0003"));
        res.basePremium = limitRate.add(revenueRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal rated = res.basePremium
                .multiply(BigDecimal.valueOf(res.postureModifier))
                .multiply(BigDecimal.valueOf(res.cloudAccumulationLoad));

        // Contingent Business Interruption limit (50% of aggregate limit)
        res.contingentBILimit = res.aggregateLimit.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);

        res.finalRatedPremium = rated.setScale(2, RoundingMode.HALF_UP);
        return res;
    }
}
