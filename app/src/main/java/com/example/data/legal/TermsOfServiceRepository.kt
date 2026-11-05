package com.example.data.legal

import com.example.util.SecuritySanitizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Enumerates all 21 mandatory sections of the SolarFix Terms of Use and Terms of Service.
 */
enum class TermsSectionTopic(
    val sectionNumber: Int,
    val title: String,
    val shortLabel: String
) {
    ACCEPTANCE_OF_TERMS(
        1,
        "1. Acceptance of Terms",
        "Acceptance"
    ),
    ELIGIBILITY(
        2,
        "2. Eligibility to Use SolarFix",
        "Eligibility"
    ),
    CUSTOMER_RESPONSIBILITIES(
        3,
        "3. Customer Responsibilities",
        "Customer Duties"
    ),
    SELLER_RESPONSIBILITIES(
        4,
        "4. Independent Seller Responsibilities",
        "Seller Duties"
    ),
    SELLER_REGISTRATION_AND_VERIFICATION(
        5,
        "5. Seller Registration & Verification",
        "Seller KYB/KYC"
    ),
    PRODUCT_LISTING_REQUIREMENTS(
        6,
        "6. Product Listing Requirements",
        "Listing Rules"
    ),
    COUNTERFEIT_PROHIBITION(
        7,
        "7. Prohibition of Counterfeit Products",
        "Anti-Counterfeit"
    ),
    MARKETPLACE_RULES(
        8,
        "8. Multi-Vendor Marketplace Rules",
        "Marketplace Rules"
    ),
    PRICING_AND_PAYMENT_CONDITIONS(
        9,
        "9. Pricing and Payment Conditions",
        "Pricing & Payment"
    ),
    COMMISSION_ARRANGEMENTS(
        10,
        "10. SolarFix Commission Arrangements",
        "Commissions"
    ),
    SELLER_PAYOUT_CONDITIONS(
        11,
        "11. Seller Payout Conditions",
        "Seller Payouts"
    ),
    DELIVERY_RESPONSIBILITIES(
        12,
        "12. Delivery Responsibilities",
        "Delivery"
    ),
    CANCELLATION_POLICIES(
        13,
        "13. Order Cancellation Policies",
        "Cancellations"
    ),
    REFUND_AND_RETURN_CONDITIONS(
        14,
        "14. Refund and Return Conditions",
        "Refunds & Returns"
    ),
    DISPUTE_RESOLUTION(
        15,
        "15. Technical Inspection & Dispute Resolution",
        "Dispute Resolution"
    ),
    PROHIBITED_ACTIVITIES(
        16,
        "16. Prohibited Activities",
        "Prohibited Acts"
    ),
    ACCOUNT_SUSPENSION_AND_TERMINATION(
        17,
        "17. Account Suspension and Termination",
        "Suspension"
    ),
    INTELLECTUAL_PROPERTY(
        18,
        "18. Intellectual Property Rights",
        "Intellectual Property"
    ),
    LIMITATION_OF_LIABILITY(
        19,
        "19. Fair Allocation & Limitation of Liability",
        "Liability"
    ),
    CHANGES_TO_TERMS(
        20,
        "20. Changes to the Terms",
        "Updates to Terms"
    ),
    CONTACT_AND_COMPLAINT_PROCEDURES(
        21,
        "21. Contact and Complaint Procedures",
        "Contact & Complaints"
    )
}

/**
 * Structured representation of a single legal section in the SolarFix Terms of Use & Terms of Service.
 */
data class TermsOfServiceSection(
    val topic: TermsSectionTopic,
    val summary: String,
    val detailedClauses: List<String>,
    val appliesToCustomer: Boolean = true,
    val appliesToSeller: Boolean = true,
    val statutoryRightsPreservationNote: String? = null
)

/**
 * Side-by-side comparison entry clearly distinguishing SolarFix's responsibilities as a platform operator
 * from those of independent marketplace sellers.
 */
data class ResponsibilityComparisonEntry(
    val domain: String,
    val solarFixPlatformResponsibility: String,
    val independentSellerResponsibility: String,
    val customerProtectionGuarantee: String
)

/**
 * Immutable audit record when a customer or seller accepts a version of the Terms of Use & Terms of Service.
 */
data class TermsAcceptanceRecord(
    val userIdOrEmail: String,
    val acceptedVersion: String,
    val acceptedAtEpochMs: Long,
    val entryPoint: String,
    val acceptedRole: String
)

/**
 * Single source of truth for SolarFix's Terms of Use and Terms of Service.
 *
 * Key guarantees:
 * 1. Covers all 21 mandatory legal & operational topics in clear, plain-language and legally accurate clauses.
 * 2. Explicitly distinguishes SolarFix's responsibilities (marketplace facilitator, escrow custodian,
 *    KYB/KYC verifier, AI diagnostic advisor, dispute mediator) from those of independent marketplace sellers
 *    (merchant of record, hardware authenticity & capacity guarantor, OEM warranty provider, pre-dispatch QC).
 * 3. Never introduces misleading terms or unfairly removes customers' statutory consumer protection and legal rights
 *    (explicitly preserves rights under the Federal Competition and Consumer Protection Act (FCCPA) 2018,
 *    Nigeria Data Protection Act (NDPA) 2023, and applicable consumer protection laws).
 */
object TermsOfServiceRepository {

    const val CURRENT_TERMS_VERSION = "2026.11-v3.2"
    const val EFFECTIVE_DATE = "November 5, 2026"
    const val LEGAL_SUPPORT_EMAIL = "legal-compliance@solarfix.ng"
    const val COMPLAINTS_DESK_EMAIL = "complaints-ombudsman@solarfix.ng"
    const val COMPLAINTS_HOTLINE = "+234 800 SOLARFIX (765-27349)"

    private val acceptanceLedger = ConcurrentHashMap<String, TermsAcceptanceRecord>()

    /**
     * Explicit Statutory Consumer Rights Preservation Statement.
     * Ensures no clause in the Terms of Use or Terms of Service misleads customers or purports to waive
     * non-excludable statutory consumer rights.
     */
    const val STATUTORY_RIGHTS_PRESERVATION_BANNER =
        "Statutory Consumer Rights Guarantee: Nothing in these Terms of Use and Terms of Service excludes, " +
            "restricts, or modifies any mandatory consumer rights, remedies, implied warranties of merchantability, " +
            "fitness for purpose, or statutory protections granted to you under the Federal Competition and Consumer " +
            "Protection Act (FCCPA) 2018, the Standards Organisation of Nigeria (SON) Act, the Nigeria Data Protection " +
            "Act (NDPA) 2023, or any other applicable consumer protection legislation."

    /**
     * Explicit Role & Responsibility Matrix distinguishing SolarFix from Independent Marketplace Sellers.
     */
    val responsibilityComparisonMatrix: List<ResponsibilityComparisonEntry> = listOf(
        ResponsibilityComparisonEntry(
            domain = "Contract of Sale & Merchant of Record",
            solarFixPlatformResponsibility = "Operates the digital marketplace infrastructure, verified catalog discovery, order routing, and escrow protection service. SolarFix is not the manufacturer or seller of third-party vendor listings.",
            independentSellerResponsibility = "Acts as the legal Merchant of Record for every solar panel, inverter, lithium battery, or Balance-of-System item listed by their store, bearing primary contractual responsibility for the goods sold.",
            customerProtectionGuarantee = "Customers receive a transparent tax/order invoice identifying the verified seller's legal business name, registration badge, and SolarFix escrow reference."
        ),
        ResponsibilityComparisonEntry(
            domain = "Product Authenticity, Specifications & Warranties",
            solarFixPlatformResponsibility = "Audits seller KYB/KYC credentials, enforces mandatory specification disclosures, removes reported counterfeit listings, and freezes escrow funds when specification discrepancies arise.",
            independentSellerResponsibility = "Guarantees 100% genuine Grade-A OEM hardware, accurate nameplate ratings (W, kVA, Ah, kWh, cycle life), valid serial numbers, and honors the stated manufacturer/seller warranty.",
            customerProtectionGuarantee = "If an item fails load testing or differs from its listed specifications, the customer is entitled to a full 100% escrow refund including return shipping costs."
        ),
        ResponsibilityComparisonEntry(
            domain = "Payments, Escrow Custody & Commissions",
            solarFixPlatformResponsibility = "Collects customer payments via PCI-DSS compliant gateways (Paystack) into a ring-fenced Escrow Trust Account, deducts the transparent 5% platform commission upon completion, and processes verified refunds.",
            independentSellerResponsibility = "Honors the published checkout price without requesting off-platform surcharges, and receives the 95% net settlement only after customer delivery confirmation and inspection.",
            customerProtectionGuarantee = "Seller cannot touch customer funds while goods are in transit or under technical dispute."
        ),
        ResponsibilityComparisonEntry(
            domain = "Packaging, Waybill Dispatch & Transit Safety",
            solarFixPlatformResponsibility = "Tracks waybill milestones, enforces dispatch SLAs, and provides in-app delivery confirmation and dispute reporting tools.",
            independentSellerResponsibility = "Packages high-voltage/fragile solar equipment according to UN3480 (Lithium Ion) and IEC shock-protection standards, dispatches within the stated SLA, and bears transit risk until signed delivery.",
            customerProtectionGuarantee = "Goods damaged in transit prior to customer acceptance are eligible for immediate replacement or full escrow reversal at no cost to the customer."
        ),
        ResponsibilityComparisonEntry(
            domain = "AI Solar Technician & Diagnostic Guidance",
            solarFixPlatformResponsibility = "Provides AI-assisted fault analysis, load sizing estimates, and directory access to certified physical solar technicians as decision-support tools.",
            independentSellerResponsibility = "Provides accurate wiring diagrams, BMS DIP-switch settings, and inverter parameter sheets specific to the hardware model sold.",
            customerProtectionGuarantee = "High-voltage physical installation and live AC/DC troubleshooting must always be performed or verified by a certified solar engineer."
        )
    )

    /**
     * All 21 comprehensive sections of the SolarFix Terms of Use and Terms of Service.
     */
    val allSections: List<TermsOfServiceSection> = listOf(
        TermsOfServiceSection(
            topic = TermsSectionTopic.ACCEPTANCE_OF_TERMS,
            summary = "Explains how accessing, registering, or transacting on SolarFix forms a binding, fair, and transparent agreement.",
            detailedClauses = listOf(
                "1.1 Binding Agreement: By creating an account, signing in, browsing the marketplace, using the AI Solar Technician, or registering as a vendor on SolarFix, you agree to comply with these Terms of Use and Terms of Service ('Terms').",
                "1.2 Plain-Language Transparency: These Terms are written in clear language so that Customers, Independent Sellers, and Certified Technicians understand their exact rights, obligations, and remedies before committing to any transaction.",
                "1.3 Non-Waiver of Statutory Consumer Rights: Acceptance of these Terms does not waive, diminish, or override your mandatory legal rights under Nigerian consumer protection, electrical safety, or data privacy laws."
            ),
            statutoryRightsPreservationNote = "Your statutory rights under the Federal Competition and Consumer Protection Act (FCCPA) 2018 remain fully enforceable at all times."
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.ELIGIBILITY,
            summary = "Sets out age, legal capacity, and electrical safety prerequisites for using SolarFix.",
            detailedClauses = listOf(
                "2.1 Age and Legal Capacity: You must be at least 18 years old and legally capable of entering into binding contracts under applicable law to register a Customer or Seller account.",
                "2.2 Corporate & Commercial Representation: If you register on behalf of a company, solar installation firm, or cooperative, you confirm that you have authority to bind that legal entity.",
                "2.3 High-Voltage Safety Awareness: Solar inverters, PV strings, and lithium/tubular battery banks operate at potentially lethal DC and AC voltages. Users must engage qualified technicians for physical wiring and commissioning."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.CUSTOMER_RESPONSIBILITIES,
            summary = "Outlines customer duties regarding accurate account info, safe installation, and timely delivery inspection.",
            detailedClauses = listOf(
                "3.1 Accurate Account & Delivery Details: Customers must provide accurate contact details, valid email addresses, and reachable delivery addresses so high-value solar freight can be delivered safely.",
                "3.2 Account Credential Security: Customers are responsible for safeguarding their login passwords and one-time verification codes (OTPs) and notifying SolarFix immediately of any unauthorized account use.",
                "3.3 Safe Installation & Operation: Customers agree to have inverters, charge controllers, and battery banks installed in accordance with manufacturer manuals and IEC/NEMSA electrical standards.",
                "3.4 Good-Faith Inspection Upon Delivery: Customers should inspect delivered solar hardware within the 7-day escrow inspection window to verify physical condition, serial numbers, and nameplate ratings."
            ),
            appliesToCustomer = true,
            appliesToSeller = false
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.SELLER_RESPONSIBILITIES,
            summary = "Defines the legal and operational obligations of independent marketplace sellers as the Merchant of Record.",
            detailedClauses = listOf(
                "4.1 Merchant of Record Status: Each Verified Seller is an independent business entity and the legal Merchant of Record for products sold from its SolarFix storefront. SolarFix is the platform operator and escrow custodian, not the seller of third-party vendor goods.",
                "4.2 Hardware Quality & Pre-Dispatch Testing: Sellers must perform pre-dispatch quality control (including open-circuit voltage, BMS health check, and physical casing inspection) before handing equipment to logistics carriers.",
                "4.3 Warranty Fulfillment & Technical Support: Sellers must honor all advertised manufacturer and store warranties, supply genuine warranty cards/serial numbers, and respond to customer technical inquiries within 24 business hours.",
                "4.4 Regulatory & Tax Compliance: Sellers are solely responsible for complying with SONCAP import certifications, customs duties, and applicable Value Added Tax (VAT) or corporate tax obligations on their sales."
            ),
            appliesToCustomer = false,
            appliesToSeller = true
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.SELLER_REGISTRATION_AND_VERIFICATION,
            summary = "Details mandatory Know-Your-Business (KYB) and Know-Your-Customer (KYC) verification before a seller can list products.",
            detailedClauses = listOf(
                "5.1 Mandatory Multi-Step KYB/KYC Verification: Prospective sellers must submit a valid Corporate Affairs Commission (CAC) Registration Number, Tax Identification Number (TIN), Government-issued ID, physical warehouse address, and business verification photographs.",
                "5.2 Private Document Security: Seller identity and business verification documents are encrypted at rest, strictly isolated from public product media, and accessible only to authorized SolarFix Compliance Administrators.",
                "5.3 Ongoing Verification Audits: SolarFix reserves the right to conduct periodic re-verification or physical warehouse audits to ensure continued compliance with marketplace standards."
            ),
            appliesToCustomer = false,
            appliesToSeller = true
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.PRODUCT_LISTING_REQUIREMENTS,
            summary = "Requires complete, truthful technical specifications and genuine product photography for all solar equipment.",
            detailedClauses = listOf(
                "6.1 Accurate Nameplate & Technical Disclosures: Every listing must state true continuous power rating (kVA/kW), surge rating, Nominal Voltage (12V/24V/48V), battery capacity (Ah/kWh), chemistry (LiFePO4, Tubular Gel, AGM), cycle life at specified Depth of Discharge (DoD), and PV efficiency.",
                "6.2 Authentic Media Only: Product photographs and videos must accurately depict the exact model and grade being shipped. Misleading stock graphics ordoctored specification labels are prohibited.",
                "6.3 Clear Condition & Warranty Disclosure: Listings must explicitly state whether an item is Brand New OEM Grade-A, Manufacturer Refurbished, or Used, along with the exact warranty duration in months or years."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.COUNTERFEIT_PROHIBITION,
            summary = "Establishes zero tolerance for counterfeit, re-labeled, cloned, or degraded second-life cells sold as new.",
            detailedClauses = listOf(
                "7.1 Zero-Tolerance Anti-Counterfeit Policy: Listing counterfeit inverters, cloned MPPT charge controllers, re-wrapped or used EV cells misrepresented as brand-new Grade-A LiFePO4 cells, or solar panels with inflated wattage stickers is strictly prohibited.",
                "7.2 Immediate Escrow Freeze & Full Customer Refund: If a product is found to be counterfeit, cloned, or materially below its rated capacity, SolarFix will immediately freeze the transaction escrow and issue a 100% refund (plus return freight costs) to the Customer.",
                "7.3 Permanent Delisting & Regulatory Reporting: Sellers who list counterfeit or hazardous electrical hardware face immediate store de-verification, permanent ban, and reporting to the Standards Organisation of Nigeria (SON) and FCCPC."
            ),
            statutoryRightsPreservationNote = "Customers have an absolute statutory right to receive authentic goods that match their technical description and safety standards."
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.MARKETPLACE_RULES,
            summary = "Governs fair multi-vendor conduct, anti-circumvention of escrow, and authentic customer reviews.",
            detailedClauses = listOf(
                "8.1 Escrow Protection Integrity (No Off-Platform Circumvention): All marketplace orders and payments must be processed through SolarFix's integrated escrow checkout. Sellers must never direct customers to pay into personal bank accounts outside SolarFix.",
                "8.2 Honest Reviews & Ratings: Customers have the right to post honest, factual reviews and test measurements of purchased solar equipment. Sellers are prohibited from manipulating ratings, offering bribes for removing negative reviews, or posting fake reviews.",
                "8.3 Respectful Professional Communication: All participants must communicate truthfully and respectfully during order fulfillment, technical support, and dispute mediation."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.PRICING_AND_PAYMENT_CONDITIONS,
            summary = "Explains transparent pricing, server-side amount verification, and Paystack escrow payment processing.",
            detailedClauses = listOf(
                "9.1 Transparent All-Inclusive Checkout Pricing: Listed prices must clearly reflect the unit cost in Nigerian Naira (NGN) or selected display currency. Sellers may not demand hidden post-checkout fees or unannounced price increases after an order is placed.",
                "9.2 Backend Payment Verification & Escrow Custody: Customer payments are processed via TLS-encrypted PCI-DSS Level 1 gateways (Paystack) and verified server-side. SolarFix never stores raw credit/debit card numbers, CVVs, or banking PINs.",
                "9.3 Escrow Hold Period: Customer funds are held securely in SolarFix Escrow until delivery is confirmed and the inspection window is satisfied."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.COMMISSION_ARRANGEMENTS,
            summary = "Sets out SolarFix's transparent 5% platform commission structure on completed marketplace orders.",
            detailedClauses = listOf(
                "10.1 Standard 5% Platform Commission: SolarFix charges a flat, transparent 5.0% platform commission on the gross item value of successfully completed and accepted marketplace orders, leaving 95.0% as net seller earnings.",
                "10.2 What the Commission Covers: The 5% commission funds Paystack payment processing, escrow custody, KYB/KYC compliance verification, automated fraud & rate-limit protection, and technical dispute mediation.",
                "10.3 No Commission on Cancelled or Refunded Orders: If an order is cancelled prior to dispatch or refunded due to seller default, defective hardware, or transit damage, no platform commission is charged."
            ),
            appliesToCustomer = false,
            appliesToSeller = true
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.SELLER_PAYOUT_CONDITIONS,
            summary = "Governs how net seller earnings unlock from escrow and are settled exclusively to verified corporate bank accounts.",
            detailedClauses = listOf(
                "11.1 Escrow Release Trigger: A seller's 95% net proceeds move from 'Escrow Locked' to 'Available Balance' once the customer confirms receipt in good working order or upon expiration of the 7-day post-delivery inspection window without an active dispute.",
                "11.2 Verified Bank Account Restriction: Withdrawals are strictly restricted to the seller's verified Nigerian commercial bank account matching their registered KYB/KYC legal identity.",
                "11.3 Anti-Manipulation & Audit Controls: All commission deductions, escrow releases, and withdrawal requests are calculated server-side in an immutable cryptographic ledger and subject to velocity and anomaly checks."
            ),
            appliesToCustomer = false,
            appliesToSeller = true
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.DELIVERY_RESPONSIBILITIES,
            summary = "Clarifies dispatch timelines, heavy-freight handling, waybill tracking, and risk of loss during transit.",
            detailedClauses = listOf(
                "12.1 Dispatch Timelines & Tracking: Sellers must dispatch paid orders within 24–48 business hours (unless a custom pre-order lead time is explicitly stated on the listing) and upload verifiable waybill/courier tracking details.",
                "12.2 Heavy Solar Freight & Hazardous Goods Packaging: Solar panels (tempered glass) and lithium/lead-acid batteries must be crated or palletized with moisture and shock protection.",
                "12.3 Risk of Loss in Transit: The Seller bears the risk of physical loss or transit damage until the shipment is delivered to and signed for at the Customer's designated delivery address."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.CANCELLATION_POLICIES,
            summary = "Protects customers' right to cancel unshipped or delayed orders for an immediate 100% escrow refund.",
            detailedClauses = listOf(
                "13.1 Pre-Dispatch Customer Cancellation: Customers may cancel any order free of charge at any time before the Seller dispatches the item and receive an immediate 100% refund from escrow.",
                "13.2 Automatic SLA Breach Cancellation: If a Seller fails to dispatch an order within the committed fulfillment window and does not obtain the Customer's consent to an extension, the Customer may cancel for an immediate full refund.",
                "13.3 Seller-Initiated Cancellation Penalties: Sellers must maintain accurate live inventory counts. Repeated stock-out cancellations by a Seller may result in store visibility demotion or account suspension."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.REFUND_AND_RETURN_CONDITIONS,
            summary = "Guarantees fair refunds and returns for defective, damaged, counterfeit, or misdescribed solar equipment.",
            detailedClauses = listOf(
                "14.1 Full Refund Rights for Non-Conforming or Defective Goods: Customers are entitled to a 100% refund (including original delivery and return shipping costs) if delivered hardware is Dead-on-Arrival (DOA), damaged in transit, counterfeit, missing components, or fails to meet its advertised nameplate capacity.",
                "14.2 7-Day Escrow Inspection Window: Customers have 7 calendar days from confirmed delivery to test equipment and initiate a return/refund request directly inside SolarFix before escrow funds are released to the Seller.",
                "14.3 Post-Escrow Statutory & OEM Warranty Remedies: Even after the 7-day escrow window closes, Customers retain their full manufacturer/seller warranty rights and statutory rights under the FCCPA 2018 against latent defects.",
                "14.4 Fast Escrow Reversal Processing: Approved escrow refunds are reversed directly to the Customer's original payment method within 1–5 business days."
            ),
            statutoryRightsPreservationNote = "Under Sections 122 and 130 of the Federal Competition and Consumer Protection Act (FCCPA) 2018, consumers have an inalienable right to return unsafe, defective, or substandard goods for a full refund or replacement."
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.DISPUTE_RESOLUTION,
            summary = "Establishes an impartial, evidence-based technical mediation process without restricting access to courts or regulators.",
            detailedClauses = listOf(
                "15.1 Step 1 — In-App Escrow Hold & Direct Resolution: Raising a dispute on an order immediately freezes the escrow payout. The Customer and Seller have 48 hours to resolve the issue (e.g., replacement part, configuration assistance, or return authorization).",
                "15.2 Step 2 — SolarFix Technical Ombudsman Review: If unresolved after 48 hours, SolarFix's Technical Dispute Team reviews multimeter/BMS logs, unboxing photos, serial numbers, and waybill records to issue an impartial escrow determination within 5 business days.",
                "15.3 Preservation of External Legal & Regulatory Remedies: Participating in SolarFix's internal dispute resolution never prevents a Customer or Seller from filing a complaint with the Federal Competition and Consumer Protection Commission (FCCPC) or pursuing claims in a court of competent jurisdiction."
            ),
            statutoryRightsPreservationNote = "Internal platform mediation is a free consumer protection benefit and never blocks your right to approach the FCCPC or the courts."
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.PROHIBITED_ACTIVITIES,
            summary = "Lists strictly forbidden security, financial, and marketplace abuses on SolarFix.",
            detailedClauses = listOf(
                "16.1 Financial & Escrow Tampering: Attempting to manipulate payment amounts, forge webhook signatures, trigger duplicate withdrawals, or modify commission records is strictly prohibited.",
                "16.2 Cyber & API Abuse: Automated scraping, credential stuffing, brute-force login attacks, SQL/NoSQL injection, XSS payload injection, or uploading malicious executables/scripts is forbidden.",
                "16.3 Identity & Role Escalation Fraud: Impersonating another user, submitting forged CAC/KYC documents, or attempting unauthorized privilege escalation to Seller or Administrator roles is strictly prohibited."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.ACCOUNT_SUSPENSION_AND_TERMINATION,
            summary = "Explains proportional enforcement actions, notice procedures, and right to appeal account restrictions.",
            detailedClauses = listOf(
                "17.1 Grounds for Restriction or Suspension: SolarFix may temporarily restrict or suspend an account for verified security threats, counterfeit sales, payment fraud, repeated SLA defaults, or severe violations of these Terms.",
                "17.2 Protection of Pending Customer Orders & Lawful Funds: Account suspension does not forfeit legitimate Customer refunds or lawful funds for undisputed, completed orders, subject to fraud investigation clearance.",
                "17.3 Notice and Right to Appeal: Except where immediate action is required to stop active fraud or safety hazards, SolarFix provides written notice of the reason for suspension and a clear procedure to appeal to the Compliance Ombudsman."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.INTELLECTUAL_PROPERTY,
            summary = "Protects SolarFix's software/AI diagnostics and respects sellers' and manufacturers' brand trademarks.",
            detailedClauses = listOf(
                "18.1 SolarFix Platform IP: The SolarFix mobile application, AI Solar Technician diagnostic algorithms, UI design, escrow architecture, and brand trademarks are owned by SolarFix and protected by copyright and trademark laws.",
                "18.2 Seller & Manufacturer Content License: Sellers retain ownership of their original store photography and descriptions while granting SolarFix a non-exclusive license to display them on the marketplace.",
                "18.3 Trademark Respect: Sellers may only use OEM brand names and logos (e.g., Victron, Deye, Growatt, Felicity, Pylontech, Longi, Jinko) accurately to describe genuine products from those manufacturers."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.LIMITATION_OF_LIABILITY,
            summary = "Sets out a fair, balanced allocation of liability between SolarFix and Independent Sellers without excluding mandatory legal liabilities.",
            detailedClauses = listOf(
                "19.1 Responsibilities SolarFix Accepts: SolarFix is directly responsible for the security and accuracy of the Escrow Trust Account, platform cybersecurity, proper handling of encrypted personal data under the NDPA 2023, and exercising reasonable care in verifying Sellers and mediating disputes.",
                "19.2 Responsibilities Borne by Independent Sellers: Because Independent Sellers are the Merchants of Record who control physical inventory, each Seller is directly liable for product authenticity, hardware defects, warranty claims, and regulatory compliance of their listed goods.",
                "19.3 Non-Excludable Liabilities Never Limited: Nothing in these Terms limits or excludes liability for death or personal injury caused by negligence, fraud or fraudulent misrepresentation, gross negligence, willful misconduct, or any statutory consumer liability that cannot lawfully be excluded."
            ),
            statutoryRightsPreservationNote = "SolarFix never uses 'blanket disclaimers' to strip customers of their legal remedies for fraud, negligence, or statutory consumer protections."
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.CHANGES_TO_TERMS,
            summary = "Requires advance notice of material updates and protects existing orders under the terms in effect at checkout.",
            detailedClauses = listOf(
                "20.1 Advance Notice of Material Changes: Whenever SolarFix updates these Terms for regulatory, security, or marketplace improvements, we will update the version number and provide at least 14 days' advance in-app and email notice for material changes.",
                "20.2 Protection of Existing Orders: Any change to commission rates, return windows, or payment terms will never apply retroactively to orders already placed and in escrow prior to the effective date.",
                "20.3 Right to Close Account Without Penalty: If you do not agree to a material update, you may close your account and withdraw any undisputed available balance without penalty."
            )
        ),
        TermsOfServiceSection(
            topic = TermsSectionTopic.CONTACT_AND_COMPLAINT_PROCEDURES,
            summary = "Provides direct, responsive contact channels and formal escalation timelines for customer and seller complaints.",
            detailedClauses = listOf(
                "21.1 Official Support & Legal Channels: You may reach our Legal & Compliance Desk at $LEGAL_SUPPORT_EMAIL, our Ombudsman & Complaints Desk at $COMPLAINTS_DESK_EMAIL, or call $COMPLAINTS_HOTLINE.",
                "21.2 SLA for Complaint Acknowledgment & Resolution: All formal complaints receive a tracked ticket acknowledgment within 24 hours and a substantive resolution or escalation update within 5 business days.",
                "21.3 Regulatory Escalation Rights: If you remain dissatisfied after completing SolarFix's internal complaint procedure, you have the right to escalate consumer disputes to the Federal Competition and Consumer Protection Commission (FCCPC) or data privacy matters to the Nigeria Data Protection Commission (NDPC)."
            )
        )
    )

    /**
     * Searches sections by keyword across titles, summaries, detailed clauses, and statutory notes.
     */
    fun searchSections(query: String): List<TermsOfServiceSection> {
        val cleanQuery = SecuritySanitizer.sanitizeSearchQuery(query).trim()
        if (cleanQuery.isBlank()) return allSections
        val lower = cleanQuery.lowercase()
        return allSections.filter { section ->
            section.topic.title.lowercase().contains(lower) ||
                section.topic.shortLabel.lowercase().contains(lower) ||
                section.summary.lowercase().contains(lower) ||
                section.detailedClauses.any { it.lowercase().contains(lower) } ||
                (section.statutoryRightsPreservationNote?.lowercase()?.contains(lower) == true)
        }
    }

    /**
     * Retrieves a specific section by its mandatory topic enum.
     */
    fun getSection(topic: TermsSectionTopic): TermsOfServiceSection {
        return allSections.first { it.topic == topic }
    }

    /**
     * Records a user's explicit acceptance of the Terms of Use and Terms of Service.
     */
    fun recordTermsAcceptance(
        userIdOrEmail: String,
        entryPoint: String,
        acceptedRole: String = "CUSTOMER"
    ): TermsAcceptanceRecord {
        val key = SecuritySanitizer.sanitizeText(userIdOrEmail, maxLength = 120).lowercase().ifBlank { "anonymous_session" }
        val safeEntryPoint = SecuritySanitizer.sanitizeText(entryPoint, maxLength = 60).ifBlank { "GENERAL" }
        val safeRole = SecuritySanitizer.sanitizeText(acceptedRole, maxLength = 40).ifBlank { "CUSTOMER" }
        val record = TermsAcceptanceRecord(
            userIdOrEmail = key,
            acceptedVersion = CURRENT_TERMS_VERSION,
            acceptedAtEpochMs = System.currentTimeMillis(),
            entryPoint = safeEntryPoint,
            acceptedRole = safeRole
        )
        acceptanceLedger[key] = record
        return record
    }

    /**
     * Checks whether a user has accepted the latest version of the Terms.
     */
    fun hasAcceptedLatestTerms(userIdOrEmail: String): Boolean {
        val key = SecuritySanitizer.sanitizeText(userIdOrEmail, maxLength = 120).lowercase().ifBlank { "anonymous_session" }
        return acceptanceLedger[key]?.acceptedVersion == CURRENT_TERMS_VERSION
    }

    /**
     * Returns the recorded acceptance metadata for a user if present.
     */
    fun getAcceptanceRecord(userIdOrEmail: String): TermsAcceptanceRecord? {
        val key = SecuritySanitizer.sanitizeText(userIdOrEmail, maxLength = 120).lowercase().ifBlank { "anonymous_session" }
        return acceptanceLedger[key]
    }
}
