package com.guidewire.pc.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Underwriter Collaborative Deal Room & Multi-Tier Authority Escalation Service.
 */
public class UWDealRoomService {
    private static final Logger LOGGER = Logger.getLogger(UWDealRoomService.class.getName());
    private static final UWDealRoomService INSTANCE = new UWDealRoomService();

    public static UWDealRoomService getInstance() {
        return INSTANCE;
    }

    private final Map<String, DealRoomThread> dealRooms = new ConcurrentHashMap<>();

    public static class UWNote {
        public String author;
        public String role;
        public String timestamp;
        public String message;
    }

    public static class DealRoomThread {
        public String dealId;
        public String policyNumber;
        public String insuredName;
        public BigDecimal totalPremium;
        public BigDecimal aggregateLimit;
        public String requiredAuthorityLevel;
        public String currentStatus;
        public String assignedReviewer;
        public List<String> subjectiveConditions = new ArrayList<>();
        public List<UWNote> notes = new ArrayList<>();
        public String approvedBy;
        public String approvalTimestamp;
        public String digitalAuditStamp;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("dealId", dealId);
            map.put("policyNumber", policyNumber);
            map.put("insuredName", insuredName);
            map.put("totalPremium", totalPremium);
            map.put("aggregateLimit", aggregateLimit);
            map.put("requiredAuthorityLevel", requiredAuthorityLevel);
            map.put("currentStatus", currentStatus);
            map.put("assignedReviewer", assignedReviewer);
            map.put("subjectiveConditions", subjectiveConditions);
            map.put("notes", notes);
            map.put("approvedBy", approvedBy);
            map.put("approvalTimestamp", approvalTimestamp);
            map.put("digitalAuditStamp", digitalAuditStamp);
            return map;
        }
    }

    public DealRoomThread createOrEscalateDeal(String policyNumber,
                                               String insuredName,
                                               BigDecimal totalPremium,
                                               BigDecimal aggregateLimit,
                                               String initialNote,
                                               String condition) {
        LOGGER.log(Level.INFO, "Creating/Escalating Deal Room for policy: {0}", policyNumber);

        String pNum = (policyNumber != null && !policyNumber.isBlank()) ? policyNumber : "POL-DEAL-1001";
        DealRoomThread thread = dealRooms.computeIfAbsent(pNum, k -> {
            DealRoomThread t = new DealRoomThread();
            t.dealId = "DEAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            t.policyNumber = k;
            return t;
        });

        thread.insuredName = (insuredName != null && !insuredName.isBlank()) ? insuredName : "Titan Heavy Manufacturing Corp";
        thread.totalPremium = (totalPremium != null) ? totalPremium : new BigDecimal("350000.00");
        thread.aggregateLimit = (aggregateLimit != null) ? aggregateLimit : new BigDecimal("10000000.00");

        // Determine required authority level
        if (thread.totalPremium.compareTo(new BigDecimal("1000000.00")) > 0 || thread.aggregateLimit.compareTo(new BigDecimal("10000000.00")) > 0) {
            thread.requiredAuthorityLevel = "CHIEF_UNDERWRITING_OFFICER";
            thread.assignedReviewer = "Victoria Vance (CUO)";
        } else if (thread.totalPremium.compareTo(new BigDecimal("250000.00")) > 0 || thread.aggregateLimit.compareTo(new BigDecimal("2000000.00")) > 0) {
            thread.requiredAuthorityLevel = "REGIONAL_UW_DIRECTOR";
            thread.assignedReviewer = "Marcus Sterling (Regional Director)";
        } else if (thread.totalPremium.compareTo(new BigDecimal("50000.00")) > 0) {
            thread.requiredAuthorityLevel = "SENIOR_UNDERWRITER";
            thread.assignedReviewer = "Elena Rostova (Senior UW)";
        } else {
            thread.requiredAuthorityLevel = "LINE_UNDERWRITER";
            thread.assignedReviewer = "Auto-assigned Line UW";
        }

        thread.currentStatus = "PENDING_AUTHORITY_REVIEW";

        if (condition != null && !condition.isBlank() && !thread.subjectiveConditions.contains(condition)) {
            thread.subjectiveConditions.add(condition);
        } else if (thread.subjectiveConditions.isEmpty()) {
            thread.subjectiveConditions.add("Subjective: Satisfactory Loss Control survey within 45 days of binding.");
            thread.subjectiveConditions.add("Subjective: Verified audited financial statements showing positive net working capital.");
        }

        if (initialNote != null && !initialNote.isBlank()) {
            addNote(thread, "Submitting Broker / UW", "Underwriter", initialNote);
        } else if (thread.notes.isEmpty()) {
            addNote(thread, "Sarah Jenkins", "Line Underwriter", "Escalated for senior review due to high manufacturing capacity and $10M aggregate limits.");
        }

        return thread;
    }

    public DealRoomThread performAction(String policyNumber, String action, String reviewerName, String role, String comment) {
        LOGGER.log(Level.INFO, "Performing Deal Room Action: {0} for policy: {1}", new Object[]{action, policyNumber});

        DealRoomThread thread = dealRooms.get(policyNumber);
        if (thread == null) {
            thread = createOrEscalateDeal(policyNumber, "Commercial Enterprise Insured", new BigDecimal("125000.00"), new BigDecimal("5000000.00"), null, null);
        }

        String rev = (reviewerName != null && !reviewerName.isBlank()) ? reviewerName : thread.assignedReviewer;
        String rRole = (role != null && !role.isBlank()) ? role : thread.requiredAuthorityLevel;

        if ("APPROVE".equalsIgnoreCase(action)) {
            thread.currentStatus = "APPROVED_WITH_CONDITIONS";
            thread.approvedBy = rev + " (" + rRole + ")";
            thread.approvalTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            thread.digitalAuditStamp = "AUDIT-SIG-" + UUID.randomUUID().toString().toUpperCase();
            addNote(thread, rev, rRole, "APPROVED: " + (comment != null ? comment : "Underwriting sign-off granted within authority delegation limits."));
        } else if ("REJECT".equalsIgnoreCase(action)) {
            thread.currentStatus = "DECLINED_EXCEEDS_APPETITE";
            addNote(thread, rev, rRole, "DECLINED: " + (comment != null ? comment : "Risk does not meet portfolio hazard profile or capacity limits."));
        } else {
            // Note or Inquiry
            if (comment != null && !comment.isBlank()) {
                addNote(thread, rev, rRole, comment);
            }
        }

        return thread;
    }

    private void addNote(DealRoomThread thread, String author, String role, String message) {
        UWNote n = new UWNote();
        n.author = author;
        n.role = role;
        n.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        n.message = message;
        thread.notes.add(n);
    }

    public DealRoomThread getDealRoom(String policyNumber) {
        if (policyNumber == null) return null;
        return dealRooms.get(policyNumber);
    }
}
