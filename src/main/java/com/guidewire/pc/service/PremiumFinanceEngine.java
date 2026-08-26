package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Commercial Premium Financing (PFC), Electronic Notice of Assignment (NOA) & Power of Attorney Engine.
 */
public class PremiumFinanceEngine {
    private static final Logger LOGGER = Logger.getLogger(PremiumFinanceEngine.class.getName());
    private static final PremiumFinanceEngine INSTANCE = new PremiumFinanceEngine();

    public static PremiumFinanceEngine getInstance() {
        return INSTANCE;
    }

    public static class FinancePaymentScheduleItem {
        public int installmentNumber;
        public String dueDate;
        public BigDecimal paymentAmount;
        public BigDecimal principalPortion;
        public BigDecimal interestPortion;
        public BigDecimal remainingBalance;
    }

    public static class PremiumFinanceResult {
        public String agreementNumber;
        public String policyNumber;
        public String financeCompanyName;
        public BigDecimal totalPolicyPremium;
        public double downPaymentPct;
        public BigDecimal downPaymentAmount;
        public BigDecimal financedPrincipal;
        public double aprPercentage;
        public int installmentCount;
        public BigDecimal totalInterestCharge;
        public BigDecimal totalFinancedRepayment;
        public BigDecimal monthlyInstallmentAmount;
        public String noticeOfAssignmentId;
        public String noaStatus;
        public boolean powerOfAttorneyGranted;
        public String carrierRemittanceRouting;
        public List<FinancePaymentScheduleItem> installmentSchedule = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("agreementNumber", agreementNumber);
            map.put("policyNumber", policyNumber);
            map.put("financeCompanyName", financeCompanyName);
            map.put("totalPolicyPremium", totalPolicyPremium);
            map.put("downPaymentPct", downPaymentPct);
            map.put("downPaymentAmount", downPaymentAmount);
            map.put("financedPrincipal", financedPrincipal);
            map.put("aprPercentage", aprPercentage);
            map.put("installmentCount", installmentCount);
            map.put("totalInterestCharge", totalInterestCharge);
            map.put("totalFinancedRepayment", totalFinancedRepayment);
            map.put("monthlyInstallmentAmount", monthlyInstallmentAmount);
            map.put("noticeOfAssignmentId", noticeOfAssignmentId);
            map.put("noaStatus", noaStatus);
            map.put("powerOfAttorneyGranted", powerOfAttorneyGranted);
            map.put("carrierRemittanceRouting", carrierRemittanceRouting);
            map.put("installmentSchedule", installmentSchedule);
            return map;
        }
    }

    public PremiumFinanceResult createFinanceAgreementAndNOA(String policyNumber,
                                                             BigDecimal policyPremium,
                                                             String pfcName,
                                                             double downPaymentPercent,
                                                             double aprPct,
                                                             int installmentCount) {
        LOGGER.log(Level.INFO, "Generating Premium Finance Agreement for policy: {0}", policyNumber);

        PremiumFinanceResult res = new PremiumFinanceResult();
        res.agreementNumber = "PFC-AGR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        res.policyNumber = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-COMM-8001";
        res.financeCompanyName = (pfcName != null && !pfcName.isBlank()) ? pfcName : "AFCO Premium Credit Corp";
        res.totalPolicyPremium = (policyPremium != null && policyPremium.compareTo(BigDecimal.ZERO) > 0)
                ? policyPremium : new BigDecimal("50000.00");

        res.downPaymentPct = (downPaymentPercent > 0) ? downPaymentPercent : 20.0;
        res.aprPercentage = (aprPct > 0) ? aprPct : 8.75;
        res.installmentCount = (installmentCount > 0) ? installmentCount : 9;

        // Down Payment
        res.downPaymentAmount = res.totalPolicyPremium.multiply(BigDecimal.valueOf(res.downPaymentPct / 100.0)).setScale(2, RoundingMode.HALF_UP);
        res.financedPrincipal = res.totalPolicyPremium.subtract(res.downPaymentAmount).setScale(2, RoundingMode.HALF_UP);

        // Interest Calculation: Simple Interest = Principal * (APR / 100) * (Months / 12)
        double yearFraction = (double) res.installmentCount / 12.0;
        double interest = res.financedPrincipal.doubleValue() * (res.aprPercentage / 100.0) * yearFraction;
        res.totalInterestCharge = BigDecimal.valueOf(interest).setScale(2, RoundingMode.HALF_UP);

        res.totalFinancedRepayment = res.financedPrincipal.add(res.totalInterestCharge).setScale(2, RoundingMode.HALF_UP);
        res.monthlyInstallmentAmount = res.totalFinancedRepayment.divide(BigDecimal.valueOf(res.installmentCount), 2, RoundingMode.HALF_UP);

        res.noticeOfAssignmentId = "NOA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        res.noaStatus = "EXECUTED_ACTIVE_ASSIGNMENT";
        res.powerOfAttorneyGranted = true;
        res.carrierRemittanceRouting = "ACH-CARRIER-REMITTANCE-LOCKED (Payable to PFC in event of cancellation)";

        // Generate Installment Schedule
        BigDecimal remaining = res.totalFinancedRepayment;
        for (int i = 1; i <= res.installmentCount; i++) {
            FinancePaymentScheduleItem item = new FinancePaymentScheduleItem();
            item.installmentNumber = i;
            item.dueDate = String.format("2026-%02d-15", (i % 12) + 1);
            item.paymentAmount = res.monthlyInstallmentAmount;
            item.interestPortion = res.totalInterestCharge.divide(BigDecimal.valueOf(res.installmentCount), 2, RoundingMode.HALF_UP);
            item.principalPortion = item.paymentAmount.subtract(item.interestPortion);
            remaining = remaining.subtract(item.paymentAmount).max(BigDecimal.ZERO);
            item.remainingBalance = remaining.setScale(2, RoundingMode.HALF_UP);
            res.installmentSchedule.add(item);
        }

        return res;
    }
}
