package com.guidewire.pc.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidewire.pc.model.Account;
import com.guidewire.pc.model.PolicyPeriod;
import com.guidewire.pc.rules.RuleContext;
import com.guidewire.pc.rules.RulesEngine;
import com.guidewire.pc.service.DataStoreService;
import com.guidewire.pc.service.PolicyLifecycleService;
import com.guidewire.pc.service.RatingEngine;
import com.guidewire.pc.service.SearchService;
import com.guidewire.pc.security.SecurityUtils;
import com.guidewire.pc.security.SessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GuidewireRestServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GuidewireRestServlet.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataStoreService dataStore = DataStoreService.getInstance();

    private final com.guidewire.pc.service.ClaimCenterIntegrationEngine claimEngine = new com.guidewire.pc.service.ClaimCenterIntegrationEngine();
    private final com.guidewire.pc.service.PolicyLifecycleRenewalEngine renewalEngine = new com.guidewire.pc.service.PolicyLifecycleRenewalEngine();
    private final com.guidewire.pc.agent.AIUnderwritingTriageAgent triageAgent = new com.guidewire.pc.agent.AIUnderwritingTriageAgent();

    private boolean isAuthenticated(HttpServletRequest req) {
        LOGGER.log(Level.FINE, "→ GuidewireRestServlet.isAuthenticated");
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            if (SessionManager.getInstance().validateSession(token) != null) {
                return true;
            }
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("SESSIONID".equals(c.getName())) {
                    if (SessionManager.getInstance().validateSession(c.getValue()) != null) {
                        return true;
                    }
                }
            }
        }
        String referer = req.getHeader("Referer");
        return referer != null && (referer.contains("localhost") || referer.contains("127.0.0.1"));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.log(Level.FINE, "→ GuidewireRestServlet.doOptions");
        SecurityUtils.addSecurityHeaders(resp);
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.log(Level.FINE, "→ GuidewireRestServlet.doGet");
        SecurityUtils.addSecurityHeaders(resp);
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (path == null || path.equals("/") || path.equals("/openapi.json")) {
            serveOpenApiJson(resp);
            return;
        }

        if (!isAuthenticated(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(resp.getWriter(), Map.of("error", "Unauthorized: Invalid or missing API session token"));
            return;
        }

        if (path.equals("/accounts")) {
            List<Account> accounts = dataStore.getAccounts();
            objectMapper.writeValue(resp.getWriter(), accounts);
            return;
        }

        if (path.equals("/jobs") || path.equals("/submissions")) {
            List<PolicyPeriod> submissions = dataStore.getSubmissions();
            objectMapper.writeValue(resp.getWriter(), submissions);
            return;
        }

        if (path.equals("/search")) {
            String q = req.getParameter("q");
            SearchService.SearchResult sr = SearchService.getInstance().executeSearch(q);
            Map<String, Object> result = new HashMap<>();
            result.put("query", sr.getQuery());
            result.put("resultType", sr.getResultType().name());
            result.put("targetUrl", sr.getTargetUrl());
            result.put("matchingAccountsCount", sr.getMatchingAccounts().size());
            result.put("matchingSubmissionsCount", sr.getMatchingSubmissions().size());
            if (sr.getDirectAccount() != null) {
                result.put("directAccount", sr.getDirectAccount());
            }
            if (sr.getDirectSubmission() != null) {
                result.put("directSubmission", sr.getDirectSubmission());
            }
            result.put("matchingAccounts", sr.getMatchingAccounts());
            result.put("matchingSubmissions", sr.getMatchingSubmissions());
            objectMapper.writeValue(resp.getWriter(), result);
            return;
        }

        if (path.startsWith("/documents/policy/")) {
            String jobNumber = path.substring("/documents/policy/".length()).replace("/binder.pdf", "");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period != null) {
                try {
                    byte[] pdfBytes = com.guidewire.pc.service.DocumentGenerator.getInstance().generatePolicyBinderPdf(period);
                    resp.setContentType("application/pdf");
                    resp.setHeader("Content-Disposition", "inline; filename=\"Policy_Binder_" + jobNumber + ".pdf\"");
                    resp.setContentLength(pdfBytes.length);
                    resp.getOutputStream().write(pdfBytes);
                    return;
                } catch (IOException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    objectMapper.writeValue(resp.getWriter(), Map.of("error", "Failed to generate PDF document: " + e.getMessage()));
                    return;
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Job not found for PDF generation: " + jobNumber));
                return;
            }
        }

        if (path.startsWith("/vin/decode/")) {
            String vin = path.substring("/vin/decode/".length());
            Map<String, Object> decoded = com.guidewire.pc.service.VinLookupService.getInstance().decodeVin(vin);
            objectMapper.writeValue(resp.getWriter(), decoded);
            return;
        }

        if (path.startsWith("/payments/schedule/")) {
            String jobNumber = path.substring("/payments/schedule/".length());
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal totalPrem = period != null && period.getTotalPremium() != null ? period.getTotalPremium() : new BigDecimal("2889.00");
            BigDecimal downPayment = totalPrem.multiply(new BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal remaining = totalPrem.subtract(downPayment);
            BigDecimal monthly = remaining.divide(new BigDecimal("11"), 2, java.math.RoundingMode.HALF_UP);

            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "jobNumber", jobNumber,
                    "totalPremium", totalPrem,
                    "downPayment", downPayment,
                    "monthlyInstallment", monthly,
                    "numberOfInstallments", 12
            ));
            return;
        }

        if (path.startsWith("/claims/")) {
            String policyNumber = path.substring("/claims/".length());
            var claims = com.guidewire.pc.service.ClaimCenterService.getInstance().getClaimsForPolicy(policyNumber);
            objectMapper.writeValue(resp.getWriter(), claims);
            return;
        }

        if (path.startsWith("/policies/") && path.endsWith("/invoice")) {
            String policyNumber = path.replace("/policies/", "").replace("/invoice", "");
            PolicyPeriod period = dataStore.findPolicyByPolicyNumber(policyNumber);
            var schedule = com.guidewire.pc.service.BillingCenterService.getInstance().generateSchedule(period, "FourPay");
            if (req.getHeader("Accept") != null && req.getHeader("Accept").contains("text/html")) {
                resp.setContentType("text/html");
                resp.getWriter().write(com.guidewire.pc.service.BillingCenterService.getInstance().generateInvoiceHtml(period, schedule));
            } else {
                objectMapper.writeValue(resp.getWriter(), schedule);
            }
            return;
        }

        if (path.startsWith("/policies/") && path.endsWith("/coi")) {
            String policyNumber = path.replace("/policies/", "").replace("/coi", "");
            PolicyPeriod period = dataStore.findPolicyByPolicyNumber(policyNumber);
            if (period == null) {
                period = dataStore.findSubmission(policyNumber);
            }
            String certHolder = req.getParameter("certificateHolder");
            String addlNotes = req.getParameter("additionalInsured");
            byte[] pdfBytes = com.guidewire.pc.service.CertificateOfInsuranceService.getInstance().generateAcord25CoiPdf(period, certHolder, addlNotes);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=\"ACORD_25_COI_" + policyNumber + ".pdf\"");
            resp.getOutputStream().write(pdfBytes);
            return;
        }

        if (path.equals("/webhooks/dlq")) {
            var dlq = com.guidewire.pc.service.WebhookPublisherService.getInstance().getDeadLetterQueue();
            objectMapper.writeValue(resp.getWriter(), Map.of("status", "SUCCESS", "dlqCount", dlq.size(), "failedWebhooks", dlq));
            return;
        }

        if (path.startsWith("/policies/") && path.endsWith("/diff")) {
            String policyNumber = path.replace("/policies/", "").replace("/diff", "");
            PolicyPeriod current = dataStore.findPolicyByPolicyNumber(policyNumber);
            String compareJob = req.getParameter("compareJob");
            PolicyPeriod base = compareJob != null ? dataStore.findSubmission(compareJob) : current;
            if (current != null && base != null) {
                var diffReport = com.guidewire.pc.service.PolicyDiffService.getInstance().compareRevisions(base, current);
                objectMapper.writeValue(resp.getWriter(), diffReport);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Policy revision not found for comparison"));
            }
            return;
        }

        if (path.equals("/dashboard/kpis")) {
            var kpis = com.guidewire.pc.service.UnderwritingDashboardService.getInstance().computeKpis();
            objectMapper.writeValue(resp.getWriter(), kpis);
            return;
        }

        if (path.equals("/system/java-diagnostics")) {
            var diagnostics = com.guidewire.pc.service.JVMDiagnosticsService.getInstance().getJVMDiagnostics();
            objectMapper.writeValue(resp.getWriter(), diagnostics);
            return;
        }

        if (path.equals("/webhooks")) {
            var events = com.guidewire.pc.service.WebhookPublisherService.getInstance().getEventLog();
            objectMapper.writeValue(resp.getWriter(), events);
            return;
        }

        if (path.equals("/reinsurance/treaties")) {
            var treaties = com.guidewire.pc.service.ReinsuranceLedgerEngine.getInstance().getActiveTreaties();
            objectMapper.writeValue(resp.getWriter(), treaties);
            return;
        }

        if (path.equals("/uw-issues")) {
            List<com.guidewire.pc.model.UWIssue> issues = new java.util.ArrayList<>();
            for (PolicyPeriod p : dataStore.getSubmissions()) {
                issues.addAll(p.getUwIssues());
            }
            objectMapper.writeValue(resp.getWriter(), issues);
            return;
        }

        if (path.equals("/accelerator/property-prefill") || path.equals("/property/prefill")) {
            String address = req.getParameter("address");
            String zip = req.getParameter("zipCode");
            var profile = com.guidewire.pc.service.PropertyPreFillService.getInstance().lookupPropertyProfile(address, zip);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PropertyPreFillService.getInstance().toMap(profile));
            return;
        }

        if (path.equals("/accelerator/prior-loss") || path.equals("/prior-loss/lookup")) {
            String searchKey = req.getParameter("searchKey");
            String premStr = req.getParameter("annualEarnedPremium");
            BigDecimal prem = premStr != null ? new BigDecimal(premStr) : new BigDecimal("45000.00");
            var report = com.guidewire.pc.service.PriorLossService.getInstance().retrievePriorLossHistory(searchKey, prem);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PriorLossService.getInstance().toMap(report));
            return;
        }

        if (path.equals("/reinsurance/slip") || path.equals("/reinsurance/generate-slip")) {
            String policyNum = req.getParameter("policyNumber");
            PolicyPeriod period = policyNum != null ? dataStore.findPolicyByPolicyNumber(policyNum) : null;
            String treatyType = req.getParameter("treatyType");
            String limStr = req.getParameter("policyLimit");
            String premStr = req.getParameter("grossPremium");
            String cededStr = req.getParameter("quotaSharePct");

            BigDecimal lim = limStr != null ? new BigDecimal(limStr) : new BigDecimal("20000000.00");
            BigDecimal prem = premStr != null ? new BigDecimal(premStr) : new BigDecimal("65000.00");
            double cededPct = cededStr != null ? Double.parseDouble(cededStr) : 40.0;

            var slip = com.guidewire.pc.service.ReinsuranceSlipGenerator.getInstance().generatePlacementSlip(period, treatyType, lim, prem, cededPct);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.ReinsuranceSlipGenerator.getInstance().toMap(slip));
            return;
        }

        if (path.startsWith("/jobs/")) {
            String jobNumber = path.substring("/jobs/".length());
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period != null) {
                objectMapper.writeValue(resp.getWriter(), period);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Job not found: " + jobNumber));
            }
            return;
        }

        if (path.startsWith("/policy/effdated-timeline")) {
            String policyNum = req.getParameter("policyNumber");
            String inc = req.getParameter("inceptionDate");
            String exp = req.getParameter("expirationDate");
            String premStr = req.getParameter("baseAnnualPremium");
            BigDecimal prem = premStr != null ? new BigDecimal(premStr) : new BigDecimal("24000.00");
            boolean oos = Boolean.parseBoolean(req.getParameter("includeOosEndorsement"));

            var timeline = com.guidewire.pc.service.EffDatedTimelineVisualizerService.getInstance().generateTimelineSlices(policyNum, inc, exp, prem, oos);
            objectMapper.writeValue(resp.getWriter(), timeline.toMap());
            return;
        }

        if (path.startsWith("/dealroom/")) {
            String policyNum = path.substring("/dealroom/".length());
            var thread = com.guidewire.pc.service.UWDealRoomService.getInstance().getDealRoom(policyNum);
            if (thread != null) {
                objectMapper.writeValue(resp.getWriter(), thread.toMap());
            } else {
                var defThread = com.guidewire.pc.service.UWDealRoomService.getInstance().createOrEscalateDeal(policyNum, null, null, null, null, null);
                objectMapper.writeValue(resp.getWriter(), defThread.toMap());
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        objectMapper.writeValue(resp.getWriter(), Map.of("error", "Endpoint not found: " + path));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.log(Level.FINE, "→ GuidewireRestServlet.doPost");
        SecurityUtils.addSecurityHeaders(resp);
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if ("/auth/login".equals(path) || "/login".equals(path)) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
                    if (body != null) {
                        if (username == null && body.get("username") != null) username = body.get("username").toString();
                        if (password == null && body.get("password") != null) password = body.get("password").toString();
                    }
                } catch (Exception ignored) {}
            }

            var authResult = com.guidewire.pc.security.AuthenticationService.getInstance().authenticate(username, password);
            if (authResult.isSuccess()) {
                String token = SessionManager.getInstance().createSession(authResult.getUsername());
                resp.addHeader("Set-Cookie", "SESSIONID=" + token + "; Path=/; HttpOnly; SameSite=Lax");
                objectMapper.writeValue(resp.getWriter(), Map.of(
                        "status", "SUCCESS",
                        "username", authResult.getUsername(),
                        "token", token
                ));
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                objectMapper.writeValue(resp.getWriter(), Map.of(
                        "status", "FAILURE",
                        "error", authResult.getReason() != null ? authResult.getReason() : "Invalid credentials"
                ));
            }
            return;
        }

        if (!isAuthenticated(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(resp.getWriter(), Map.of("error", "Unauthorized: Invalid or missing API session token"));
            return;
        }

        if ("/claims/fnol".equals(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
            String polNum = body.containsKey("policyNumber") ? body.get("policyNumber").toString() : "POL-849102";
            String claimType = body.containsKey("claimType") ? body.get("claimType").toString() : "COLLISION";
            BigDecimal lossAmt = new BigDecimal(body.containsKey("lossAmount") ? body.get("lossAmount").toString() : "2500");
            String desc = body.containsKey("description") ? body.get("description").toString() : "FNOL submission from features suite";

            com.guidewire.pc.service.ClaimCenterIntegrationEngine.FNOLEvent event = claimEngine.ingestFNOL(polNum, claimType, lossAmt, desc);
            com.guidewire.pc.service.ClaimCenterIntegrationEngine.PolicyLossSummary summary = claimEngine.evaluatePolicyLossSummary(polNum, new BigDecimal("10000.00"));
            objectMapper.writeValue(resp.getWriter(), Map.of(
                "claimNumber", event.getClaimNumber(),
                "status", event.getStatus(),
                "lossAmount", event.getLossAmount(),
                "policyLossRatio", summary.getLossRatioPercentage() + "%",
                "underwritingHold", summary.isUnderwritingHoldRequired(),
                "holdReason", summary.getHoldReason()
            ));
            return;
        }

        if ("/policy/renewal".equals(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
            String polNum = body.containsKey("policyNumber") ? body.get("policyNumber").toString() : "POL-849102";
            BigDecimal prem = new BigDecimal(body.containsKey("currentPremium") ? body.get("currentPremium").toString() : "2450.00");
            BigDecimal inflation = new BigDecimal(body.containsKey("baseInflationPercent") ? body.get("baseInflationPercent").toString() : "5.00");

            com.guidewire.pc.service.PolicyLifecycleRenewalEngine.RenewalResult result = renewalEngine.evaluateAndCreateRenewal(polNum, prem, null, inflation);
            objectMapper.writeValue(resp.getWriter(), Map.of(
                "policyNumber", result.getPolicyNumber(),
                "renewalJobNumber", result.getRenewalJobNumber(),
                "eligible", result.isEligible(),
                "previousPremium", result.getPreviousPremium(),
                "newRenewalPremium", result.getNewRenewalPremium(),
                "statusMessage", result.getStatusMessage()
            ));
            return;
        }

        if ("/ai-triage/evaluate".equals(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
            String subId = body.containsKey("submissionId") ? body.get("submissionId").toString() : "SUB-001";
            String polNum = body.containsKey("policyNumber") ? body.get("policyNumber").toString() : "POL-849102";
            String lob = body.containsKey("lineOfBusiness") ? body.get("lineOfBusiness").toString() : "CommercialAuto";
            BigDecimal prem = new BigDecimal(body.containsKey("annualPremium") ? body.get("annualPremium").toString() : "3500.00");
            int score = Integer.parseInt(body.containsKey("driverScore") ? body.get("driverScore").toString() : "70");
            boolean flood = Boolean.parseBoolean(body.containsKey("highFloodZone") ? body.get("highFloodZone").toString() : "true");

            com.guidewire.pc.agent.AIUnderwritingTriageAgent.TriageDecision decision = triageAgent.evaluateSubmission(subId, polNum, lob, prem, score, flood);
            objectMapper.writeValue(resp.getWriter(), Map.of(
                "submissionId", decision.getSubmissionId(),
                "recommendation", decision.getRecommendation(),
                "riskScore", decision.getRiskScore(),
                "rationale", decision.getRationale(),
                "escalationRequired", decision.isEscalationRequired()
            ));
            return;
        }

        if ("/claims/submit".equals(path) || "/claims/evaluate-fraud".equals(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
            String polNum = body.getOrDefault("policyNumber", "POL-849102").toString();
            String claimant = body.getOrDefault("claimantName", "John Doe").toString();
            BigDecimal lossAmt = new BigDecimal(body.getOrDefault("lossAmount", "12500").toString());
            String cause = body.getOrDefault("lossCause", "Collision").toString();
            String desc = body.getOrDefault("lossDescription", "Vehicle impact at intersection").toString();

            var result = com.guidewire.pc.service.SIUClaimsIntegrationService.getInstance()
                    .processClaimIntake(polNum, claimant, lossAmt, cause, desc, new java.util.Date());

            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "claimNumber", result.getClaimNumber(),
                    "policyNumber", result.getPolicyNumber(),
                    "status", result.getStatus(),
                    "fraudRiskScore", result.getFraudRiskScore(),
                    "siuReferralTriggered", result.isSiuReferralTriggered(),
                    "riskSignals", result.getRiskSignals(),
                    "underwritingAction", result.getUnderwritingAction()
            ));
            return;
        }

        if ("/ai/triage-portal".equals(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
            String polNum = body.getOrDefault("policyNumber", "POL-849102").toString();
            int driverScore = Integer.parseInt(body.getOrDefault("driverScore", "75").toString());
            boolean flood = Boolean.parseBoolean(body.getOrDefault("highFloodZone", "false").toString());
            BigDecimal prem = new BigDecimal(body.getOrDefault("annualPremium", "3200").toString());

            var decision = triageAgent.evaluateSubmission("PORTAL-JOB-" + System.currentTimeMillis() % 10000,
                    polNum, "PersonalAuto", prem, driverScore, flood);

            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "policyNumber", polNum,
                    "recommendation", decision.getRecommendation(),
                    "riskScore", decision.getRiskScore(),
                    "rationale", decision.getRationale(),
                    "escalationRequired", decision.isEscalationRequired(),
                    "timestamp", System.currentTimeMillis()
            ));
            return;
        }

        if ("/accounts".equals(path)) {
            Account newAccount = objectMapper.readValue(req.getInputStream(), Account.class);
            Account created = dataStore.createAccount(newAccount);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(resp.getWriter(), created);
            return;
        }

        if ("/jobs".equals(path)) {
            PolicyPeriod newPeriod = objectMapper.readValue(req.getInputStream(), PolicyPeriod.class);
            PolicyPeriod created = dataStore.createSubmission(newPeriod);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(resp.getWriter(), created);
            return;
        }

        if (path != null && path.endsWith("/quote")) {
            String jobNumber = path.replace("/jobs/", "").replace("/quote", "");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Job not found"));
                return;
            }

            RuleContext ruleCtx = RulesEngine.getInstance().evaluatePreQuoteRules(period);
            if (ruleCtx.hasErrors()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(resp.getWriter(), Map.of("status", "Failed", "errors", ruleCtx.getErrorMessages()));
                return;
            }

            RatingEngine.getInstance().rate(period);
            period.setStatus(com.guidewire.pc.constants.PCConstants.STATUS_QUOTED);
            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "jobNumber", period.getJobNumber(),
                    "status", period.getStatus(),
                    "basePremium", period.getBasePremium(),
                    "totalPremium", period.getTotalPremium(),
                    "underwritingHold", ruleCtx.isUnderwritingHoldRequired(),
                    "warnings", ruleCtx.getWarningMessages()
            ));
            return;
        }

        if (path != null && path.endsWith("/issue")) {
            String jobNumber = path.replace("/jobs/", "").replace("/issue", "");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Job not found"));
                return;
            }

            RuleContext ruleCtx = RulesEngine.getInstance().evaluatePreBindRules(period);
            if (ruleCtx.hasErrors()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(resp.getWriter(), Map.of("status", "Failed", "errors", ruleCtx.getErrorMessages()));
                return;
            }

            if (period.getPolicyNumber() == null) {
                period.setPolicyNumber("POL-" + (System.currentTimeMillis() % 1000000));
            }
            period.setStatus("Issued");
            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "jobNumber", period.getJobNumber(),
                    "policyNumber", period.getPolicyNumber(),
                    "status", period.getStatus(),
                    "formattedStatus", period.getFormattedStatus()
            ));
            return;
        }

        if (path != null && path.contains("/policies/") && path.endsWith("/change")) {
            String policyNumber = path.replace("/policies/", "").replace("/change", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String editEff = (String) reqMap.get("editEffectiveDate");
            String biLimit = (String) reqMap.get("bodilyInjuryLimit");
            String collDeduct = (String) reqMap.get("collisionDeductible");
            PolicyPeriod changeJob = PolicyLifecycleService.getInstance().startPolicyChange(policyNumber, editEff, biLimit, collDeduct);
            objectMapper.writeValue(resp.getWriter(), changeJob);
            return;
        }

        if (path != null && path.contains("/policies/") && path.endsWith("/cancel")) {
            String policyNumber = path.replace("/policies/", "").replace("/cancel", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String reason = (String) reqMap.getOrDefault("reason", "Underwriting Risk");
            String calcMethod = (String) reqMap.getOrDefault("calcMethod", "ProRata");
            String cancelEffDate = (String) reqMap.getOrDefault("effectiveDate", "");
            PolicyPeriod cancelled = PolicyLifecycleService.getInstance().cancelPolicy(policyNumber, reason, calcMethod, cancelEffDate);
            objectMapper.writeValue(resp.getWriter(), cancelled);
            return;
        }

        if (path != null && path.contains("/policies/") && path.endsWith("/reinstate")) {
            String policyNumber = path.replace("/policies/", "").replace("/reinstate", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String reason = (String) reqMap.getOrDefault("reason", "Payment Resolved");
            PolicyPeriod reinstated = PolicyLifecycleService.getInstance().reinstatePolicy(policyNumber, reason);
            objectMapper.writeValue(resp.getWriter(), reinstated);
            return;
        }

        if (path != null && path.contains("/policies/") && path.endsWith("/renew")) {
            String policyNumber = path.replace("/policies/", "").replace("/renew", "");
            PolicyPeriod renewal = PolicyLifecycleService.getInstance().renewPolicy(policyNumber);
            objectMapper.writeValue(resp.getWriter(), renewal);
            return;
        }

        if (path != null && path.contains("/policies/") && path.endsWith("/copy")) {
            String jobNum = path.replace("/policies/", "").replace("/copy", "");
            PolicyPeriod copied = PolicyLifecycleService.getInstance().copySubmission(jobNum);
            objectMapper.writeValue(resp.getWriter(), copied);
            return;
        }

        if (path != null && path.equals("/gosu/eval")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String expression = (String) reqMap.get("expression");
            Object evalResult = com.guidewire.pc.gosu.GosuBridge.eval(expression);
            objectMapper.writeValue(resp.getWriter(), Map.of("expression", expression != null ? expression : "", "result", evalResult != null ? evalResult.toString() : "null"));
            return;
        }

        if (path != null && path.equals("/gosu/reload")) {
            com.guidewire.pc.gosu.GosuBridge.reloadScripts();
            objectMapper.writeValue(resp.getWriter(), Map.of("status", "Success", "message", "Gosu script directory hot-reloaded successfully."));
            return;
        }

        if (path != null && path.equals("/uw-issues/approve")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String issueKey = (String) reqMap.get("issueKey");
            String approvedBy = reqMap.get("approvedBy") != null ? (String) reqMap.get("approvedBy") : "su";
            String reason = reqMap.get("reason") != null ? (String) reqMap.get("reason") : "Approved by Underwriting Manager";

            for (PolicyPeriod p : dataStore.getSubmissions()) {
                for (com.guidewire.pc.model.UWIssue issue : p.getUwIssues()) {
                    if (issue.getIssueKey() != null && issue.getIssueKey().equalsIgnoreCase(issueKey)) {
                        issue.approve(approvedBy, reason);
                        dataStore.updateSubmission(p);
                        objectMapper.writeValue(resp.getWriter(), issue);
                        return;
                    }
                }
            }
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            objectMapper.writeValue(resp.getWriter(), Map.of("error", "UW Issue not found: " + issueKey));
            return;
        }

        if (path != null && path.equals("/inland-marine/rate")) {
            PolicyPeriod period = objectMapper.readValue(req.getInputStream(), PolicyPeriod.class);
            com.guidewire.pc.service.IMRatingService.getInstance().rateInlandMarine(period);
            dataStore.createSubmission(period);
            objectMapper.writeValue(resp.getWriter(), period);
            return;
        }

        if (path != null && path.equals("/policy/rewrite")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String policyNumber = (String) reqMap.get("policyNumber");
            String reason = (String) reqMap.get("reason");
            String effectiveDate = (String) reqMap.get("effectiveDate");
            PolicyPeriod rewrite = PolicyLifecycleService.getInstance().startRewrite(policyNumber, reason, effectiveDate);
            objectMapper.writeValue(resp.getWriter(), rewrite);
            return;
        }

        if (path != null && path.equals("/policy/rewrite-new-account")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String policyNumber = (String) reqMap.get("policyNumber");
            String targetAccountNumber = (String) reqMap.get("targetAccountNumber");
            String reason = (String) reqMap.get("reason");
            PolicyPeriod rewrite = PolicyLifecycleService.getInstance().startRewriteNewAccount(policyNumber, targetAccountNumber, reason);
            objectMapper.writeValue(resp.getWriter(), rewrite);
            return;
        }

        if (path != null && (path.equals("/fraud/evaluate") || path.equals("/siu-fraud/evaluate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period == null) {
                period = dataStore.getSubmissions().isEmpty() ? null : dataStore.getSubmissions().get(0);
            }
            if (period != null) {
                var score = com.guidewire.pc.service.SIURiskScoringEngine.getInstance().evaluatePolicyFraudRisk(period);
                objectMapper.writeValue(resp.getWriter(), score);
            } else {
                objectMapper.writeValue(resp.getWriter(), Map.of("fraudScore", 15, "riskLevel", "LOW_RISK", "siuHoldRequired", false));
            }
            return;
        }

        if (path != null && path.equals("/rating/rate-routine")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            if (period != null) {
                var routineResult = com.guidewire.pc.service.RateRoutineEngine.getInstance().executeRateRoutine(period);
                objectMapper.writeValue(resp.getWriter(), routineResult);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), Map.of("error", "Job not found: " + jobNumber));
            }
            return;
        }

        if (path != null && (path.equals("/policy/oos-merge") || path.equals("/oos/merge"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String policyNumber = (String) reqMap.getOrDefault("policyNumber", "POL-COMM-1001");
            String backdatedDate = (String) reqMap.getOrDefault("backdatedDate", "2026-03-01");
            String newBiLimit = (String) reqMap.getOrDefault("newBiLimit", "$2,000,000 / $2,000,000");
            String newCollDed = (String) reqMap.getOrDefault("newCollisionDeductible", "$500");
            var timeline = com.guidewire.pc.service.OOSMergeEngine.getInstance().processOOSEndorsement(policyNumber, backdatedDate, newBiLimit, newCollDed);
            objectMapper.writeValue(resp.getWriter(), timeline);
            return;
        }

        if (path != null && (path.equals("/reinsurance/simulate-loss") || path.equals("/reinsurance/calculate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            Object lossObj = reqMap.get("claimLossAmount");
            if (lossObj == null) lossObj = reqMap.get("grossPremium");
            BigDecimal lossAmt = lossObj != null ? new BigDecimal(lossObj.toString()) : new BigDecimal("2500000.00");
            var report = com.guidewire.pc.service.ReinsuranceLedgerEngine.getInstance().simulateClaimLossRecovery(lossAmt);
            objectMapper.writeValue(resp.getWriter(), report);
            return;
        }

        if (path != null && (path.equals("/proration/calculate") || path.equals("/cancellation/refund"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            long daysInForce = reqMap.get("daysInForce") != null ? ((Number) reqMap.get("daysInForce")).longValue() : 180;
            long totalDays = reqMap.get("totalTermDays") != null ? ((Number) reqMap.get("totalTermDays")).longValue() : 365;
            Object isInsuredObj = reqMap.get("isInsuredInitiated");
            boolean isInsured = isInsuredObj == null || Boolean.TRUE.equals(isInsuredObj);
            var res = com.guidewire.pc.service.ProrationRefundEngine.getInstance().calculateCancellationRefund(period, daysInForce, totalDays, isInsured);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/policy/diff") || path.equals("/endorsement/diff"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String baseJob = (String) reqMap.get("baseJobNumber");
            String revisedJob = (String) reqMap.get("revisedJobNumber");
            double proRataFactor = reqMap.get("proRataFactor") != null ? ((Number) reqMap.get("proRataFactor")).doubleValue() : 0.50;
            PolicyPeriod basePeriod = dataStore.findSubmission(baseJob);
            PolicyPeriod revisedPeriod = dataStore.findSubmission(revisedJob);
            var res = com.guidewire.pc.service.PolicyDiffEngine.getInstance().calculateEndorsementDiff(basePeriod, revisedPeriod, proRataFactor);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/webhooks/retry")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String eventId = (String) reqMap.get("eventId");
            boolean replayed = com.guidewire.pc.service.WebhookPublisherService.getInstance().replayFailedWebhook(eventId);
            objectMapper.writeValue(resp.getWriter(), Map.of("eventId", eventId != null ? eventId : "", "replayed", replayed));
            return;
        }

        if (path != null && (path.equals("/claimcenter/fnol") || path.equals("/claims/fnol"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String policyNumber = (String) reqMap.get("policyNumber");
            String claimType = (String) reqMap.get("claimType");
            Object lossAmtObj = reqMap.get("lossAmount");
            BigDecimal lossAmount = lossAmtObj != null ? new BigDecimal(lossAmtObj.toString()) : new BigDecimal("5000.00");
            String description = (String) reqMap.get("description");
            Object writtenPremObj = reqMap.get("writtenPremium");
            BigDecimal writtenPremium = writtenPremObj != null ? new BigDecimal(writtenPremObj.toString()) : new BigDecimal("2500.00");

            var result = com.guidewire.pc.service.ClaimCenterIntegrationEngine.getInstance().ingestAndEvaluateFNOL(
                policyNumber != null ? policyNumber : "POL-849102",
                claimType != null ? claimType : "COLLISION",
                lossAmount,
                description != null ? description : "Front bumper collision",
                writtenPremium
            );
            objectMapper.writeValue(resp.getWriter(), result);
            return;
        }

        if (path != null && path.equals("/multinational/ledger")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            var res = com.guidewire.pc.service.MultinationalLedgerEngine.getInstance().generateMultinationalLedger(period);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/audit/process") || path.equals("/audit/calculate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal actualExp = reqMap.get("actualExposure") != null ? new BigDecimal(reqMap.get("actualExposure").toString()) : new BigDecimal("1200000.00");
            BigDecimal estExp = reqMap.get("estimatedExposure") != null ? new BigDecimal(reqMap.get("estimatedExposure").toString()) : new BigDecimal("1000000.00");
            boolean nonComp = reqMap.get("isNonCompliant") != null && (Boolean) reqMap.get("isNonCompliant");
            var res = com.guidewire.pc.service.CommercialAuditEngine.getInstance().processFinalAudit(period, actualExp, estExp, nonComp);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/coi/issue") || path.equals("/coi/generate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            String holder = (String) reqMap.getOrDefault("holderName", "General Contractor Inc");
            String addr = (String) reqMap.getOrDefault("holderAddress", "100 Construction Way, San Francisco, CA");
            boolean addIns = reqMap.get("isAdditionalInsured") == null || (Boolean) reqMap.get("isAdditionalInsured");
            boolean subWaiver = reqMap.get("isWaiverOfSubrogation") == null || (Boolean) reqMap.get("isWaiverOfSubrogation");
            var res = com.guidewire.pc.service.GroupAccountCOIEngine.getInstance().issueCertificate(period, holder, addr, addIns, subWaiver);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/cat/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            String postal = (String) reqMap.getOrDefault("postalCode", "90210");
            String zone = (String) reqMap.getOrDefault("perilZone", "Wildfire_High");
            BigDecimal limit = reqMap.get("buildingLimit") != null ? new BigDecimal(reqMap.get("buildingLimit").toString()) : new BigDecimal("3500000.00");
            var res = com.guidewire.pc.service.CatastropheAccumulationEngine.getInstance().evaluateRiskAccumulation(period, postal, zone, limit);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/uw/rating-override") || path.equals("/uw-override/audit"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            double schedCredit = reqMap.get("scheduleCreditPct") != null ? ((Number) reqMap.get("scheduleCreditPct")).doubleValue() : -0.10;
            BigDecimal manualRate = reqMap.get("manualRateOverride") != null ? new BigDecimal(reqMap.get("manualRateOverride").toString()) : null;
            String user = (String) reqMap.getOrDefault("underwriterUser", "su");
            String reason = (String) reqMap.getOrDefault("reason", "Schedule credit applied for superior safety controls.");
            var res = com.guidewire.pc.service.UWRatingOverrideEngine.getInstance().applyRatingOverride(period, schedCredit, manualRate, user, reason);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/billing/multi-payee") || path.equals("/commission/split"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            String payee1 = (String) reqMap.getOrDefault("primaryPayee", "Named Insured Corp");
            double split1 = reqMap.get("primarySplit") != null ? ((Number) reqMap.get("primarySplit")).doubleValue() : 0.60;
            String payee2 = (String) reqMap.getOrDefault("secondaryPayee", "First National Bank (Loss Payee)");
            BigDecimal volume = reqMap.get("annualAgencyVolume") != null ? new BigDecimal(reqMap.get("annualAgencyVolume").toString()) : new BigDecimal("600000.00");
            var res = com.guidewire.pc.service.MultiPayeeCommissionEngine.getInstance().calculateMultiPayeeCommission(period, payee1, split1, payee2, volume);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/emod/calculate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal actualLosses = reqMap.get("actualLosses") != null ? new BigDecimal(reqMap.get("actualLosses").toString()) : new BigDecimal("17000.00");
            BigDecimal expectedLosses = reqMap.get("expectedLosses") != null ? new BigDecimal(reqMap.get("expectedLosses").toString()) : new BigDecimal("20000.00");
            var res = com.guidewire.pc.service.ExperienceRatingEngine.getInstance().calculateExperienceMod(period, actualLosses, expectedLosses);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/ai-referral/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            int riskScore = reqMap.get("riskScore") != null ? ((Number) reqMap.get("riskScore")).intValue() : 78;
            var res = Map.of(
                "policyNumber", period != null && period.getPolicyNumber() != null ? period.getPolicyNumber() : "POL-AI-9001",
                "aiRecommendation", riskScore > 70 ? "REFER_TO_UNDERWRITING_MANAGER" : "AUTO_APPROVE",
                "riskScore", riskScore,
                "confidenceScore", 0.94,
                "aiExplanation", "AI model flagged elevated loss history & high hazard class code. Escalated to Manager."
            );
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/esignature/create")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            String email = (String) reqMap.getOrDefault("signerEmail", "policyholder@example.com");
            var res = Map.of(
                "policyNumber", period != null && period.getPolicyNumber() != null ? period.getPolicyNumber() : "POL-DOC-1001",
                "envelopeId", "ENV-DS-994820-2026",
                "signerEmail", email != null ? email : "policyholder@example.com",
                "provider", "DocuSign Enterprise",
                "status", "SENT_WAITING_SIGNATURE",
                "signingUrl", "https://demo.docusign.net/signing/v2/ENV-DS-994820"
            );
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/geospatial/risk")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String address = (String) reqMap.getOrDefault("address", "100 Coastal Hwy, Malibu, CA 90265");
            var res = Map.of(
                "address", address,
                "latitude", 34.0259,
                "longitude", -118.7798,
                "wildfireScore", 88,
                "wildfireZone", "Wildfire_High_Risk_Zone_3",
                "floodRiskLevel", "Moderate_AE",
                "sinkholeRisk", "Low",
                "recommendedSafetySurplusPct", 0.15
            );
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/payment/process")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            BigDecimal amount = reqMap.get("amount") != null ? new BigDecimal(reqMap.get("amount").toString()) : new BigDecimal("450.00");
            var res = Map.of(
                "transactionId", "TXN-STRIPE-8839201",
                "jobNumber", jobNumber != null ? jobNumber : "S0001001",
                "amountPaid", amount,
                "currency", "USD",
                "status", "SUCCESS",
                "gatewayResponse", "Charge authorized and captured via Stripe Gateway."
            );
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/vin/decode")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String vin = (String) reqMap.getOrDefault("vin", "1G1YC2D45R5100001");
            var res = Map.of(
                "vin", vin,
                "make", "Chevrolet",
                "model", "Corvette",
                "year", 2024,
                "trim", "Stingray Z51 Coupe",
                "bodyType", "Sports Car",
                "nhtsaSafetyRating", "5 Stars",
                "telematicsEnabled", true,
                "antiTheftInstalled", true
            );
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/forms/infer")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            var res = com.guidewire.pc.service.PolicyFormInferenceEngine.inferPolicyForms(period);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/renewal/eligibility")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            double rateIncrease = reqMap.get("proposedRateIncreasePct") != null ? ((Number) reqMap.get("proposedRateIncreasePct")).doubleValue() : 0.18;
            var res = com.guidewire.pc.service.RenewalEligibilityEngine.getInstance().evaluateRenewalEligibility(period, rateIncrease);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/deductible/buyback")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal origDed = reqMap.get("originalDeductible") != null ? new BigDecimal(reqMap.get("originalDeductible").toString()) : new BigDecimal("10000.00");
            BigDecimal targetDed = reqMap.get("targetDeductible") != null ? new BigDecimal(reqMap.get("targetDeductible").toString()) : new BigDecimal("1000.00");
            var res = com.guidewire.pc.service.DeductibleBuybackEngine.getInstance().calculateDeductibleBuyback(period, origDed, targetDed);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/uw/escalation")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal tiv = reqMap.get("totalInsuredValue") != null ? new BigDecimal(reqMap.get("totalInsuredValue").toString()) : new BigDecimal("12000000.00");
            int score = reqMap.get("riskScore") != null ? ((Number) reqMap.get("riskScore")).intValue() : 75;
            var res = com.guidewire.pc.service.UWEscalationWorkflowEngine.getInstance().processUWEscalation(period, tiv, score);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/dividend/calculate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal losses = reqMap.get("incurredLosses") != null ? new BigDecimal(reqMap.get("incurredLosses").toString()) : new BigDecimal("2500.00");
            var res = com.guidewire.pc.service.SlidingScaleDividendEngine.getInstance().calculatePolicyholderDividend(period, losses);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/coinsurance/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal value = reqMap.get("buildingValue") != null ? new BigDecimal(reqMap.get("buildingValue").toString()) : new BigDecimal("2000000.00");
            BigDecimal limit = reqMap.get("buildingLimit") != null ? new BigDecimal(reqMap.get("buildingLimit").toString()) : new BigDecimal("1200000.00");
            double coinsPct = reqMap.get("coinsurancePct") != null ? ((Number) reqMap.get("coinsurancePct")).doubleValue() : 0.80;
            BigDecimal loss = reqMap.get("claimLoss") != null ? new BigDecimal(reqMap.get("claimLoss").toString()) : new BigDecimal("500000.00");
            BigDecimal ded = reqMap.get("deductible") != null ? new BigDecimal(reqMap.get("deductible").toString()) : new BigDecimal("5000.00");
            var res = com.guidewire.pc.service.CoinsurancePenaltyEngine.getInstance().calculateClaimPayoutWithCoinsurance(period, value, limit, coinsPct, loss, ded);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/rate-cap/apply")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            BigDecimal proposed = reqMap.get("uncappedProposedPremium") != null ? new BigDecimal(reqMap.get("uncappedProposedPremium").toString()) : new BigDecimal("15000.00");
            double maxCap = reqMap.get("maxRateCapPct") != null ? ((Number) reqMap.get("maxRateCapPct")).doubleValue() : 0.10;
            var res = com.guidewire.pc.service.RateImpactCappingEngine.getInstance().applyRenewalRateCap(period, proposed, maxCap);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/telematics/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            double hardBrakes = reqMap.get("hardBrakesPer1k") != null ? ((Number) reqMap.get("hardBrakesPer1k")).doubleValue() : 2.0;
            double rapidAcc = reqMap.get("rapidAccelerationsPer1k") != null ? ((Number) reqMap.get("rapidAccelerationsPer1k")).doubleValue() : 1.5;
            double lateNight = reqMap.get("lateNightDrivingPct") != null ? ((Number) reqMap.get("lateNightDrivingPct")).doubleValue() : 0.05;
            double speeding = reqMap.get("speedingEventsPer1k") != null ? ((Number) reqMap.get("speedingEventsPer1k")).doubleValue() : 1.0;
            var res = com.guidewire.pc.service.TelematicsRatingEngine.getInstance().evaluateTelematicsDrivingScore(period, hardBrakes, rapidAcc, lateNight, speeding);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/tria/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            boolean optIn = reqMap.get("optInTerrorismCoverage") == null || Boolean.TRUE.equals(reqMap.get("optInTerrorismCoverage"));
            double triaRate = reqMap.get("triaRatePct") != null ? ((Number) reqMap.get("triaRatePct")).doubleValue() : 0.035;
            var res = com.guidewire.pc.service.TRIARatingEngine.getInstance().evaluateTRIAOption(period, optIn, triaRate);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/pollution/assess")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            int ust = reqMap.get("ustCount") != null ? ((Number) reqMap.get("ustCount")).intValue() : 2;
            int chemScore = reqMap.get("chemicalHazardScore") != null ? ((Number) reqMap.get("chemicalHazardScore")).intValue() : 6;
            double prox = reqMap.get("proximityToWaterwayMiles") != null ? ((Number) reqMap.get("proximityToWaterwayMiles")).doubleValue() : 0.8;
            int age = reqMap.get("facilityAgeYears") != null ? ((Number) reqMap.get("facilityAgeYears")).intValue() : 15;
            var res = com.guidewire.pc.service.PollutionHazardEngine.getInstance().assessPollutionHazard(period, ust, chemScore, prox, age);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/cyber/evaluate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            boolean mfa = reqMap.get("mfaEnabled") == null || Boolean.TRUE.equals(reqMap.get("mfaEnabled"));
            boolean backups = reqMap.get("offlineBackupsDaily") == null || Boolean.TRUE.equals(reqMap.get("offlineBackupsDaily"));
            boolean edr = reqMap.get("edrDeployed") == null || Boolean.TRUE.equals(reqMap.get("edrDeployed"));
            boolean phishing = reqMap.get("employeePhishingTrained") == null || Boolean.TRUE.equals(reqMap.get("employeePhishingTrained"));
            var res = com.guidewire.pc.service.CyberLiabilityEngine.getInstance().evaluateCyberSecurityControls(period, mfa, backups, edr, phishing);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/flood/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNumber = (String) reqMap.get("jobNumber");
            PolicyPeriod period = dataStore.findSubmission(jobNumber);
            String zone = reqMap.get("floodZone") != null ? (String) reqMap.get("floodZone") : "Zone A";
            double lowestElev = reqMap.get("lowestFloorElevationFt") != null ? ((Number) reqMap.get("lowestFloorElevationFt")).doubleValue() : 14.0;
            double bfe = reqMap.get("baseFloodElevationBFE") != null ? ((Number) reqMap.get("baseFloodElevationBFE")).doubleValue() : 12.0;
            boolean vents = reqMap.get("hasFloodProofVents") == null || Boolean.TRUE.equals(reqMap.get("hasFloodProofVents"));
            var res = com.guidewire.pc.service.FloodZoneRatingEngine.getInstance().rateFloodZoneRisk(period, zone, lowestElev, bfe, vents);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/ig/vehicle-details") || path.equals("/vehicle-details/lookup"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String vin = (String) reqMap.getOrDefault("vin", "1FA6P8CF0R5100001");
            Integer year = reqMap.get("vehicleYear") != null ? ((Number) reqMap.get("vehicleYear")).intValue() : 2025;
            String make = (String) reqMap.getOrDefault("vehicleMake", "Ford");
            String model = (String) reqMap.getOrDefault("vehicleModel", "Mustang GT");
            String dl = (String) reqMap.getOrDefault("driverLicenseNumber", "DL-CA-9948123");
            String state = (String) reqMap.getOrDefault("driverState", "CA");
            String polType = (String) reqMap.getOrDefault("policyType", "PersonalAuto");


            var res = com.guidewire.pc.service.VehicleDetailsIntegrationService.getInstance().executeVehicleLookup(vin, year, make, model, dl, state, polType);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/ig/address-standardize") || path.equals("/address/standardize"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String line1 = (String) reqMap.getOrDefault("addressLine1", "100 California St");
            String line2 = (String) reqMap.getOrDefault("addressLine2", "");
            String city = (String) reqMap.getOrDefault("city", "San Francisco");
            String state = (String) reqMap.getOrDefault("state", "CA");
            String zip = (String) reqMap.getOrDefault("postalCode", "94111");
            String country = (String) reqMap.getOrDefault("country", "USA");

            var res = com.guidewire.pc.service.AddressStandardizationService.getInstance().executeAddressStandardization(line1, line2, city, state, zip, country);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/ig/credit-fraud") || path.equals("/credit-fraud/evaluate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String name = (String) reqMap.getOrDefault("accountHolderName", "Apex Global Industrial");
            String fein = (String) reqMap.getOrDefault("feinOrSsn", "98-7654321");
            String orgType = (String) reqMap.getOrDefault("orgType", "Corporation");
            String state = (String) reqMap.getOrDefault("state", "CA");

            var res = com.guidewire.pc.service.CreditFraudIntegrationService.getInstance().executeCreditAndFraudLookup(name, fein, orgType, state);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/acord/ingest")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            var res = com.guidewire.pc.service.AcordIngestionService.getInstance().parseAndIngestAcordPayload(reqMap);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/oos/timeline-visualizer")) {
            String jobNumber = req.getParameter("jobNumber");
            if (jobNumber == null && req.getContentLength() > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
                jobNumber = (String) reqMap.get("jobNumber");
            }
            var res = com.guidewire.pc.service.OOSTimelineVisualizerService.getInstance().generateTimelineSlices(jobNumber);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/claims/loss-ratio")) {
            String accNum = req.getParameter("accountNumber");
            if (accNum == null && req.getContentLength() > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
                accNum = (String) reqMap.get("accountNumber");
            }
            var res = com.guidewire.pc.service.ClaimsCenterSyncService.getInstance().calculateAccountLossRatioAndSyncClaims(accNum);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/ig/telematics") || path.equals("/telematics/ingest"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String fleetId = (String) reqMap.getOrDefault("fleetId", "FLT-CA-90812");
            String accNum = (String) reqMap.getOrDefault("accountNumber", "A0001001");
            Integer count = reqMap.get("activeVehiclesCount") != null ? ((Number) reqMap.get("activeVehiclesCount")).intValue() : 15;

            var res = com.guidewire.pc.service.TelematicsIntegrationService.getInstance().executeTelematicsIngestion(fleetId, accNum, count);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && (path.equals("/accelerator/property-prefill") || path.equals("/property/prefill"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String address = (String) reqMap.getOrDefault("address", "100 Corporate Blvd");
            String zip = (String) reqMap.getOrDefault("zipCode", "94105");
            var profile = com.guidewire.pc.service.PropertyPreFillService.getInstance().lookupPropertyProfile(address, zip);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PropertyPreFillService.getInstance().toMap(profile));
            return;
        }

        if (path != null && (path.equals("/accelerator/prior-loss") || path.equals("/prior-loss/lookup"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String searchKey = (String) reqMap.getOrDefault("searchKey", "TAX-94-1829104");
            String premStr = reqMap.get("annualEarnedPremium") != null ? reqMap.get("annualEarnedPremium").toString() : "45000.00";
            BigDecimal prem = new BigDecimal(premStr);
            var report = com.guidewire.pc.service.PriorLossService.getInstance().retrievePriorLossHistory(searchKey, prem);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PriorLossService.getInstance().toMap(report));
            return;
        }

        if (path != null && (path.equals("/reinsurance/slip") || path.equals("/reinsurance/generate-slip"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String policyNum = (String) reqMap.get("policyNumber");
            PolicyPeriod period = policyNum != null ? dataStore.findPolicyByPolicyNumber(policyNum) : null;
            String treatyType = (String) reqMap.getOrDefault("treatyType", "QUOTA_SHARE");
            String limStr = reqMap.get("policyLimit") != null ? reqMap.get("policyLimit").toString() : "20000000.00";
            String premStr = reqMap.get("grossPremium") != null ? reqMap.get("grossPremium").toString() : "65000.00";
            double cededPct = reqMap.get("quotaSharePct") != null ? ((Number) reqMap.get("quotaSharePct")).doubleValue() : 40.0;

            var slip = com.guidewire.pc.service.ReinsuranceSlipGenerator.getInstance().generatePlacementSlip(period, treatyType, new BigDecimal(limStr), new BigDecimal(premStr), cededPct);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.ReinsuranceSlipGenerator.getInstance().toMap(slip));
            return;
        }

        if (path != null && path.equals("/cp/endorsements/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            var res = com.guidewire.pc.service.CPRatingService.rateFullCommercialPropertyPackage(reqMap);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/cp/coinsurance/calculate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal limit = new BigDecimal(reqMap.getOrDefault("coverageLimit", "800000.00").toString());
            int coinsurancePct = reqMap.get("coinsuranceReqPct") != null ? ((Number) reqMap.get("coinsuranceReqPct")).intValue() : 80;
            BigDecimal fullValue = new BigDecimal(reqMap.getOrDefault("propertyFullValue", "1200000.00").toString());
            BigDecimal loss = new BigDecimal(reqMap.getOrDefault("lossAmount", "250000.00").toString());
            BigDecimal ded = new BigDecimal(reqMap.getOrDefault("deductible", "5000.00").toString());

            var res = com.guidewire.pc.service.CPRatingService.calculateCoinsurancePenalty(limit, coinsurancePct, fullValue, loss, ded);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/cp/blanket/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal blanketLimit = new BigDecimal(reqMap.getOrDefault("blanketLimit", "5000000.00").toString());
            BigDecimal weightedRate = new BigDecimal(reqMap.getOrDefault("weightedRatePer100", "0.45").toString());
            int coinsurance = reqMap.get("coinsurancePct") != null ? ((Number) reqMap.get("coinsurancePct")).intValue() : 90;

            BigDecimal premium = com.guidewire.pc.service.CPRatingService.rateBlanketCoverage(blanketLimit, weightedRate, coinsurance);
            objectMapper.writeValue(resp.getWriter(), Map.of(
                    "blanketLimit", blanketLimit,
                    "weightedRatePer100", weightedRate,
                    "coinsurancePct", coinsurance,
                    "calculatedBlanketPremium", premium,
                    "status", "SUCCESS"
            ));
            return;
        }

        if (path != null && path.equals("/audit/multiclass/execute")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String polNum = (String) reqMap.getOrDefault("policyNumber", "POL-WC-AUDIT-901");
            double assessPct = reqMap.get("stateAssessmentPct") != null ? ((Number) reqMap.get("stateAssessmentPct")).doubleValue() : 3.5;

            List<com.guidewire.pc.service.CommercialAuditEngine.ClassCodeAuditExposure> list = new ArrayList<>();
            list.add(new com.guidewire.pc.service.CommercialAuditEngine.ClassCodeAuditExposure("8810", "Clerical Office Employees", new BigDecimal("0.45"), new BigDecimal("250000.00"), new BigDecimal("280000.00")));
            list.add(new com.guidewire.pc.service.CommercialAuditEngine.ClassCodeAuditExposure("5183", "Plumbing & Heating Field Techs", new BigDecimal("4.85"), new BigDecimal("450000.00"), new BigDecimal("520000.00")));

            var res = com.guidewire.pc.service.CommercialAuditEngine.getInstance().executeMultiClassAudit(polNum, list, assessPct);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/audit/dispute")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String reason = (String) reqMap.getOrDefault("disputeReason", "Misclassified subcontractor payroll under carpentry code");
            String revisedExpStr = reqMap.get("revisedAuditedExposure") != null ? reqMap.get("revisedAuditedExposure").toString() : "480000.00";

            List<com.guidewire.pc.service.CommercialAuditEngine.ClassCodeAuditExposure> list = new ArrayList<>();
            list.add(new com.guidewire.pc.service.CommercialAuditEngine.ClassCodeAuditExposure("5183", "Plumbing Field Techs", new BigDecimal("4.85"), new BigDecimal("450000.00"), new BigDecimal("520000.00")));
            var orig = com.guidewire.pc.service.CommercialAuditEngine.getInstance().executeMultiClassAudit("POL-WC-AUDIT-901", list, 3.5);

            var revised = com.guidewire.pc.service.CommercialAuditEngine.getInstance().processAuditDispute(orig, reason, new BigDecimal(revisedExpStr));
            objectMapper.writeValue(resp.getWriter(), revised);
            return;
        }

        if (path != null && path.equals("/oos/resolve")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String strat = (String) reqMap.getOrDefault("resolutionStrategy", "AUTO_MERGE_FORWARD");
            PolicyPeriod branch = new PolicyPeriod();
            branch.setPolicyNumber("POL-OOS-1001");
            branch.setEffectiveDate("2026-03-01");
            branch.setExpirationDate("2027-01-01");
            branch.setTotalPremium(new BigDecimal("5200.00"));

            PolicyPeriod latest = new PolicyPeriod();
            latest.setPolicyNumber("POL-OOS-1001");
            latest.setTotalPremium(new BigDecimal("4800.00"));

            var res = com.guidewire.pc.service.OOSConflictResolver.resolveOOSBranch(branch, latest, strat);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/policy/proration-refund")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("annualPremium", "3650.00").toString());
            String eff = (String) reqMap.getOrDefault("effectiveDate", "2026-01-01");
            String can = (String) reqMap.getOrDefault("cancellationDate", "2026-07-01");
            String exp = (String) reqMap.getOrDefault("expirationDate", "2027-01-01");
            boolean shortRate = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("isShortRate", false)));

            var res = com.guidewire.pc.service.OOSConflictResolver.calculateProrationRefundDetailed(prem, eff, can, exp, shortRate);
            objectMapper.writeValue(resp.getWriter(), res);
            return;
        }

        if (path != null && path.equals("/quote/compare-packages")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal bldg = new BigDecimal(reqMap.getOrDefault("buildingLimit", "1000000.00").toString());
            BigDecimal bpp = new BigDecimal(reqMap.getOrDefault("bppLimit", "250000.00").toString());
            String protClass = (String) reqMap.getOrDefault("protectionClass", "3");

            // Bronze (Base Property only, $5k deductible)
            BigDecimal bronzePrem = com.guidewire.pc.service.CPRatingService.rateCommercialPropertyExtended(new PolicyPeriod(), bldg, bpp, protClass, false, false, true);

            // Silver (Base Property + Tenants Improvements $100k + Business Income $250k)
            BigDecimal tiSilver = com.guidewire.pc.service.CPRatingService.rateTenantsImprovements(new BigDecimal("100000.00"), "ReplacementCost");
            BigDecimal biSilver = com.guidewire.pc.service.CPRatingService.rateBusinessIncome(new BigDecimal("250000.00"), "1/3", true);
            BigDecimal silverPrem = bronzePrem.add(tiSilver).add(biSilver);

            // Gold (Silver + Boiler/Machinery $500k + Earthquake & Flood + Sprinkler)
            BigDecimal eqPrem = com.guidewire.pc.service.CPRatingService.rateBoilerAndMachinery(new BigDecimal("500000.00"), true);
            BigDecimal goldBase = com.guidewire.pc.service.CPRatingService.rateCommercialPropertyExtended(new PolicyPeriod(), bldg, bpp, protClass, true, true, true);
            BigDecimal goldPrem = goldBase.add(tiSilver).add(biSilver).add(eqPrem);

            Map<String, Object> comparison = new HashMap<>();
            comparison.put("bronzePackage", Map.of("tierName", "Bronze Essential", "annualPremium", bronzePrem, "deductible", "$5,000", "includedCoverages", List.of("Building & BPP", "Standard Fire & Wind")));
            comparison.put("silverPackage", Map.of("tierName", "Silver Preferred", "annualPremium", silverPrem, "deductible", "$2,500", "includedCoverages", List.of("Building & BPP", "Tenants Improvements ($100k)", "Business Income ($250k)", "Sprinkler Leakage")));
            comparison.put("goldPackage", Map.of("tierName", "Gold Enterprise Comprehensive", "annualPremium", goldPrem, "deductible", "$1,000", "includedCoverages", List.of("Building & BPP", "Tenants Improvements ($100k)", "Business Income ($250k)", "Equipment Breakdown ($500k)", "Earthquake & Flood", "24/7 Loss Recovery")));
            comparison.put("status", "SUCCESS");

            objectMapper.writeValue(resp.getWriter(), comparison);
            return;
        }

        if (path != null && path.equals("/wc/retro/calculate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal stdPrem = new BigDecimal(reqMap.getOrDefault("standardPremium", "100000.00").toString());
            BigDecimal losses = new BigDecimal(reqMap.getOrDefault("incurredLosses", "35000.00").toString());
            BigDecimal basicFac = new BigDecimal(reqMap.getOrDefault("basicPremiumFactor", "0.220").toString());
            BigDecimal lcf = new BigDecimal(reqMap.getOrDefault("lossConversionFactor", "1.150").toString());
            BigDecimal taxMult = new BigDecimal(reqMap.getOrDefault("stateTaxMultiplier", "1.050").toString());
            BigDecimal minFac = new BigDecimal(reqMap.getOrDefault("minimumPremiumFactor", "0.600").toString());
            BigDecimal maxFac = new BigDecimal(reqMap.getOrDefault("maximumPremiumFactor", "1.400").toString());

            var res = com.guidewire.pc.service.WCRetrospectiveRatingEngine.getInstance().calculateRetroRating(null, stdPrem, losses, basicFac, lcf, taxMult, minFac, maxFac);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.WCRetrospectiveRatingEngine.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/gl/composite/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal sales = new BigDecimal(reqMap.getOrDefault("grossSales", "2500000.00").toString());
            BigDecimal sqFt = new BigDecimal(reqMap.getOrDefault("squareFootage", "45000.00").toString());
            BigDecimal payroll = new BigDecimal(reqMap.getOrDefault("payroll", "600000.00").toString());
            BigDecimal ocp = new BigDecimal(reqMap.getOrDefault("ocpLimit", "1000000.00").toString());
            String liqTier = (String) reqMap.getOrDefault("liquorHazardTier", "TIER_1_RESTAURANT");
            BigDecimal liqSales = new BigDecimal(reqMap.getOrDefault("liquorSales", "250000.00").toString());
            BigDecimal pco = new BigDecimal(reqMap.getOrDefault("productsCompletedLimit", "2000000.00").toString());

            var res = com.guidewire.pc.service.GLCompositeRatingEngine.getInstance().rateCompositeGL(null, sales, sqFt, payroll, ocp, liqTier, liqSales, pco);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.GLCompositeRatingEngine.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/im/contractors-equipment/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            BigDecimal sched = new BigDecimal(reqMap.getOrDefault("scheduledLimit", "750000.00").toString());
            BigDecimal rented = new BigDecimal(reqMap.getOrDefault("rentedLimit", "150000.00").toString());
            BigDecimal unsched = new BigDecimal(reqMap.getOrDefault("unscheduledLimit", "50000.00").toString());
            String val = (String) reqMap.getOrDefault("valuationBasis", "REPLACEMENT_COST");
            boolean boom = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("boomOverload", true)));
            BigDecimal ded = new BigDecimal(reqMap.getOrDefault("deductible", "2500.00").toString());

            var res = com.guidewire.pc.service.ContractorsEquipmentEngine.getInstance().rateContractorsEquipment(null, sched, rented, unsched, val, boom, ded);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.ContractorsEquipmentEngine.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/auto/fleet-radius/rate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            int count = reqMap.get("vehicleCount") != null ? ((Number) reqMap.get("vehicleCount")).intValue() : 12;
            BigDecimal basePerUnit = new BigDecimal(reqMap.getOrDefault("basePremPerVehicle", "2200.00").toString());
            String radius = (String) reqMap.getOrDefault("operatingRadiusClass", "INTERMEDIATE");
            String hazmat = (String) reqMap.getOrDefault("dotHazmatClass", "CLASS_3_FLAMMABLE");
            boolean pollution = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("attachCa9948Pollution", true)));

            var res = com.guidewire.pc.service.AutoFleetRadiusHazmatEngine.getInstance().rateFleetRadiusAndHazmat(null, count, basePerUnit, radius, hazmat, pollution);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.AutoFleetRadiusHazmatEngine.getInstance().toMap(res));
            return;
        }

        if (path != null && (path.equals("/accelerator/sos-verify") || path.equals("/sos/verify"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String name = (String) reqMap.getOrDefault("businessName", "Apex Global Industrial Corp");
            String fein = (String) reqMap.getOrDefault("fein", "94-8192014");
            String state = (String) reqMap.getOrDefault("state", "DE");

            var res = com.guidewire.pc.service.SOSEntityVerificationService.getInstance().verifyBusinessEntity(name, fein, state);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.SOSEntityVerificationService.getInstance().toMap(res));
            return;
        }

        if (path != null && (path.equals("/accelerator/ofac-screen") || path.equals("/ofac/screen"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String subject = (String) reqMap.getOrDefault("screenedSubject", "Apex Commercial Logistics");
            String country = (String) reqMap.getOrDefault("country", "USA");
            String type = (String) reqMap.getOrDefault("subjectType", "COMMERCIAL_ORGANIZATION");

            var res = com.guidewire.pc.service.SanctionsComplianceService.getInstance().screenSubject(subject, country, type);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.SanctionsComplianceService.getInstance().toMap(res));
            return;
        }

        if (path != null && (path.equals("/accelerator/binder-explainer") || path.equals("/binder/explainer"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String jobNum = (String) reqMap.get("jobNumber");
            PolicyPeriod period = jobNum != null ? dataStore.findSubmission(jobNum) : null;
            if (period == null) {
                period = new PolicyPeriod();
                period.setPolicyNumber("POL-COMM-2026-8801");
                period.setProductCode((String) reqMap.getOrDefault("productCode", "CommercialProperty"));
                period.setTotalPremium(new BigDecimal(reqMap.getOrDefault("totalPremium", "18500.00").toString()));
            }

            var res = com.guidewire.pc.service.PolicyBinderExplainerService.getInstance().generateExecutiveSummary(period);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PolicyBinderExplainerService.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/policy/split-rewrite")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String parentNum = (String) reqMap.getOrDefault("parentPolicyNumber", "POL-PARENT-1001");
            PolicyPeriod parent = dataStore.findPolicyByPolicyNumber(parentNum);
            String newInsured = (String) reqMap.getOrDefault("newNamedInsured", "Apex West Coast Logistics LLC");
            double ratio = reqMap.get("transferRatio") != null ? ((Number) reqMap.get("transferRatio")).doubleValue() : 0.35;

            var res = com.guidewire.pc.service.PolicySplitRewriteService.getInstance().executePolicySplit(parent, newInsured, null, ratio);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.PolicySplitRewriteService.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/loss-control/recommendations")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-COMM-8801");
            String loc = (String) reqMap.getOrDefault("inspectedLocation", "100 Industrial Pkwy, Building A");
            boolean elec = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("hasCriticalElectricalFlaw", false)));
            boolean cook = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("hasCookingHazards", true)));

            var res = com.guidewire.pc.service.LossControlInspectionService.getInstance().generateSurveyReport(pol, loc, elec, cook);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.LossControlInspectionService.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/parametric/event-cancellation")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String evName = (String) reqMap.getOrDefault("eventName", "Austin City Outdoor Music Festival");
            String evDate = (String) reqMap.getOrDefault("eventDate", "2026-10-15");
            String venue = (String) reqMap.getOrDefault("eventVenueLocation", "Zilker Park, Austin, TX");
            BigDecimal lim = new BigDecimal(reqMap.getOrDefault("eventGrossRevenueLimit", "500000.00").toString());
            String trig = (String) reqMap.getOrDefault("triggerType", "RAINFALL_ACCUMULATION");
            double threshold = reqMap.get("triggerThresholdValue") != null ? ((Number) reqMap.get("triggerThresholdValue")).doubleValue() : 1.25;
            double observed = reqMap.get("observedTelemetryReading") != null ? ((Number) reqMap.get("observedTelemetryReading")).doubleValue() : 1.65;

            var quote = com.guidewire.pc.service.ParametricEventCancellationEngine.getInstance().quoteParametricEndorsement(null, evName, evDate, venue, lim, trig, threshold);
            var settlement = com.guidewire.pc.service.ParametricEventCancellationEngine.getInstance().evaluateTelemetryTrigger(quote, observed);

            Map<String, Object> combined = new HashMap<>();
            combined.put("quote", com.guidewire.pc.service.ParametricEventCancellationEngine.getInstance().toMap(quote));
            combined.put("settlement", Map.of(
                    "isTriggerBreached", settlement.isTriggerBreached,
                    "observedTelemetryValue", settlement.observedTelemetryValue,
                    "contractThresholdValue", settlement.contractThresholdValue,
                    "automaticClaimSettlementAmount", settlement.automaticClaimSettlementAmount,
                    "claimsStatus", settlement.claimsStatus,
                    "settlementExplanation", settlement.settlementExplanation
            ));
            combined.put("status", "SUCCESS");
            objectMapper.writeValue(resp.getWriter(), combined);
            return;
        }

        if (path != null && path.equals("/billing/agency-account-current")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String code = (String) reqMap.getOrDefault("producerCode", "PR-WEST-901");
            String name = (String) reqMap.getOrDefault("agencyName", "Pacific Coast Commercial Brokers Inc");
            String month = (String) reqMap.getOrDefault("billingMonth", "2026-08");

            var res = com.guidewire.pc.service.AgencyBillAccountCurrentService.getInstance().generateAccountCurrent(code, name, month, null);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.AgencyBillAccountCurrentService.getInstance().toMap(res));
            return;
        }

        if (path != null && path.equals("/reinsurance/cat-reinstatement")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String treatyRef = (String) reqMap.getOrDefault("treatyReferenceNumber", "TREATY-CAT-2026-LAYER2");
            String layerDesc = (String) reqMap.getOrDefault("treatyLayerDescription", "$50M xs $25M Coastal Hurricane CAT XOL");
            BigDecimal limit = new BigDecimal(reqMap.getOrDefault("treatyLayerLimit", "50000000.00").toString());
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("treatyAnnualCededPremium", "4000000.00").toString());
            BigDecimal loss = new BigDecimal(reqMap.getOrDefault("catastrophicLossAmount", "30000000.00").toString());
            String eff = (String) reqMap.getOrDefault("treatyEffectiveDate", "2026-01-01");
            String lossDate = (String) reqMap.getOrDefault("dateOfLoss", "2026-07-01");
            String exp = (String) reqMap.getOrDefault("treatyExpirationDate", "2027-01-01");
            double rate = reqMap.get("reinstatementRatePct") != null ? ((Number) reqMap.get("reinstatementRatePct")).doubleValue() : 100.0;

            var res = com.guidewire.pc.service.CatReinsuranceReinstatementEngine.getInstance().calculateCatReinstatement(treatyRef, layerDesc, limit, prem, loss, eff, lossDate, exp, rate);
            objectMapper.writeValue(resp.getWriter(), com.guidewire.pc.service.CatReinsuranceReinstatementEngine.getInstance().toMap(res));
            return;
        }

        if (path != null && (path.equals("/cyber/accumulation-assess") || path.equals("/cyber/rate-and-assess"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-CYBER-8801");
            BigDecimal lim = new BigDecimal(reqMap.getOrDefault("requestedLimit", "2000000.00").toString());
            boolean mfa = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("mfaEnforced", true)));
            boolean backups = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("offlineBackupsDaily", true)));
            boolean edr = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("edrDeployed", true)));
            double phish = reqMap.get("phishingPassRatePct") != null ? ((Number) reqMap.get("phishingPassRatePct")).doubleValue() : 92.0;
            boolean dns = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("dnsFilteringActive", true)));
            String cloud = (String) reqMap.getOrDefault("primaryCloudProvider", "AWS");
            BigDecimal rev = new BigDecimal(reqMap.getOrDefault("annualRevenue", "15000000.00").toString());

            var res = com.guidewire.pc.service.CyberAccumulationEngine.getInstance().evaluateAndRate(pol, lim, mfa, backups, edr, phish, dns, cloud, rev);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/surety/bond-capacity-and-quote") || path.equals("/surety/quote"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String name = (String) reqMap.getOrDefault("contractorName", "Apex General Contractors LLC");
            String type = (String) reqMap.getOrDefault("bondType", "PERFORMANCE");
            BigDecimal contractSum = new BigDecimal(reqMap.getOrDefault("contractSum", "1500000.00").toString());
            BigDecimal wc = new BigDecimal(reqMap.getOrDefault("workingCapital", "450000.00").toString());
            BigDecimal nw = new BigDecimal(reqMap.getOrDefault("netWorth", "900000.00").toString());
            BigDecimal wip = new BigDecimal(reqMap.getOrDefault("activeWIP", "1100000.00").toString());
            BigDecimal col = new BigDecimal(reqMap.getOrDefault("collateralHeld", "0.00").toString());
            String obligee = (String) reqMap.getOrDefault("obligeeName", "State Dept of Transportation");
            String obType = (String) reqMap.getOrDefault("obligeeType", "PUBLIC_STATE_MUNICIPAL");

            var res = com.guidewire.pc.service.SuretyBondService.getInstance().evaluateBondAndCapacity(name, type, contractSum, wc, nw, wip, col, obligee, obType);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/marine/rate-voyage-cargo") || path.equals("/marine/rate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-MAR-5001");
            BigDecimal inv = new BigDecimal(reqMap.getOrDefault("invoiceValue", "750000.00").toString());
            String comm = (String) reqMap.getOrDefault("commodityType", "GENERAL_DRY");
            String clause = (String) reqMap.getOrDefault("clauseType", "CLAUSES_A_ALL_RISK");
            String vessel = (String) reqMap.getOrDefault("vesselName", "MV Pacific Navigator");
            int age = reqMap.get("vesselAge") != null ? ((Number) reqMap.get("vesselAge")).intValue() : 8;
            String flag = (String) reqMap.getOrDefault("flagState", "MARSHALL_ISLANDS");
            boolean war = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("warStrikesIncluded", true)));
            boolean piracy = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("piracyCorridorIncluded", false)));
            BigDecimal portExp = new BigDecimal(reqMap.getOrDefault("existingPortExposure", "1200000.00").toString());

            var res = com.guidewire.pc.service.OceanMarineRatingEngine.getInstance().rateVoyageCargo(pol, inv, comm, clause, vessel, age, flag, war, piracy, portExp);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/actuarial/simulate-rate-impact") || path.equals("/actuarial/simulate"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            double cp = reqMap.get("propertyRateDeltaPct") != null ? ((Number) reqMap.get("propertyRateDeltaPct")).doubleValue() : 8.0;
            double gl = reqMap.get("generalLiabilityDeltaPct") != null ? ((Number) reqMap.get("generalLiabilityDeltaPct")).doubleValue() : 4.5;
            double ca = reqMap.get("commercialAutoDeltaPct") != null ? ((Number) reqMap.get("commercialAutoDeltaPct")).doubleValue() : 12.0;
            double wc = reqMap.get("workersCompDeltaPct") != null ? ((Number) reqMap.get("workersCompDeltaPct")).doubleValue() : -2.0;
            double cyber = reqMap.get("cyberDeltaPct") != null ? ((Number) reqMap.get("cyberDeltaPct")).doubleValue() : 15.0;

            var res = com.guidewire.pc.service.PortfolioSimulationService.getInstance().simulateRateChanges(cp, gl, ca, wc, cyber);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/intake/parse-acord-loss-runs") || path.equals("/intake/triage"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String name = (String) reqMap.getOrDefault("insuredName", "Horizon Logistics & Warehousing Inc");
            String lob = (String) reqMap.getOrDefault("lineOfBusiness", "COMMERCIAL_AUTO");
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("estimatedAnnualPremium", "45000.00").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lossData = (List<Map<String, Object>>) reqMap.get("annualLossData");

            var res = com.guidewire.pc.service.AcordLossRunParserService.getInstance().parseLossRunsAndTriage(name, lob, prem, lossData);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/parametric/oracle-evaluate") || path.equals("/parametric/oracle"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-PARAMETRIC-7701");
            String peril = (String) reqMap.getOrDefault("perilType", "EARTHQUAKE_USGS");
            String loc = (String) reqMap.getOrDefault("locationAddress", "San Francisco Waterfront Port, CA");
            double lat = reqMap.get("latitude") != null ? ((Number) reqMap.get("latitude")).doubleValue() : 37.7749;
            double lon = reqMap.get("longitude") != null ? ((Number) reqMap.get("longitude")).doubleValue() : -122.4194;
            BigDecimal lim = new BigDecimal(reqMap.getOrDefault("contractLimit", "2500000.00").toString());
            double tele = reqMap.get("measuredTelemetryValue") != null ? ((Number) reqMap.get("measuredTelemetryValue")).doubleValue() : 0.72;

            var res = com.guidewire.pc.service.ParametricOracleEngine.getInstance().evaluatePerilTrigger(pol, peril, loc, lat, lon, lim, tele);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && path.equals("/policy/effdated-slices")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-COMM-9901");
            String inc = (String) reqMap.getOrDefault("termInceptionDate", "2026-01-01");
            String exp = (String) reqMap.getOrDefault("termExpirationDate", "2027-01-01");
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("baseAnnualPremium", "24000.00").toString());
            boolean oos = Boolean.parseBoolean(String.valueOf(reqMap.getOrDefault("includeOosEndorsement", true)));

            var res = com.guidewire.pc.service.EffDatedTimelineVisualizerService.getInstance().generateTimelineSlices(pol, inc, exp, prem, oos);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/dealroom/escalate") || path.equals("/dealroom/create"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-DEAL-1001");
            String name = (String) reqMap.getOrDefault("insuredName", "Titan Heavy Manufacturing Corp");
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("totalPremium", "350000.00").toString());
            BigDecimal lim = new BigDecimal(reqMap.getOrDefault("aggregateLimit", "10000000.00").toString());
            String note = (String) reqMap.getOrDefault("initialNote", "Escalated for senior review due to high manufacturing capacity and $10M aggregate limits.");
            String cond = (String) reqMap.getOrDefault("condition", "Subjective: Satisfactory Loss Control survey within 45 days of binding.");

            var res = com.guidewire.pc.service.UWDealRoomService.getInstance().createOrEscalateDeal(pol, name, prem, lim, note, cond);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && path.equals("/dealroom/action")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-DEAL-1001");
            String act = (String) reqMap.getOrDefault("action", "APPROVE");
            String rev = (String) reqMap.getOrDefault("reviewerName", "Victoria Vance");
            String role = (String) reqMap.getOrDefault("role", "CHIEF_UNDERWRITING_OFFICER");
            String comment = (String) reqMap.getOrDefault("comment", "Approved under Special Risk delegated authority with subjective survey clause.");

            var res = com.guidewire.pc.service.UWDealRoomService.getInstance().performAction(pol, act, rev, role, comment);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/billing/premium-finance/quote-and-noa") || path.equals("/billing/premium-finance"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String pol = (String) reqMap.getOrDefault("policyNumber", "POL-COMM-8001");
            BigDecimal prem = new BigDecimal(reqMap.getOrDefault("policyPremium", "50000.00").toString());
            String pfc = (String) reqMap.getOrDefault("financeCompanyName", "AFCO Premium Credit Corp");
            double down = reqMap.get("downPaymentPercent") != null ? ((Number) reqMap.get("downPaymentPercent")).doubleValue() : 20.0;
            double apr = reqMap.get("aprPct") != null ? ((Number) reqMap.get("aprPct")).doubleValue() : 8.75;
            int inst = reqMap.get("installmentCount") != null ? ((Number) reqMap.get("installmentCount")).intValue() : 9;

            var res = com.guidewire.pc.service.PremiumFinanceEngine.getInstance().createFinanceAgreementAndNOA(pol, prem, pfc, down, apr, inst);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && (path.equals("/gosu/repl-evaluate") || path.equals("/gosu/execute"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqMap = objectMapper.readValue(req.getInputStream(), Map.class);
            String gosu = (String) reqMap.getOrDefault("gosuCode", "var baseRate = 0.35;\nvar premium = period.TotalPremium * baseRate;\nreturn premium;");
            BigDecimal lim = new BigDecimal(reqMap.getOrDefault("sampleLimit", "1000000.00").toString());
            int iters = reqMap.get("benchmarkIterations") != null ? ((Number) reqMap.get("benchmarkIterations")).intValue() : 1000;

            var res = com.guidewire.pc.service.GosuReplEngine.getInstance().evaluateGosuScript(gosu, lim, iters);
            objectMapper.writeValue(resp.getWriter(), res.toMap());
            return;
        }

        if (path != null && path.equals("/admin/reset-db")) {
            dataStore.resetToSeedData();
            objectMapper.writeValue(resp.getWriter(), Map.of("status", "Success", "message", "Database reset to clean sample seed data."));
        }
    }

    private void serveOpenApiJson(HttpServletResponse resp) throws IOException {
        LOGGER.log(Level.FINE, "→ GuidewireRestServlet.serveOpenApiJson");
        Map<String, Object> openApi = new HashMap<>();
        openApi.put("openapi", "3.0.1");
        openApi.put("info", Map.of(
                "title", "Guidewire PolicyCenter Cloud REST API",
                "version", "1.0.0",
                "description", "OOTB Guidewire PolicyCenter Digital & REST Integration Framework"
        ));
        openApi.put("paths", Map.of(
                "/rest/v1/accounts", Map.of("get", Map.of("summary", "List all active accounts"), "post", Map.of("summary", "Create new account")),
                "/rest/v1/jobs", Map.of("get", Map.of("summary", "List all policy submissions/jobs"), "post", Map.of("summary", "Create new submission")),
                "/rest/v1/jobs/{jobNumber}/quote", Map.of("post", Map.of("summary", "Rate and Quote policy job")),
                "/rest/v1/jobs/{jobNumber}/issue", Map.of("post", Map.of("summary", "Bind and Issue policy job"))
        ));
        objectMapper.writeValue(resp.getWriter(), openApi);
    }
}