import { FeatureModule } from './types/features';

export const ENTERPRISE_FEATURES_CATALOG: FeatureModule[] = [
  {
    id: 'ai-underwriting',
    title: 'AI Automated Underwriting Referral & Decision Assistant',
    category: 'Underwriting & Risk',
    shortDescription: 'AI-driven decision assistant evaluating loss history, hazard class codes, and risk scores to provide automated binding recommendations or manager escalation.',
    businessPurpose: 'Accelerates underwriting triage and provides predictive AI risk explanations for complex commercial applications.',
    endpoint: '/rest/v1/ai-referral/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'riskScore', label: 'Risk Score (0-100)', type: 'number', defaultValue: 78, description: 'Composite underwriting risk score' }
    ]
  },
  {
    id: 'esignature-docusign',
    title: 'DocuSign E-Signature Envelope Integration Engine',
    category: 'Compliance & Regulatory',
    shortDescription: 'Generates secure DocuSign e-signature envelope packages for instant digital policy binding and statutory application execution.',
    businessPurpose: 'Automates digital policy binding and application sign-offs via seamless DocuSign API integration.',
    endpoint: '/rest/v1/esignature/create',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'signerEmail', label: 'Signer Email Address', type: 'string', defaultValue: 'policyholder@example.com', description: 'Named Insured email for e-signature' }
    ]
  },
  {
    id: 'geospatial-gis',
    title: 'Geospatial GIS Risk & Wildfire Exposure Service',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Evaluates GIS latitude/longitude coordinates against live wildfire risk zones, coastal storm surge maps, and sinkhole fault lines.',
    businessPurpose: 'Protects carrier portfolio concentration by evaluating precise property coordinates for natural hazard exposures.',
    endpoint: '/rest/v1/geospatial/risk',
    method: 'POST',
    inputs: [
      { name: 'address', label: 'Property Address', type: 'string', defaultValue: '100 Coastal Hwy, Malibu, CA 90265', description: 'Insured property physical location' }
    ]
  },
  {
    id: 'payment-gateway',
    title: 'Stripe Payment Gateway Installment Processing Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Processes real-time credit card, ACH, and installment payments with tokenized security via Stripe Payment Gateway.',
    businessPurpose: 'Enables direct digital payment collection upon policy quote and renewal binding.',
    endpoint: '/rest/v1/payment/process',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'amount', label: 'Payment Amount ($)', type: 'number', defaultValue: 450.00, description: 'Installment or deposit payment amount' }
    ]
  },
  {
    id: 'vin-lookup',
    title: 'NHTSA VIN Decoder & Vehicle Safety Feature Lookup Engine',
    category: 'Specialty Lines',
    shortDescription: 'Decodes 17-digit VIN numbers to populate vehicle make, model, trim, NHTSA safety ratings, and ADAS anti-theft equipment.',
    businessPurpose: 'Automates vehicle schedule data entry and applies safety equipment rating credits for Commercial and Personal Auto.',
    endpoint: '/rest/v1/vin/decode',
    method: 'POST',
    inputs: [
      { name: 'vin', label: '17-Digit Vehicle VIN', type: 'string', defaultValue: '1G1YC2D45R5100001', description: 'Standard vehicle identification number' }
    ]
  },
  {
    id: 'telematics-ubi',
    title: 'Auto Fleet Telematics Driving Behavior Premium Discount Engine (UBI)',
    category: 'Specialty Lines',
    shortDescription: 'Evaluates driver safety telemetry (hard braking, rapid acceleration, late-night driving) for dynamic UBI discounts up to -20% or surcharges +15%.',
    businessPurpose: 'Commercial & Personal Auto lines use telematics telemetry to reward safe drivers with rate discounts while pricing high-risk behaviors appropriately on renewal.',
    endpoint: '/rest/v1/telematics/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'hardBrakesPer1k', label: 'Hard Brakes / 1k Miles', type: 'number', defaultValue: 1.0, description: 'Events per 1,000 miles driven' },
      { name: 'rapidAccelerationsPer1k', label: 'Rapid Accelerations / 1k Miles', type: 'number', defaultValue: 0.5, description: 'Events per 1,000 miles driven' },
      { name: 'lateNightDrivingPct', label: 'Late Night Driving % (12am-4am)', type: 'number', defaultValue: 0.02, description: 'Fraction of total driving during high-risk hours' },
      { name: 'speedingEventsPer1k', label: 'Speeding Events / 1k Miles', type: 'number', defaultValue: 0.0, description: 'Events exceeding limit by >10mph' }
    ]
  },
  {
    id: 'tria-compliance',
    title: 'TRIA Terrorism Risk Insurance Act Opt-In/Opt-Out Disclosure Engine',
    category: 'Compliance & Regulatory',
    shortDescription: 'Manages mandatory U.S. Federal TRIA 3.5% terrorism surcharge disclosures, opt-in endorsements, and rejection exclusion forms.',
    businessPurpose: 'Federal regulation requires commercial property and liability insurers to disclose certified terrorism coverage terms and attach mandatory TRIA rejection forms if declined.',
    endpoint: '/rest/v1/tria/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'optInTerrorismCoverage', label: 'Opt-In TRIA Coverage', type: 'boolean', defaultValue: true, description: 'Policyholder accepts certified terrorism coverage' },
      { name: 'triaRatePct', label: 'TRIA Surcharge Rate (%)', type: 'number', defaultValue: 0.035, description: 'Standard 3.5% federal terrorism rate' }
    ]
  },
  {
    id: 'pollution-hazard',
    title: 'Environmental & Pollution Liability Hazard Assessment Engine',
    category: 'Specialty Lines',
    shortDescription: 'Assesses Underground Storage Tanks (UST), chemical hazard scores, and proximity to waterways to calculate EIL rating multipliers and containment deductibles.',
    businessPurpose: 'Environmental Impairment Liability (EIL) underwriters evaluate hazardous storage and waterway proximity to enforce mandatory containment deductibles up to $50,000.',
    endpoint: '/rest/v1/pollution/assess',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'ustCount', label: 'Underground Storage Tanks (UST)', type: 'number', defaultValue: 4, description: 'Active tank count on facility grounds' },
      { name: 'chemicalHazardScore', label: 'Chemical Hazard Score (1-10)', type: 'number', defaultValue: 8, description: 'Storage volume and toxic rating score' },
      { name: 'proximityToWaterwayMiles', label: 'Proximity to Waterway (Miles)', type: 'number', defaultValue: 0.4, description: 'Distance to nearest river, lake or coastal bay' },
      { name: 'facilityAgeYears', label: 'Facility Age (Years)', type: 'number', defaultValue: 25, description: 'Age of industrial site structures' }
    ]
  },
  {
    id: 'cyber-liability',
    title: 'Cyber Liability Ransomware & Breach Response Sub-Limit Engine',
    category: 'Specialty Lines',
    shortDescription: 'Evaluates corporate cyber security posture (MFA, daily backups, EDR) and caps ransomware sub-limits at $250k with +30% surcharge if MFA is missing.',
    businessPurpose: 'Cyber underwriters enforce strict security controls like Multi-Factor Authentication (MFA) to prevent catastrophic ransomware losses.',
    endpoint: '/rest/v1/cyber/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'mfaEnabled', label: 'MFA Enabled on All Systems', type: 'boolean', defaultValue: false, description: 'Mandatory Multi-Factor Authentication' },
      { name: 'offlineBackupsDaily', label: 'Daily Offline Backups', type: 'boolean', defaultValue: true, description: 'Immutable daily offsite backups' },
      { name: 'edrDeployed', label: 'EDR Antivirus Deployed', type: 'boolean', defaultValue: true, description: 'Endpoint Detection and Response' },
      { name: 'employeePhishingTrained', label: 'Phishing Awareness Training', type: 'boolean', defaultValue: true, description: 'Quarterly staff phishing simulation training' }
    ]
  },
  {
    id: 'flood-zone-rating',
    title: 'Flood Zone Risk & NFIP Elevation Certificate Premium Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Evaluates FEMA Flood Zones (A, V, X) and Elevation Certificate differentials to grant -30% elevation credits or apply +50% below-BFE surcharges.',
    businessPurpose: 'Commercial property underwriters evaluate FEMA flood maps and structural elevation relative to Base Flood Elevation (BFE) for precise flood pricing.',
    endpoint: '/rest/v1/flood/rate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'floodZone', label: 'FEMA Flood Zone', type: 'select', defaultValue: 'Zone A', options: [{label: 'Zone A (High Risk Inland)', value: 'Zone A'}, {label: 'Zone V (High Risk Coastal)', value: 'Zone V'}, {label: 'Zone X (Low/Moderate Risk)', value: 'Zone X'}], description: 'FEMA flood hazard designation' },
      { name: 'lowestFloorElevationFt', label: 'Lowest Floor Elevation (ft)', type: 'number', defaultValue: 14.0, description: 'Structure lowest floor height above sea level' },
      { name: 'baseFloodElevationBFE', label: 'Base Flood Elevation BFE (ft)', type: 'number', defaultValue: 12.0, description: 'FEMA 100-year flood level height' },
      { name: 'hasFloodProofVents', label: 'Engineered Flood Vents Installed', type: 'boolean', defaultValue: true, description: 'Compliant engineered hydrostatic vents' }
    ]
  },
  {
    id: 'coinsurance-penalty',
    title: 'Property Coinsurance Clause Penalty Engine',
    category: 'Underwriting & Risk',
    shortDescription: 'Evaluates commercial building replacement valuation against 80%/90% coinsurance clauses to apply claim payout penalty reductions if under-insured.',
    businessPurpose: 'Commercial property clauses require buildings to be insured for at least 80-90% of replacement value; under-insured properties incur proportional claim penalties.',
    endpoint: '/rest/v1/coinsurance/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'buildingValue', label: 'Building Replacement Value ($)', type: 'number', defaultValue: 2000000, description: '100% full replacement valuation' },
      { name: 'buildingLimit', label: 'Carried Building Limit ($)', type: 'number', defaultValue: 1200000, description: 'Actual insured policy limit carried' },
      { name: 'coinsurancePct', label: 'Coinsurance Clause %', type: 'number', defaultValue: 0.80, description: 'Mandatory 80% or 90% clause requirement' },
      { name: 'claimLoss', label: 'Claim Property Loss Amount ($)', type: 'number', defaultValue: 500000, description: 'Incurred building property loss' },
      { name: 'deductible', label: 'Property Deductible ($)', type: 'number', defaultValue: 5000, description: 'Policy deductible amount' }
    ]
  },
  {
    id: 'deductible-buyback',
    title: 'Policy Deductible Buyback & Surcharge Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Calculates actuarial buyback surcharge factors when policyholders reduce high deductibles (e.g. buying down $10,000 to $1,000).',
    businessPurpose: 'Allows policyholders to buy down deductible exposure in exchange for a calculated premium surcharge.',
    endpoint: '/rest/v1/deductible/buyback',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'originalDeductible', label: 'Original Deductible ($)', type: 'number', defaultValue: 10000, description: 'Standard baseline deductible' },
      { name: 'targetDeductible', label: 'Target Buyback Deductible ($)', type: 'number', defaultValue: 1000, description: 'Requested reduced deductible' }
    ]
  },
  {
    id: 'uw-escalation',
    title: 'Multi-Tier UW Authority Escalation & Sign-Off Workflow Engine',
    category: 'Underwriting & Risk',
    shortDescription: 'Manages approval hierarchy (Underwriter -> Manager -> VP) enforcing dual sign-offs for TIV > $10M or fraud scores >= 70.',
    businessPurpose: 'Ensures high exposure commercial accounts receive proper governance and senior leadership sign-off prior to policy binding.',
    endpoint: '/rest/v1/uw/escalation',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'totalInsuredValue', label: 'Total Insured Value TIV ($)', type: 'number', defaultValue: 15000000, description: 'Aggregate property & liability value' },
      { name: 'riskScore', label: 'Underwriting Risk Score (0-100)', type: 'number', defaultValue: 75, description: 'Automated hazard risk assessment score' }
    ]
  },
  {
    id: 'sliding-dividend',
    title: 'Loss Sensitive Sliding Scale Policyholder Dividend Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Evaluates commercial retrospective rating plans returning up to 15% dividend returns for low loss ratios (<30%).',
    businessPurpose: 'Rewards commercial accounts that maintain clean safety records with cash dividend refunds after policy expiration.',
    endpoint: '/rest/v1/dividend/calculate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'incurredLosses', label: 'Annual Incurred Loss Amount ($)', type: 'number', defaultValue: 2500, description: 'Total paid + reserved policy claims' }
    ]
  },
  {
    id: 'rate-cap',
    title: 'Renewal Rate Impact Capping & Transition Smoothing Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Enforces maximum annual rate increase caps (e.g. max +10%) on renewal policies to smooth price hikes and prevent customer churn.',
    businessPurpose: 'Carriers cap steep rate increases to maintain high renewal retention while subsidizing transition rates over multiple terms.',
    endpoint: '/rest/v1/rate-cap/apply',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'uncappedProposedPremium', label: 'Uncapped Proposed Premium ($)', type: 'number', defaultValue: 15000, description: 'Raw technical benchmark renewal rate' },
      { name: 'maxRateCapPct', label: 'Max Renewal Cap % (e.g. 0.10)', type: 'number', defaultValue: 0.10, description: 'Maximum allowed annual rate increase cap' }
    ]
  },
  {
    id: 'siu-fraud',
    title: 'SIU Fraud Risk Scoring Engine',
    category: 'Underwriting & Risk',
    shortDescription: 'Analyzes identity anomalies, policy change velocity, and loss history to generate weighted fraud scores and trigger SIU holds.',
    businessPurpose: 'Detects suspicious policy applications and fraudulent claim patterns early to protect carrier loss ratios.',
    endpoint: '/rest/v1/fraud/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' }
    ]
  },
  {
    id: 'reinsurance-ledger',
    title: 'Reinsurance Treaty Layering & Cession Ledger Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Applies Quota Share, Excess of Loss (XOL), and Catastrophe Treaty layers to partition policy premiums and loss cessions.',
    businessPurpose: 'Automates complex reinsurance accounting, treaty attachment points, layer limits, and reinsurer bordereau reporting.',
    endpoint: '/rest/v1/reinsurance/simulate-loss',
    method: 'POST',
    inputs: [
      { name: 'claimLossAmount', label: 'Claim Loss Amount ($)', type: 'number', defaultValue: 2500000, description: 'Incurred claim loss amount to simulate reinsurance recovery' }
    ]
  },
  {
    id: 'cat-accumulation',
    title: 'Real-Time Catastrophe (CAT) Accumulation Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Aggregates geospatial Total Insured Value (TIV) across coastal hurricane and earthquake fault zones against carrier exposure caps.',
    businessPurpose: 'Prevents over-concentration of risk in hurricane or wildfire zones by tracking live CAT accumulation totals.',
    endpoint: '/rest/v1/cat/evaluate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'postalCode', label: 'Postal Code', type: 'string', defaultValue: '90210', description: 'Geospatial location ZIP' },
      { name: 'perilZone', label: 'CAT Peril Zone', type: 'string', defaultValue: 'Wildfire_High', description: 'Peril zone category' },
      { name: 'buildingLimit', label: 'Building Limit TIV ($)', type: 'number', defaultValue: 3500000, description: 'Property schedule total value' }
    ]
  },
  {
    id: 'commercial-audit',
    title: 'Commercial Premium Audit & Final Adjustment Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Compares estimated vs actual gross sales/payroll to calculate final audit additional or return premiums.',
    businessPurpose: 'Audits commercial policies post-expiration to adjust premiums based on true operational exposure figures.',
    endpoint: '/rest/v1/audit/process',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'actualExposure', label: 'Actual Audited Payroll ($)', type: 'number', defaultValue: 1200000, description: 'Audited financial exposure' },
      { name: 'estimatedExposure', label: 'Estimated Initial Payroll ($)', type: 'number', defaultValue: 1000000, description: 'Initial binding estimate' },
      { name: 'isNonCompliant', label: 'Non-Compliant Audit', type: 'boolean', defaultValue: false, description: 'Non-cooperative audit surcharge' }
    ]
  },
  {
    id: 'experience-mod',
    title: 'Experience Rating Mod (e-Mod) & NCCI Calculation Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Computes Workers Compensation Experience Modification Factor (e-Mod) using NCCI actual vs expected loss formulas.',
    businessPurpose: 'Adjusts Workers Comp premiums based on individual employer claim history relative to industry benchmarks.',
    endpoint: '/rest/v1/emod/calculate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'actualLosses', label: 'Actual Incurred Losses ($)', type: 'number', defaultValue: 17000, description: '3-year policy claim losses' },
      { name: 'expectedLosses', label: 'Expected Benchmark Losses ($)', type: 'number', defaultValue: 20000, description: 'Expected losses for class codes' }
    ]
  },
  {
    id: 'proration-refund',
    title: 'Policy Cancellation Short-Rate vs Pro-Rata Refund Calculator',
    category: 'Compliance & Regulatory',
    shortDescription: 'Calculates unearned premium return refunds comparing standard Pro-Rata factor vs Short-Rate 90% penalty table.',
    businessPurpose: 'Enforces state insurance department cancellation refund rules when policies are cancelled early by carrier or insured.',
    endpoint: '/rest/v1/proration/calculate',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'daysInForce', label: 'Days in Force', type: 'number', defaultValue: 180, description: 'Days policy was active before cancellation' },
      { name: 'totalTermDays', label: 'Total Term Days', type: 'number', defaultValue: 365, description: 'Full policy term length' },
      { name: 'isInsuredInitiated', label: 'Insured Initiated (Short-Rate)', type: 'boolean', defaultValue: true, description: 'Short-rate penalty applies if insured cancels' }
    ]
  },
  {
    id: 'multinational-ledger',
    title: 'Multi-Currency Multinational Local Policy Ledger Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Manages global master umbrella policy allocations across local foreign currencies (EUR, GBP, JPY) with real-time FX rates.',
    businessPurpose: 'Allows global commercial clients to manage multi-national admitted policies with local currency tax compliance.',
    endpoint: '/rest/v1/multinational/ledger',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' }
    ]
  },
  {
    id: 'commission-split',
    title: 'Multi-Payee Commission Split Engine',
    category: 'Compliance & Regulatory',
    shortDescription: 'Splits gross agency commission across wholesale brokers, managing general agents (MGA), and producing agents.',
    businessPurpose: 'Automates agency accounting and multi-producer commission splits on commercial transactions.',
    endpoint: '/rest/v1/commission/split',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'commissionRate', label: 'Agency Commission Rate (e.g. 0.15)', type: 'number', defaultValue: 0.15, description: 'Gross commission percentage' }
    ]
  },
  {
    id: 'oos-merge',
    title: 'Out-of-Sequence (OOS) Endorsement Transaction Merge Engine',
    category: 'Underwriting & Risk',
    shortDescription: 'Merges effective date endorsement conflicts when backdated policy changes overlap on the policy timeline slice.',
    businessPurpose: 'Guidewire core capability for resolving retroactive policy endorsements without corrupting policy state.',
    endpoint: '/rest/v1/policy/oos-merge',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy #', type: 'string', defaultValue: 'POL-COMM-1001', description: 'Bound policy number' },
      { name: 'backdatedDate', label: 'Backdated Effective Date', type: 'string', defaultValue: '2026-03-01', description: 'Retroactive endorsement effective date' },
      { name: 'newBiLimit', label: 'New BI Limit', type: 'string', defaultValue: '$2,000,000 / $2,000,000', description: 'Updated Bodily Injury Limit' },
      { name: 'newCollisionDeductible', label: 'New Collision Ded', type: 'string', defaultValue: '$500', description: 'Updated Deductible' }
    ]
  },
  {
    id: 'renewal-eligibility',
    title: 'Pre-Renewal Portfolio Health Batch Process Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Automated batch process scanning portfolio policies 90 days prior to expiration to score renewal profitability.',
    businessPurpose: 'Pre-screens renewal portfolio to automatically non-renew un-profitable accounts or apply rate increases.',
    endpoint: '/rest/v1/renewal/eligibility',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'proposedRateIncreasePct', label: 'Proposed Renewal Rate Increase (e.g. 0.18)', type: 'number', defaultValue: 0.18, description: 'Target rate increase factor' }
    ]
  },
  {
    id: 'group-coi',
    title: 'Automated Group Account Certificate of Insurance (COI) Issuance',
    category: 'Compliance & Regulatory',
    shortDescription: 'Batch generates ACORD 25 COI documents across multi-location commercial policyholder schedules.',
    businessPurpose: 'Automates mass certificate generation for commercial accounts with hundreds of certificate holders.',
    endpoint: '/rest/v1/coi/issue',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'holderName', label: 'Certificate Holder Name', type: 'string', defaultValue: 'General Contractor Inc', description: 'Insured entity requesting certificate' },
      { name: 'holderAddress', label: 'Certificate Holder Address', type: 'string', defaultValue: '100 Construction Way, San Francisco, CA', description: 'Mailing address' },
      { name: 'isAdditionalInsured', label: 'Additional Insured Endorsement', type: 'boolean', defaultValue: true, description: 'Attaches AI coverage endorsement' },
      { name: 'isWaiverOfSubrogation', label: 'Waiver of Subrogation', type: 'boolean', defaultValue: true, description: 'Attaches statutory subrogation waiver' }
    ]
  },
  {
    id: 'forms-inference',
    title: 'Policy Form Inference & Attachment Rules Engine',
    category: 'Compliance & Regulatory',
    shortDescription: 'Evaluates policy coverages, state jurisdictions, and limits to dynamically attach statutory policy forms.',
    businessPurpose: 'Ensures legal policy documents contain all required state mandatory endorsements.',
    endpoint: '/rest/v1/forms/infer',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' }
    ]
  },
  {
    id: 'ig-vehicle-details',
    title: 'Guidewire Cloud Integration Gateway (IG) — Vehicle & MVR Vendor Gateway',
    category: 'Specialty Lines',
    shortDescription: 'Integration Gateway (IG) microservice layer executing outbound real-time DMV MVR driver lookup, VIN spec verification, safety scores, and auto underwriting tier recommendations.',
    businessPurpose: 'Connects PolicyCenter Personal Auto and Commercial Auto submissions to external MVR/DMV data vendors via the Integration Gateway microservice JAR.',
    endpoint: '/rest/v1/ig/vehicle-details',
    method: 'POST',
    inputs: [
      { name: 'vin', label: 'Vehicle VIN', type: 'string', defaultValue: '1FA6P8CF0R5100001', description: '17-character VIN identifier' },
      { name: 'vehicleYear', label: 'Vehicle Model Year', type: 'number', defaultValue: 2025, description: 'Model manufacture year' },
      { name: 'vehicleMake', label: 'Vehicle Make', type: 'string', defaultValue: 'Ford', description: 'Manufacturer make' },
      { name: 'vehicleModel', label: 'Vehicle Model', type: 'string', defaultValue: 'Mustang GT', description: 'Model designation' },
      { name: 'driverLicenseNumber', label: 'Driver License Number', type: 'string', defaultValue: 'DL-CA-9948123', description: 'Driver license (Use DL-DUI-9948000 for high-risk test)' },
      { name: 'driverState', label: 'Driver State Jurisdiction', type: 'string', defaultValue: 'CA', description: 'State licensing authority' }
    ]
  },
  {
    id: 'facultative-reinsurance',
    title: 'Facultative Reinsurance & Excess of Loss Allocation Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Allocates Quota Share, Surplus Treaties, and Excess of Loss retention stacks with dynamic facultative placement certificate generation.',
    businessPurpose: 'Protects carrier balance sheet by ceding excess commercial risk to treaty and facultative reinsurers.',
    endpoint: '/rest/v1/reinsurance/facultative',
    method: 'POST',
    inputs: [
      { name: 'jobNumber', label: 'Submission Job #', type: 'string', defaultValue: 'S0001001', description: 'Policy Period Job Number' },
      { name: 'tiv', label: 'Total Insured Value ($)', type: 'number', defaultValue: 10000000, description: 'Total account building and contents TIV' }
    ]
  },
  {
    id: 'parametric-catastrophe',
    title: 'Parametric Climate & Catastrophe Real-Time Trigger Engine',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Monitors real-time wind speed and seismic telemetry to trigger instant policy claim settlements.',
    businessPurpose: 'Provides instant liquidity to policyholders following natural disaster triggers without manual loss adjustment delays.',
    endpoint: '/rest/v1/parametric/eval',
    method: 'POST',
    inputs: [
      { name: 'postalCode', label: 'Postal Code', type: 'string', defaultValue: '33101', description: 'Insured location postal code' },
      { name: 'recordedWindSpeed', label: 'Recorded Windspeed (Knots)', type: 'number', defaultValue: 125, description: 'Sustained wind speed reading' }
    ]
  },
  {
    id: 'property-prefill-hazard-intel',
    title: 'Property Pre-Fill & Hazard Intelligence Accelerator (ISO 1-6 / FEMA / Wildfire)',
    category: 'Underwriting & Risk',
    shortDescription: 'Auto-retrieves building characteristics (ISO Construction Types 1-6, Year Built, Roof Geometry, Distance to Hydrant/Station, Public Protection Class, and Wildfire/Flood Risk Scores) from external property databases.',
    businessPurpose: 'Eliminates manual property data entry, accelerates commercial property underwriting, and populates building schedules automatically.',
    endpoint: '/rest/v1/accelerator/property-prefill',
    method: 'POST',
    inputs: [
      { name: 'address', label: 'Property Street Address', type: 'string', defaultValue: '100 Ocean Drive, Miami, FL', description: 'Physical commercial location address' },
      { name: 'zipCode', label: 'Postal Zip Code', type: 'string', defaultValue: '33139', description: '5-digit postal zip code' }
    ]
  },
  {
    id: 'prior-loss-clue',
    title: 'Prior Loss & C.L.U.E. Auto-Retrieval Accelerator (Loss Modifier Engine)',
    category: 'Underwriting & Risk',
    shortDescription: 'Retrieves 3-5 year commercial claims and loss histories to calculate loss frequency, severity, loss ratios, and auto-apply experience modifier credits (-15%) or debit surcharges (+30%).',
    businessPurpose: 'Automates prior loss underwriting verification and triggers referral issues for adverse claims frequency or severe losses.',
    endpoint: '/rest/v1/accelerator/prior-loss',
    method: 'POST',
    inputs: [
      { name: 'searchKey', label: 'Business FEIN / Tax ID / Name', type: 'string', defaultValue: 'TAX-94-1829104-CLEAN', description: 'Account tax ID or business search identifier' },
      { name: 'annualEarnedPremium', label: 'Annual Baseline Premium ($)', type: 'number', defaultValue: 45000.00, description: 'Estimated annual policy premium' }
    ]
  },
  {
    id: 'reinsurance-placement-slip',
    title: 'ACORD Reinsurance Placement Slip & Syndicate Bordereau Generator',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Generates standardized ACORD Reinsurance Placement Slips, Quota Share cessions, and syndicate bordereau breakdowns for high-limit commercial risks.',
    businessPurpose: 'Automates reinsurance documentation, ceding commission calculations (25%), and distribution among syndicates (Swiss Re, Munich Re, Hannover Re).',
    endpoint: '/rest/v1/reinsurance/slip',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Commercial Policy Number', type: 'string', defaultValue: 'POL-COMM-PROP-8891', description: 'Target policy number' },
      { name: 'treatyType', label: 'Treaty / Placement Type', type: 'string', defaultValue: 'QUOTA_SHARE', description: 'QUOTA_SHARE or EXCESS_OF_LOSS' },
      { name: 'policyLimit', label: 'Gross Policy Limit ($)', type: 'number', defaultValue: 20000000.00, description: 'Total policy property limit' },
      { name: 'grossPremium', label: 'Gross Written Premium ($)', type: 'number', defaultValue: 65000.00, description: 'Annual policy premium' },
      { name: 'quotaSharePct', label: 'Quota Share Ceded %', type: 'number', defaultValue: 40.0, description: 'Percentage ceded to treaty reinsurers' }
    ]
  },
  {
    id: 'cp-endorsements-blanket',
    title: 'Commercial Property Endorsements & Multi-Location Blanket Suite',
    category: 'Specialty Lines',
    shortDescription: 'Calculates detailed premiums for Tenants Improvements & Betterments, Business Income (1/3, 1/4, 1/6 indemnity periods), Equipment Breakdown, Earthquake/Flood, and multi-location Blanket coverage pools.',
    businessPurpose: 'Provides end-to-end rating for commercial property coverage endorsements and blanket rate averaging.',
    endpoint: '/rest/v1/cp/endorsements/rate',
    method: 'POST',
    inputs: [
      { name: 'buildingLimit', label: 'Building Limit ($)', type: 'number', defaultValue: 1500000.00, description: 'Primary building structure limit' },
      { name: 'bppLimit', label: 'Business Personal Property ($)', type: 'number', defaultValue: 350000.00, description: 'Contents and inventory limit' },
      { name: 'tenantsImprovementLimit', label: 'Tenants Improvements Limit ($)', type: 'number', defaultValue: 150000.00, description: 'Betterments and leasehold improvements' },
      { name: 'businessIncomeLimit', label: 'Business Income Limit ($)', type: 'number', defaultValue: 300000.00, description: 'Lost business profits & extra expense' },
      { name: 'equipmentBreakdownLimit', label: 'Equipment Breakdown Limit ($)', type: 'number', defaultValue: 500000.00, description: 'Boiler, mechanical & electrical equipment' },
      { name: 'protectionClass', label: 'ISO Protection Class (1-10)', type: 'string', defaultValue: '3', description: 'Public protection class rating' }
    ]
  },
  {
    id: 'commercial-audit-workflow',
    title: 'Commercial Multi-Class Payroll Audit & Dispute Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Executes physical and voluntary payroll audits across multiple classification codes with state assessments, exposure variance calculations, and audit dispute handling.',
    businessPurpose: 'Reconciles policyholder estimated payrolls vs audited actuals for Workers Comp and General Liability, generating return or additional invoices.',
    endpoint: '/rest/v1/audit/multiclass/execute',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-WC-AUDIT-901', description: 'Target audit policy number' },
      { name: 'stateAssessmentPct', label: 'State Assessment Surcharge %', type: 'number', defaultValue: 3.5, description: 'State statutory guaranty assessment' }
    ]
  },
  {
    id: 'quote-compare-packages',
    title: 'Multi-Option Quote Comparison & Dynamic Tier Matrix',
    category: 'Underwriting & Risk',
    shortDescription: 'Generates real-time side-by-side quote packages (Bronze Essential, Silver Preferred, Gold Enterprise) with customizable deductible levels and coverage bundles.',
    businessPurpose: 'Enables brokers and underwriters to present multi-tier options to insureds for fast comparison and 1-click binding.',
    endpoint: '/rest/v1/quote/compare-packages',
    method: 'POST',
    inputs: [
      { name: 'buildingLimit', label: 'Building Coverage Limit ($)', type: 'number', defaultValue: 1200000.00, description: 'Commercial building value' },
      { name: 'bppLimit', label: 'BPP / Contents Limit ($)', type: 'number', defaultValue: 300000.00, description: 'Personal property value' },
      { name: 'protectionClass', label: 'ISO Protection Class (1-10)', type: 'string', defaultValue: '3', description: 'Local fire protection class' }
    ]
  },
  {
    id: 'wc-retro-rating',
    title: 'Workers\' Compensation NCCI Retrospective Rating Plan',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Calculates retrospective premiums based on incurred losses, Loss Conversion Factor (LCF 1.15), Basic Premium factors, and enforces contractual Minimum / Maximum premium boundary caps.',
    businessPurpose: 'Enables large commercial employers to participate in loss-sensitive Workers Comp plans, aligning premiums with actual workplace safety performance.',
    endpoint: '/rest/v1/wc/retro/calculate',
    method: 'POST',
    inputs: [
      { name: 'standardPremium', label: 'Standard Manual Premium ($)', type: 'number', defaultValue: 120000.00, description: 'Annual standard earned premium' },
      { name: 'incurredLosses', label: 'Audited Incurred Losses ($)', type: 'number', defaultValue: 38000.00, description: 'Total paid and reserved claims' },
      { name: 'basicPremiumFactor', label: 'Basic Premium Factor', type: 'number', defaultValue: 0.220, description: 'Insurer acquisition and overhead factor' },
      { name: 'lossConversionFactor', label: 'Loss Conversion Factor (LCF)', type: 'number', defaultValue: 1.150, description: 'Claims handling adjustment factor' }
    ]
  },
  {
    id: 'gl-composite-rating',
    title: 'General Liability Multi-Variable Composite Rating Suite',
    category: 'Specialty Lines',
    shortDescription: 'Composite ratings across Gross Sales ($ per $1k), Square Footage ($ per 1k sq ft), and Payroll ($ per $100) with OCP endorsements and tiered Liquor Liability surcharges.',
    businessPurpose: 'Streamlines large commercial general liability pricing by combining multiple rating bases into a single composite rate.',
    endpoint: '/rest/v1/gl/composite/rate',
    method: 'POST',
    inputs: [
      { name: 'grossSales', label: 'Gross Annual Sales ($)', type: 'number', defaultValue: 3500000.00, description: 'Total company gross receipts' },
      { name: 'squareFootage', label: 'Premises Square Footage', type: 'number', defaultValue: 50000.00, description: 'Total maintained commercial area' },
      { name: 'payroll', label: 'Operating Payroll ($)', type: 'number', defaultValue: 750000.00, description: 'Direct employee payroll' },
      { name: 'ocpLimit', label: 'OCP Liability Limit ($)', type: 'number', defaultValue: 1000000.00, description: 'Owners & Contractors Protective limit' }
    ]
  },
  {
    id: 'im-contractors-equipment',
    title: 'Inland Marine Contractors\' Equipment Schedule & Floater',
    category: 'Specialty Lines',
    shortDescription: 'Rates scheduled mobile machinery, rented/borrowed equipment floaters, Boom & Overload crane collapse perils (+20%), and deductible buy-down structures.',
    businessPurpose: 'Provides complete Inland Marine physical damage coverage for high-value construction, excavation, and contractor machinery.',
    endpoint: '/rest/v1/im/contractors-equipment/rate',
    method: 'POST',
    inputs: [
      { name: 'scheduledLimit', label: 'Scheduled Equipment Limit ($)', type: 'number', defaultValue: 850000.00, description: 'Stated value of owned heavy machinery' },
      { name: 'rentedLimit', label: 'Rented / Leased Gear Limit ($)', type: 'number', defaultValue: 200000.00, description: 'Short-term hired equipment floater' },
      { name: 'valuationBasis', label: 'Valuation Basis', type: 'string', defaultValue: 'REPLACEMENT_COST', description: 'REPLACEMENT_COST or AGREED_VALUE' },
      { name: 'boomOverload', label: 'Boom Overload Endorsement', type: 'boolean', defaultValue: true, description: 'Hydraulic boom and crane upset peril' }
    ]
  },
  {
    id: 'auto-fleet-radius-hazmat',
    title: 'Commercial Auto Fleet Operating Radius & Hazmat Engine',
    category: 'Specialty Lines',
    shortDescription: 'Evaluates fleet operating radius (Local, Intermediate, Long Distance) and applies DOT Hazardous Materials surcharges (up to +85%) with CA 99 48 pollution endorsements.',
    businessPurpose: 'Prices commercial truck and logistics fleets based on delivery radius and hazardous cargo transportation classifications.',
    endpoint: '/rest/v1/auto/fleet-radius/rate',
    method: 'POST',
    inputs: [
      { name: 'vehicleCount', label: 'Vehicle Fleet Size', type: 'number', defaultValue: 15, description: 'Number of power units in fleet' },
      { name: 'operatingRadiusClass', label: 'Operating Radius', type: 'string', defaultValue: 'INTERMEDIATE', description: 'LOCAL (<50mi), INTERMEDIATE (50-200mi), LONG_DISTANCE (>200mi)' },
      { name: 'dotHazmatClass', label: 'DOT Hazmat Class', type: 'string', defaultValue: 'CLASS_3_FLAMMABLE', description: 'DOT hazardous cargo classification' }
    ]
  },
  {
    id: 'accelerator-sos-verify',
    title: 'Accelerator #10: Secretary of State & D&B Commercial Verifier',
    category: 'Compliance & Regulatory',
    shortDescription: 'Validates corporate state filing standing (Active/Suspended/Dissolved), officer records, and D&B Paydex Credit & Financial Stress scores to detect fronting fraud.',
    businessPurpose: 'Automates Secretary of State corporate verification during intake to block shell companies and delinquent entities prior to underwriting.',
    endpoint: '/rest/v1/accelerator/sos-verify',
    method: 'POST',
    inputs: [
      { name: 'businessName', label: 'Legal Entity Business Name', type: 'string', defaultValue: 'Apex Global Industrial Corp', description: 'Registered commercial legal name' },
      { name: 'fein', label: 'Federal EIN (FEIN)', type: 'string', defaultValue: '94-8192014', description: 'Federal Tax Identification Number' },
      { name: 'state', label: 'State of Formation', type: 'string', defaultValue: 'DE', description: 'Jurisdiction of incorporation' }
    ]
  },
  {
    id: 'accelerator-ofac-screen',
    title: 'Accelerator #11: OFAC / PEP Sanctions & AML Compliance Screener',
    category: 'Compliance & Regulatory',
    shortDescription: 'High-performance fuzzy matching against US Treasury OFAC SDN and Politically Exposed Persons (PEP) watchlist with automated Underwriting Binding Lock enforcement.',
    businessPurpose: 'Ensures strict compliance with federal OFAC sanctions and AML regulations, instantly freezing binding workflows upon positive match.',
    endpoint: '/rest/v1/accelerator/ofac-screen',
    method: 'POST',
    inputs: [
      { name: 'screenedSubject', label: 'Screened Subject Name', type: 'string', defaultValue: 'Apex Commercial Logistics', description: 'Insured name, DBA, or officer' },
      { name: 'country', label: 'Jurisdiction / Country', type: 'string', defaultValue: 'USA', description: 'Country of domicile' }
    ]
  },
  {
    id: 'accelerator-binder-explainer',
    title: 'Accelerator #12: AI Policy Binder Document Explainer & Summary',
    category: 'Underwriting & Risk',
    shortDescription: 'Synthesizes complex multi-line commercial policy binders into a 1-page executive briefing covering coverages, warranties, deductibles, exclusions, and payment installment plans.',
    businessPurpose: 'Transforms lengthy insurance contracts into transparent, broker-ready executive summaries for fast client presentation.',
    endpoint: '/rest/v1/accelerator/binder-explainer',
    method: 'POST',
    inputs: [
      { name: 'productCode', label: 'Policy Product Line', type: 'string', defaultValue: 'CommercialProperty', description: 'Line of business' },
      { name: 'totalPremium', label: 'Total Policy Premium ($)', type: 'number', defaultValue: 18500.00, description: 'Annual policy premium' }
    ]
  },
  {
    id: 'policy-split-rewrite',
    title: 'Guidewire Policy Split & Subsidiary Spin-Off Workflow',
    category: 'Underwriting & Risk',
    shortDescription: 'Executes commercial policy split transactions, spinning off subsidiary locations and vehicle schedules into new policies while preserving claims history continuity.',
    businessPurpose: 'Facilitates corporate restructuring, mergers, and divestitures by splitting complex policies into separate legal entity contracts.',
    endpoint: '/rest/v1/policy/split-rewrite',
    method: 'POST',
    inputs: [
      { name: 'parentPolicyNumber', label: 'Parent Policy Number', type: 'string', defaultValue: 'POL-PARENT-1001', description: 'Source master policy' },
      { name: 'newNamedInsured', label: 'New Spin-Off Named Insured', type: 'string', defaultValue: 'Apex West Coast Logistics LLC', description: 'Target subsidiary corporate name' },
      { name: 'transferRatio', label: 'Asset Transfer Ratio (0.0 - 1.0)', type: 'number', defaultValue: 0.35, description: 'Proportion of premium/assets spun off' }
    ]
  },
  {
    id: 'loss-control-inspection',
    title: 'Loss Control Survey & Safety Recommendation Engine',
    category: 'Underwriting & Risk',
    shortDescription: 'Evaluates site risk surveys, tracks mandatory 30/60-day engineering recommendations, and triggers Direct Notice of Cancellation (DNOC) workflows upon non-compliance.',
    businessPurpose: 'Protects commercial property and casualty portfolios by mandating physical risk improvements and enforcing cancellation policies.',
    endpoint: '/rest/v1/loss-control/recommendations',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-COMM-8801', description: 'Inspected policy number' },
      { name: 'hasCriticalElectricalFlaw', label: 'Critical Electrical Flaw (FPE Panel)', type: 'boolean', defaultValue: false, description: 'Overdue electrical hazard' },
      { name: 'hasCookingHazards', label: 'Commercial Kitchen Hood Hazard', type: 'boolean', defaultValue: true, description: 'Kitchen fire suppression required' }
    ]
  },
  {
    id: 'parametric-event-cancellation',
    title: 'Parametric Weather & Event Cancellation Endorsement',
    category: 'Specialty Lines',
    shortDescription: 'Quotes parametric event cancellation endorsements and evaluates live telemetry data (NOAA rainfall > 1.25", wind > 45 mph) for instant automatic indemnity claims settlement.',
    businessPurpose: 'Delivers frictionless weather risk transfer for concerts, festivals, and sporting events with zero loss adjustment delays.',
    endpoint: '/rest/v1/parametric/event-cancellation',
    method: 'POST',
    inputs: [
      { name: 'eventName', label: 'Event Name', type: 'string', defaultValue: 'Austin City Outdoor Music Festival', description: 'Insured special event' },
      { name: 'eventGrossRevenueLimit', label: 'Stated Value Revenue Limit ($)', type: 'number', defaultValue: 500000.00, description: 'Gross ticket and vendor revenue' },
      { name: 'observedTelemetryReading', label: 'Observed Sensor Reading', type: 'number', defaultValue: 1.65, description: 'Actual recorded weather telemetry' }
    ]
  },
  {
    id: 'billing-agency-account-current',
    title: 'Agency Bill & Monthly Account Current Settlement',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Generates broker agency monthly Account Current statements, calculating gross written premium, agency commission retention (e.g. 15%), and net carrier ACH remittances.',
    businessPurpose: 'Automates agency billing reconciliation between insurance carriers and independent broker networks.',
    endpoint: '/rest/v1/billing/agency-account-current',
    method: 'POST',
    inputs: [
      { name: 'producerCode', label: 'Producer Code', type: 'string', defaultValue: 'PR-WEST-901', description: 'Agency producer identifier' },
      { name: 'agencyName', label: 'Agency Legal Name', type: 'string', defaultValue: 'Pacific Coast Commercial Insurance Brokers Inc', description: 'Brokerage name' },
      { name: 'billingMonth', label: 'Billing Statement Month', type: 'string', defaultValue: '2026-08', description: 'Monthly statement cycle' }
    ]
  },
  {
    id: 'reinsurance-cat-reinstatement',
    title: 'Catastrophe Reinsurance Treaty Reinstatement Calculator',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Calculates pro-rata reinstatement premium due when a catastrophic claim impairs CAT XOL treaty layers, instantly restoring treaty limit capacity for remaining term.',
    businessPurpose: 'Manages capital and treaty accounting when major hurricanes or earthquakes breach catastrophe reinsurance layers.',
    endpoint: '/rest/v1/reinsurance/cat-reinstatement',
    method: 'POST',
    inputs: [
      { name: 'treatyLayerLimit', label: 'Treaty Layer Limit ($)', type: 'number', defaultValue: 50000000.00, description: 'Total treaty capacity' },
      { name: 'treatyAnnualCededPremium', label: 'Annual Ceded Treaty Premium ($)', type: 'number', defaultValue: 4000000.00, description: 'Annual treaty premium' },
      { name: 'catastrophicLossAmount', label: 'Catastrophic Incurred Loss ($)', type: 'number', defaultValue: 30000000.00, description: 'Loss amount allocated to layer' }
    ]
  },
  {
    id: 'cyber-accumulation-assess',
    title: 'Commercial Cyber Risk & Cloud Outage Accumulation Engine',
    category: 'Specialty Lines',
    shortDescription: 'Rates commercial cyber liability, scores MFA/EDR posture tiers, and models systemic cloud vendor outage accumulation loads (AWS, Azure, GCP).',
    businessPurpose: 'Accurately prices first-party ransomware extortion and cloud contingent business interruption while managing systemic cyber portfolio concentration.',
    endpoint: '/rest/v1/cyber/accumulation-assess',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-CYBER-8801', description: 'Cyber risk policy identifier' },
      { name: 'requestedLimit', label: 'Requested Aggregate Limit ($)', type: 'number', defaultValue: 2000000.00, description: 'Total cyber policy limit' },
      { name: 'mfaEnforced', label: 'MFA Enforced on Endpoints', type: 'boolean', defaultValue: true, description: 'Mandatory Multi-Factor Authentication' },
      { name: 'offlineBackupsDaily', label: 'Immutable Daily Backups', type: 'boolean', defaultValue: true, description: 'Air-gapped daily offline backups' },
      { name: 'edrDeployed', label: 'EDR Antivirus Deployed', type: 'boolean', defaultValue: true, description: 'Endpoint Detection & Response' },
      { name: 'phishingPassRatePct', label: 'Phishing Training Pass Rate %', type: 'number', defaultValue: 92.0, description: 'Employee simulation pass rate' },
      { name: 'primaryCloudProvider', label: 'Primary Cloud Provider', type: 'string', defaultValue: 'AWS', description: 'AWS, AZURE, GCP, MULTI_CLOUD, ON_PREM' },
      { name: 'annualRevenue', label: 'Annual Insured Revenue ($)', type: 'number', defaultValue: 15000000.00, description: 'Gross annual sales' }
    ]
  },
  {
    id: 'surety-bond-capacity',
    title: 'Surety Bonds & Commercial Contract Guarantee Engine',
    category: 'Specialty Lines',
    shortDescription: 'Underwrites Bid, Performance, Payment, and License bonds, computing single-job and aggregate bonding capacity from contractor balance sheets.',
    businessPurpose: 'Automates contract surety risk underwriting, capacity threshold validation, and obligee verification for commercial contractors.',
    endpoint: '/rest/v1/surety/bond-capacity-and-quote',
    method: 'POST',
    inputs: [
      { name: 'contractorName', label: 'Contractor Name', type: 'string', defaultValue: 'Apex General Contractors LLC', description: 'Principal bonded contractor' },
      { name: 'bondType', label: 'Bond Type', type: 'string', defaultValue: 'PERFORMANCE', description: 'BID, PERFORMANCE, PAYMENT, LICENSE_PERMIT' },
      { name: 'contractSum', label: 'Construction Contract Sum ($)', type: 'number', defaultValue: 1500000.00, description: 'Total contract value' },
      { name: 'workingCapital', label: 'Net Working Capital ($)', type: 'number', defaultValue: 450000.00, description: 'Current Assets - Current Liabilities' },
      { name: 'netWorth', label: 'Tangible Net Worth ($)', type: 'number', defaultValue: 900000.00, description: 'Total equity' },
      { name: 'activeWIP', label: 'Active Work In Progress Backlog ($)', type: 'number', defaultValue: 1100000.00, description: 'Uncompleted bonded contracts' },
      { name: 'obligeeName', label: 'Obligee (Project Owner)', type: 'string', defaultValue: 'State Dept of Transportation', description: 'Beneficiary of bond obligation' }
    ]
  },
  {
    id: 'ocean-marine-cargo',
    title: 'Commercial Cargo & Ocean Marine Transit Floater',
    category: 'Specialty Lines',
    shortDescription: 'Rates maritime cargo voyages under Institute Cargo Clauses (A/B/C) with CIF+10% valuation, vessel age surcharges, and port accumulation limits.',
    businessPurpose: 'Provides international open cargo underwriting and catastrophe accumulation control at major seaports.',
    endpoint: '/rest/v1/marine/rate-voyage-cargo',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-MAR-5001', description: 'Marine policy reference' },
      { name: 'invoiceValue', label: 'Invoice Cargo Value ($)', type: 'number', defaultValue: 750000.00, description: 'Commercial invoice CIF cost' },
      { name: 'commodityType', label: 'Commodity Type', type: 'string', defaultValue: 'GENERAL_DRY', description: 'GENERAL_DRY, PERISHABLES, ELECTRONICS, CRUDE_OIL' },
      { name: 'clauseType', label: 'Institute Clauses', type: 'string', defaultValue: 'CLAUSES_A_ALL_RISK', description: 'CLAUSES_A_ALL_RISK, CLAUSES_B_NAMED, CLAUSES_C_MAJOR_CASUALTY' },
      { name: 'vesselName', label: 'Vessel Name', type: 'string', defaultValue: 'MV Pacific Navigator', description: 'Commercial cargo ship' },
      { name: 'vesselAge', label: 'Vessel Age (Years)', type: 'number', defaultValue: 8, description: 'Ship age in years' },
      { name: 'warStrikesIncluded', label: 'Include War & Strikes Perils', type: 'boolean', defaultValue: true, description: 'Institute War & Strikes clauses' },
      { name: 'piracyCorridorIncluded', label: 'Transiting High-Risk Piracy Corridor', type: 'boolean', defaultValue: false, description: 'Red Sea / Gulf of Aden surcharge' }
    ]
  },
  {
    id: 'actuarial-rate-simulation',
    title: 'Actuarial Rate Change & Portfolio "What-If" Impact Simulator',
    category: 'Reinsurance & Portfolio',
    shortDescription: 'Simulates portfolio-wide financial impact of proposed line rate changes, modeling earned premium variance, renewal elasticity, and loss ratio shifts.',
    businessPurpose: 'Empowers actuaries and chief underwriting officers to stress-test rate revisions across the active commercial book before filing.',
    endpoint: '/rest/v1/actuarial/simulate-rate-impact',
    method: 'POST',
    inputs: [
      { name: 'propertyRateDeltaPct', label: 'Property Rate Change (%)', type: 'number', defaultValue: 8.0, description: 'Commercial Property % rate adjustment' },
      { name: 'generalLiabilityDeltaPct', label: 'GL Rate Change (%)', type: 'number', defaultValue: 4.5, description: 'General Liability % rate adjustment' },
      { name: 'commercialAutoDeltaPct', label: 'Auto Rate Change (%)', type: 'number', defaultValue: 12.0, description: 'Commercial Auto % rate adjustment' },
      { name: 'workersCompDeltaPct', label: 'Workers Comp Rate Change (%)', type: 'number', defaultValue: -2.0, description: 'Workers Comp % rate adjustment' },
      { name: 'cyberDeltaPct', label: 'Cyber Rate Change (%)', type: 'number', defaultValue: 15.0, description: 'Cyber line % rate adjustment' }
    ]
  },
  {
    id: 'acord-loss-run-triage',
    title: 'AI Autonomous Submission Intake & ACORD Loss Run Parser 2.0',
    category: 'Underwriting & Risk',
    shortDescription: 'Parses ACORD submissions and 5-year historical loss runs, computing Loss Development Triangles, loss severity velocity, and automated appetite routing.',
    businessPurpose: 'Accelerates intake triage by turning raw prior carrier loss runs into instant actuarial triangles and triage recommendations.',
    endpoint: '/rest/v1/intake/parse-acord-loss-runs',
    method: 'POST',
    inputs: [
      { name: 'insuredName', label: 'Named Insured', type: 'string', defaultValue: 'Horizon Logistics & Warehousing Inc', description: 'Commercial applicant' },
      { name: 'lineOfBusiness', label: 'Line of Business', type: 'string', defaultValue: 'COMMERCIAL_AUTO', description: 'Insurance line' },
      { name: 'estimatedAnnualPremium', label: 'Estimated Annual Premium ($)', type: 'number', defaultValue: 45000.00, description: 'Estimated policy term premium' }
    ]
  },
  {
    id: 'parametric-cat-oracle',
    title: 'Real-Time USGS / NOAA Parametric Catastrophe Oracle Platform',
    category: 'Specialty Lines',
    shortDescription: 'Evaluates live USGS Earthquake ShakeMaps (PGA / MMI) and NOAA Hurricane Barometric feeds for instant, zero-touch indemnity claim payouts.',
    businessPurpose: 'Provides automated parametric disaster protection with instant FedNow wire transfers and automated reinsurance recoveries upon sensor breach.',
    endpoint: '/rest/v1/parametric/oracle-evaluate',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-PARAMETRIC-7701', description: 'Parametric contract number' },
      { name: 'perilType', label: 'Peril Type', type: 'string', defaultValue: 'EARTHQUAKE_USGS', description: 'EARTHQUAKE_USGS or HURRICANE_NOAA' },
      { name: 'locationAddress', label: 'Location Address', type: 'string', defaultValue: 'San Francisco Waterfront Port, CA', description: 'Sensor monitored facility' },
      { name: 'contractLimit', label: 'Contract Indemnity Limit ($)', type: 'number', defaultValue: 2500000.00, description: 'Maximum parametric payout' },
      { name: 'measuredTelemetryValue', label: 'Measured Sensor Telemetry (PGA in g / Pressure mbar)', type: 'number', defaultValue: 0.72, description: 'Live recorded sensor reading' }
    ]
  },
  {
    id: 'effdated-slice-visualizer',
    title: 'Interactive EffDated Policy Slice Visualizer & Time-Travel Explorer',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Decomposes effective-dated policy periods into continuous non-overlapping date slices, mapping pro-rata earned premiums and out-of-sequence merges.',
    businessPurpose: 'Provides visual transparency into retroactive endorsement slice boundaries and multi-exposure timeline splits.',
    endpoint: '/rest/v1/policy/effdated-slices',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-COMM-9901', description: 'EffDated policy identifier' },
      { name: 'baseAnnualPremium', label: 'Base Annual Premium ($)', type: 'number', defaultValue: 24000.00, description: 'Inception annual rate' },
      { name: 'includeOosEndorsement', label: 'Include Out-of-Sequence Endorsement', type: 'boolean', defaultValue: true, description: 'Simulates retroactive slice merge' }
    ]
  },
  {
    id: 'uw-collaborative-dealroom',
    title: 'Underwriter Collaborative Deal Room & Referral Authority Workflow',
    category: 'Underwriting & Risk',
    shortDescription: 'Manages multi-tier underwriting authority escalation (Line UW -> Senior UW -> Regional Director -> CUO) with subjective binder conditions and audit trails.',
    businessPurpose: 'Enables high-capacity commercial deal governance and compliance-stamped collaborative peer reviews.',
    endpoint: '/rest/v1/dealroom/escalate',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-DEAL-1001', description: 'High-hazard submission job #' },
      { name: 'insuredName', label: 'Named Insured', type: 'string', defaultValue: 'Titan Heavy Manufacturing Corp', description: 'Commercial enterprise account' },
      { name: 'totalPremium', label: 'Total Premium ($)', type: 'number', defaultValue: 350000.00, description: 'Account total written premium' },
      { name: 'aggregateLimit', label: 'Aggregate Policy Limit ($)', type: 'number', defaultValue: 10000000.00, description: 'Requested aggregate capacity' },
      { name: 'initialNote', label: 'Underwriter Case Note', type: 'string', defaultValue: 'Escalated for senior review due to high manufacturing capacity and $10M aggregate limits.', description: 'Referral rationale' }
    ]
  },
  {
    id: 'premium-financing-noa',
    title: 'Commercial Premium Financing (PFC) & Notice of Assignment Engine',
    category: 'Commercial Rating & Retrospective',
    shortDescription: 'Generates third-party Premium Finance agreements (AFCO/IPFS), calculating down payments, APR monthly installment schedules, and legal NOA payloads.',
    businessPurpose: 'Automates premium finance onboarding, electronic Notice of Assignment generation, and Power of Attorney default cancellation reconciliation.',
    endpoint: '/rest/v1/billing/premium-finance/quote-and-noa',
    method: 'POST',
    inputs: [
      { name: 'policyNumber', label: 'Policy Number', type: 'string', defaultValue: 'POL-COMM-8001', description: 'Financed commercial policy #' },
      { name: 'policyPremium', label: 'Gross Financed Premium ($)', type: 'number', defaultValue: 50000.00, description: 'Total policy premium' },
      { name: 'financeCompanyName', label: 'Premium Finance Company', type: 'string', defaultValue: 'AFCO Premium Credit Corp', description: 'PFC legal name' },
      { name: 'downPaymentPercent', label: 'Down Payment (%)', type: 'number', defaultValue: 20.0, description: 'Initial deposit requirement' },
      { name: 'aprPct', label: 'Finance APR (%)', type: 'number', defaultValue: 8.75, description: 'Annual percentage interest rate' },
      { name: 'installmentCount', label: 'Installment Count', type: 'number', defaultValue: 9, description: 'Number of monthly payments (8, 9, 10)' }
    ]
  },
  {
    id: 'gosu-repl-sandbox',
    title: 'Interactive Gosu Rules & Rating Sandbox (Web REPL & Loom Benchmark)',
    category: 'Underwriting & Risk',
    shortDescription: 'Executes dynamic Gosu rating expressions and rules against in-memory policy periods, benchmarking Java 23 Virtual Threads against platform thread concurrency.',
    businessPurpose: 'Accelerates developer rule testing and validates high-throughput virtual thread execution speedups for Guidewire rating routines.',
    endpoint: '/rest/v1/gosu/repl-evaluate',
    method: 'POST',
    inputs: [
      { name: 'sampleLimit', label: 'Sample Policy Limit ($)', type: 'number', defaultValue: 1000000.00, description: 'Rating exposure basis' },
      { name: 'benchmarkIterations', label: 'Benchmark Iteration Count', type: 'number', defaultValue: 1000, description: 'Concurrent rating simulations (up to 10,000)' }
    ]
  }
];

export { ReinsuranceHeatmapComponent } from './reinsuranceHeatmap';
export { AIUnderwritingWorkbenchComponent } from './aiUnderwritingWorkbench';
export { ParametricMapComponent } from './parametricMap';


