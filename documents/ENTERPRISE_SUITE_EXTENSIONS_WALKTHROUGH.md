# Guidewire PolicyCenter Enterprise Extensions (2026 Suite) Walkthrough

## Overview
This document details the architecture, formulas, entity schemas, and REST endpoints for the **10 Enterprise Features** developed for **Guidewire PolicyCenter (`Guidewire_PC_Java_Gosu`)**.

---

## Features Matrix

| # | Feature Name | Core Java Service | Gosu / Entity Schema | Primary Endpoint |
| :-: | :--- | :--- | :--- | :--- |
| **1** | **Commercial Cyber Risk & Cloud Outage Accumulation** | `CyberAccumulationEngine.java` | `CyberRatingEngine.gs`, `CyberRiskAssessment.eti`, `CyberCoverageOption.eti` | `POST /rest/v1/cyber/accumulation-assess` |
| **2** | **Surety Bonds & Commercial Contract Guarantee** | `SuretyBondService.java` | `SuretyBondEngine.gs`, `SuretyBond.eti`, `ContractorBondCapacity.eti`, `Obligee.eti` | `POST /rest/v1/surety/bond-capacity-and-quote` |
| **3** | **Commercial Cargo & Ocean Marine Transit Floater** | `OceanMarineRatingEngine.java` | `OceanMarineEngine.gs`, `OceanMarinePolicyLine.eti`, `MarineCargoCov.eti`, `TransitVoyageSchedule.eti` | `POST /rest/v1/marine/rate-voyage-cargo` |
| **4** | **Actuarial Rate Change & Portfolio "What-If" Simulator** | `PortfolioSimulationService.java` | N/A | `POST /rest/v1/actuarial/simulate-rate-impact` |
| **5** | **AI Autonomous Submission Intake & ACORD Loss Run Parser 2.0** | `AcordLossRunParserService.java` | N/A | `POST /rest/v1/intake/parse-acord-loss-runs` |
| **6** | **Real-Time USGS / NOAA Parametric Catastrophe Oracle** | `ParametricOracleEngine.java` | N/A | `POST /rest/v1/parametric/oracle-evaluate` |
| **7** | **Interactive EffDated Policy Slice Visualizer & Explorer** | `EffDatedTimelineVisualizerService.java`| N/A | `POST /rest/v1/policy/effdated-slices`, `GET /rest/v1/policy/effdated-timeline` |
| **8** | **Underwriter Collaborative Deal Room & Authority Workflow** | `UWDealRoomService.java` | N/A | `POST /rest/v1/dealroom/escalate`, `POST /rest/v1/dealroom/action` |
| **9** | **Commercial Premium Financing (PFC) & NOA Engine** | `PremiumFinanceEngine.java` | `PremiumFinanceAgreement.eti` | `POST /rest/v1/billing/premium-finance/quote-and-noa` |
| **10**| **Interactive Gosu Rules & Rating Sandbox (REPL)** | `GosuReplEngine.java` | N/A | `POST /rest/v1/gosu/repl-evaluate` |

---

## Detailed Specifications

### 1. Commercial Cyber Risk & Cloud Outage Accumulation Engine
- **Posture Scoring**: Evaluates MFA (+30), Immutable Backups (+25), EDR (+20), Phishing Pass Rate (+15), DNS Filtering (+10).
- **Tiers**:
  - `PREFERRED_TIER_1` (Score $\ge 85$): $-20\%$ credit, $100\%$ ransomware limit.
  - `STANDARD_TIER_2` (Score $65-84$): Base rate, $\$1\text{M}$ ransomware cap.
  - `SUBSTANDARD_TIER_3` (Score $45-64$): $+35\%$ surcharge, $\$500\text{k}$ cap.
  - `HIGH_RISK_TIER_4` (Score $<45$): $+75\%$ surcharge, $\$250\text{k}$ cap, mandatory CUO referral.
- **Cloud Dependency**: Models systemic accumulation factors for AWS ($1.10\times$), Azure ($1.08\times$), GCP ($1.05\times$), Multi-Cloud ($0.95\times$).

### 2. Surety Bonds & Commercial Contract Guarantee Engine
- **Capacity Formulas**:
  $$\text{Single Job Capacity} = \min(\text{Working Capital} \times 10, \text{Net Worth} \times 4)$$
  $$\text{Aggregate Capacity} = \min(\text{Working Capital} \times 15, \text{Net Worth} \times 7)$$
  $$\text{Available Capacity} = \text{Aggregate Capacity} - \text{Active WIP Backlog}$$
- **Rating**: Sliding scale for Performance & Payment bonds ($2.5\%$ on first $\$100\text{k}$, $1.5\%$ on next $\$400\text{k}$, $1.0\%$ on next $\$2\text{M}$, $0.75\%$ over $\$2.5\text{M}$).

### 3. Commercial Cargo & Ocean Marine Transit Floater
- **Valuation**: $\text{Insured Cargo Value} = \text{CIF Invoice} \times 110\%$.
- **Institute Cargo Clauses**: Clause A (All Risk, $0.45\%$), Clause B (Named Perils, $0.30\%$), Clause C (Major Casualties, $0.20\%$).
- **Perils**: Institute War & Strikes ($+0.12\%$), Piracy / High-Risk Corridor ($+0.35\%$).
- **Port Accumulation**: Real-time port catastrophe threshold monitoring against $\$5\text{M}$ aggregate cap.

### 4. Actuarial Rate Change & Portfolio "What-If" Impact Simulator
- Simulates line-specific rate adjustments across active in-force policies.
- Calculates total earned premium variance, price elasticity renewal retention ($R = 92.0 \cdot e^{-0.35 \cdot \Delta P}$), and projected loss ratio improvements.

### 5. AI Autonomous Submission Intake & ACORD Loss Run Parser 2.0
- Computes 5-year Loss Development Triangles (Paid vs Incurred Losses by Policy Year).
- Detects large losses ($>\$50\text{k}$) and loss frequency anomalies.
- Automates underwriting triage routing (`STRAIGHT_THROUGH_AUTO_BIND` vs `MANDATORY_UW_REFERRAL`).

### 6. Real-Time USGS / NOAA Parametric Catastrophe Oracle Platform
- **Earthquake (USGS)**: Peak Ground Acceleration ($0.20g \rightarrow 25\%$, $0.40g \rightarrow 50\%$, $0.65g \rightarrow 100\%$).
- **Hurricane (NOAA)**: Central Barometric Pressure ($\le 960$ mbar $\rightarrow 50\%$, $\le 935$ mbar $\rightarrow 100\%$).
- Auto-generates simulated FedNow / Wire transfer settlement and calculates $75\%$ Catastrophe Reinsurance Treaty recoveries.

### 7. Interactive EffDated Policy Slice Visualizer & Time-Travel Explorer
- Decomposes versioned policy periods into continuous non-overlapping slices ($[t_0, t_1), [t_1, t_2), \dots$).
- Accurately tracks pro-rata earned premiums and marks retroactive Out-of-Sequence endorsement slices.

### 8. Underwriter Collaborative Deal Room & Referral Authority Workflow
- Multi-tier authority routing (`LINE_UNDERWRITER`, `SENIOR_UNDERWRITER`, `REGIONAL_UW_DIRECTOR`, `CHIEF_UNDERWRITING_OFFICER`).
- Subjective binder conditions tracking, real-time case notes, and digital audit signatures (`AUDIT-SIG-...`).

### 9. Commercial Premium Financing (PFC) & Notice of Assignment Engine
- Calculates $20-25\%$ down payment requirements, APR interest schedules ($8, 9, 10$ installments).
- Generates electronic Notice of Assignment (NOA) payloads and Power of Attorney (POA) cancellation rights.

### 10. Interactive Gosu Rules & Rating Sandbox (Web REPL & Loom Benchmark)
- Live Gosu expression evaluation against in-memory policy periods.
- Concurrency benchmarks comparing Java 23 Virtual Threads vs Platform Thread pools.

---

## Verification & Test Results
- **Full Test Suite Run**: 347 tests executed with **0 failures, 0 errors, 0 skipped**.
- **Server Health**: Embedded Jetty PolicyCenter server live on port 8085.
- **REST Endpoints**: Verified with live token authentication and JSON response validation.
