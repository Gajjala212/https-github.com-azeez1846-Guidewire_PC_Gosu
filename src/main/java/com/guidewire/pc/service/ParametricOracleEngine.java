package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real-Time USGS / NOAA Parametric Catastrophe Oracle Platform.
 */
public class ParametricOracleEngine {
    private static final Logger LOGGER = Logger.getLogger(ParametricOracleEngine.class.getName());
    private static final ParametricOracleEngine INSTANCE = new ParametricOracleEngine();

    public static ParametricOracleEngine getInstance() {
        return INSTANCE;
    }

    public static class OracleEvaluationResult {
        public String policyNumber;
        public String perilType;
        public String locationAddress;
        public double latitude;
        public double longitude;
        public BigDecimal contractLimit;
        public double measuredMetricValue;
        public String metricUnit;
        public double payoutPercentage;
        public BigDecimal instantIndemnityPayout;
        public boolean triggerBreached;
        public String oracleDataSource;
        public String settlementStatus;
        public String wireTransferReference;
        public BigDecimal reinsuranceRecoveryAmount;
        public String notes;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("policyNumber", policyNumber);
            map.put("perilType", perilType);
            map.put("locationAddress", locationAddress);
            map.put("latitude", latitude);
            map.put("longitude", longitude);
            map.put("contractLimit", contractLimit);
            map.put("measuredMetricValue", measuredMetricValue);
            map.put("metricUnit", metricUnit);
            map.put("payoutPercentage", payoutPercentage);
            map.put("instantIndemnityPayout", instantIndemnityPayout);
            map.put("triggerBreached", triggerBreached);
            map.put("oracleDataSource", oracleDataSource);
            map.put("settlementStatus", settlementStatus);
            map.put("wireTransferReference", wireTransferReference);
            map.put("reinsuranceRecoveryAmount", reinsuranceRecoveryAmount);
            map.put("notes", notes);
            return map;
        }
    }

    public OracleEvaluationResult evaluatePerilTrigger(String policyNumber,
                                                       String perilType,
                                                       String locationAddress,
                                                       double latitude,
                                                       double longitude,
                                                       BigDecimal contractLimit,
                                                       double measuredTelemetryValue) {
        LOGGER.log(Level.INFO, "Evaluating Parametric Oracle for policy: {0}, Peril: {1}, Telemetry: {2}",
                new Object[]{policyNumber, perilType, measuredTelemetryValue});

        OracleEvaluationResult res = new OracleEvaluationResult();
        res.policyNumber = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-PARAMETRIC-7701";
        res.perilType = (perilType != null && !perilType.isBlank()) ? perilType.toUpperCase() : "EARTHQUAKE_USGS";
        res.locationAddress = (locationAddress != null && !locationAddress.isBlank()) ? locationAddress : "San Francisco Waterfront Port, CA";
        res.latitude = (latitude != 0.0) ? latitude : 37.7749;
        res.longitude = (longitude != 0.0) ? longitude : -122.4194;
        res.contractLimit = (contractLimit != null && contractLimit.compareTo(BigDecimal.ZERO) > 0)
                ? contractLimit : new BigDecimal("2500000.00");
        res.measuredMetricValue = measuredTelemetryValue;

        double payoutPct = 0.0;

        if ("EARTHQUAKE_USGS".equals(res.perilType)) {
            res.oracleDataSource = "USGS Earthquake ShakeMap ShakeGrid API (Real-Time Station)";
            res.metricUnit = "Peak Ground Acceleration (PGA in g)";

            if (measuredTelemetryValue >= 0.65) {
                payoutPct = 1.00; // 100% payout (Severe Shaking / Destructive)
                res.notes = String.format("USGS PGA %.2fg exceeds Category A Destructive threshold (>=0.65g). 100%% Payout triggered.", measuredTelemetryValue);
            } else if (measuredTelemetryValue >= 0.40) {
                payoutPct = 0.50; // 50% payout
                res.notes = String.format("USGS PGA %.2fg breaches Category B Moderate threshold (>=0.40g). 50%% Payout triggered.", measuredTelemetryValue);
            } else if (measuredTelemetryValue >= 0.20) {
                payoutPct = 0.25; // 25% payout
                res.notes = String.format("USGS PGA %.2fg breaches Category C Minor threshold (>=0.20g). 25%% Payout triggered.", measuredTelemetryValue);
            } else {
                payoutPct = 0.0;
                res.notes = String.format("USGS PGA %.2fg below contract trigger threshold (0.20g). No loss payout.", measuredTelemetryValue);
            }
        } else {
            // HURRICANE_NOAA
            res.oracleDataSource = "NOAA National Hurricane Center (NHC) Automated Buoy & Recon Feed";
            res.metricUnit = "Central Barometric Pressure (mbar)";

            if (measuredTelemetryValue <= 935.0 && measuredTelemetryValue > 0.0) {
                payoutPct = 1.00; // Cat 4/5 Major Hurricane
                res.notes = String.format("NOAA Central Pressure %.1f mbar breaches Extreme Cat 4/5 threshold (<=935 mbar). 100%% Payout triggered.", measuredTelemetryValue);
            } else if (measuredTelemetryValue <= 960.0 && measuredTelemetryValue > 0.0) {
                payoutPct = 0.50; // Cat 2/3 Hurricane
                res.notes = String.format("NOAA Central Pressure %.1f mbar breaches Hurricane threshold (<=960 mbar). 50%% Payout triggered.", measuredTelemetryValue);
            } else {
                payoutPct = 0.0;
                res.notes = String.format("NOAA Pressure %.1f mbar remains above contract trigger threshold (>960 mbar). No loss payout.", measuredTelemetryValue);
            }
        }

        res.payoutPercentage = payoutPct * 100.0;
        res.triggerBreached = payoutPct > 0.0;
        res.instantIndemnityPayout = res.contractLimit.multiply(BigDecimal.valueOf(payoutPct)).setScale(2, RoundingMode.HALF_UP);

        if (res.triggerBreached) {
            res.settlementStatus = "SETTLED_INSTANT_FEDNOW_WIRE";
            res.wireTransferReference = "FEDWIRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            // 75% Catastrophe Reinsurance Treaty Recovery
            res.reinsuranceRecoveryAmount = res.instantIndemnityPayout.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
        } else {
            res.settlementStatus = "MONITORING_ACTIVE_SENSOR_FEED";
            res.wireTransferReference = "N/A";
            res.reinsuranceRecoveryAmount = BigDecimal.ZERO;
        }

        return res;
    }
}
