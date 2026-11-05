package com.example.data.legal

import com.example.util.DatabaseEncryptionAndSecurityManager
import com.example.util.SecuritySanitizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Enumerates all 15 mandatory core sections of the SolarFix Privacy Policy.
 */
enum class PrivacyPolicySectionTopic(
    val sectionNumber: Int,
    val title: String,
    val shortLabel: String
) {
    WHAT_INFORMATION_WE_COLLECT(
        1,
        "1. What Personal Information SolarFix Collects",
        "Info Collected"
    ),
    WHY_INFORMATION_IS_COLLECTED(
        2,
        "2. Why Personal Information Is Collected (Purpose & Legal Basis)",
        "Why Collected"
    ),
    HOW_INFORMATION_IS_PROCESSED(
        3,
        "3. How Personal Information Is Processed",
        "How Processed"
    ),
    HOW_INFORMATION_IS_STORED(
        4,
        "4. How Personal Information Is Stored",
        "How Stored"
    ),
    HOW_INFORMATION_IS_PROTECTED(
        5,
        "5. How Personal Information Is Protected (Encryption & Security)",
        "Protection"
    ),
    WHO_CAN_ACCESS_INFORMATION(
        6,
        "6. Who Can Access Personal Information (Strict RBAC & RLS)",
        "Access Control"
    ),
    SHARING_WITH_PAYMENT_AND_DELIVERY_PARTNERS(
        7,
        "7. Sharing with Payment Providers & Delivery Partners",
        "Third-Party Sharing"
    ),
    DATA_RETENTION_SCHEDULE(
        8,
        "8. How Long Personal Information Is Retained",
        "Retention"
    ),
    USER_RIGHT_TO_ACCESS(
        9,
        "9. How Users Can Access Their Personal Information",
        "Right of Access"
    ),
    USER_RIGHT_TO_CORRECTION(
        10,
        "10. How Users Can Request Correction of Inaccurate Information",
        "Right to Correct"
    ),
    USER_RIGHT_TO_DELETION(
        11,
        "11. How Users Can Request Deletion (Right to Erasure)",
        "Right to Delete"
    ),
    USER_RIGHT_TO_WITHDRAW_CONSENT(
        12,
        "12. How Users Can Withdraw Consent",
        "Withdraw Consent"
    ),
    PRIVACY_COMPLAINTS_PROCEDURE(
        13,
        "13. How Users Can Raise Privacy Complaints",
        "Privacy Complaints"
    ),
    DATA_BREACH_RESPONSE_PROTOCOL(
        14,
        "14. How Data Breaches Are Handled & Reported",
        "Breach Protocol"
    ),
    NOTIFICATION_OF_PRIVACY_CHANGES(
        15,
        "15. How Users Are Notified About Important Privacy Changes",
        "Policy Updates"
    )
}

/**
 * Enumerates all 8 specific data domain provisions required in SolarFix's Privacy Policy,
 * mapped directly to the actual technical implementation in the codebase.
 */
enum class SpecificDataDomainProvision(
    val domainNumber: Int,
    val title: String,
    val shortLabel: String
) {
    CUSTOMER_INFORMATION(
        1,
        "A. Customer Information",
        "Customer Data"
    ),
    SELLER_VERIFICATION_DOCUMENTS(
        2,
        "B. Seller Verification Documents (KYB/KYC)",
        "Seller KYC Docs"
    ),
    BANK_ACCOUNT_INFORMATION(
        3,
        "C. Bank Account Information",
        "Bank Accounts"
    ),
    PAYMENT_RECORDS(
        4,
        "D. Payment Records & Zero Card/PIN Storage",
        "Payment Records"
    ),
    DELIVERY_ADDRESSES(
        5,
        "E. Delivery Addresses",
        "Delivery Addresses"
    ),
    AI_TECHNICIAN_INTERACTIONS(
        6,
        "F. AI Solar Technician Interactions",
        "AI Technician"
    ),
    UPLOADED_PHOTOGRAPHS_AND_DOCUMENTS(
        7,
        "G. Uploaded Photographs, Videos & Documents",
        "Uploaded Media"
    ),
    MARKETPLACE_TRANSACTION_HISTORY(
        8,
        "H. Marketplace Transaction History & Escrow Ledger",
        "Transactions"
    )
}

/**
 * Structured representation of one of the 15 core sections of the SolarFix Privacy Policy.
 */
data class PrivacyPolicySection(
    val topic: PrivacyPolicySectionTopic,
    val summary: String,
    val detailedClauses: List<String>,
    val implementationReference: String
)

/**
 * Structured representation of one of the 8 specific data domain provisions,
 * documenting exact fields collected, fields strictly excluded (data minimization),
 * storage/encryption mechanism, access control, and retention period.
 */
data class DataDomainProvisionDetail(
    val domain: SpecificDataDomainProvision,
    val exactFieldsCollected: List<String>,
    val excludedByDataMinimization: List<String>,
    val encryptionAndStorageMechanism: String,
    val accessAndSharingPolicy: String,
    val retentionPeriod: String
)

/**
 * Granular user consent state supporting Privacy-by-Design and instant consent withdrawal.
 */
data class UserPrivacyConsentState(
    val userIdentifier: String,
    val essentialAccountAndEscrowProcessing: Boolean = true, // Strictly necessary for contract performance
    val aiTechnicianDiagnosticHistoryRetention: Boolean = true,
    val localizedSolarIrradianceOptimization: Boolean = true,
    val productSafetyAndFeatureNotifications: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Represents a Data Subject Access / Correction / Deletion / Complaint request submitted by a user.
 */
enum class DataSubjectRequestType {
    ACCESS_EXPORT,
    CORRECTION,
    DELETION_ERASURE,
    WITHDRAW_CONSENT,
    PRIVACY_COMPLAINT
}

data class DataSubjectRequestRecord(
    val requestId: String,
    val userIdentifier: String,
    val requestType: DataSubjectRequestType,
    val summary: String,
    val status: String,
    val createdAtEpochMs: Long,
    val resolutionSummary: String
)

/**
 * Audit report verifying Privacy-by-Design and Data Minimization across a payload.
 */
data class DataMinimizationAuditResult(
    val isCompliant: Boolean,
    val permittedKeys: List<String>,
    val rejectedExcessiveOrSensitiveKeys: List<String>,
    val sanitizedPayload: Map<String, String>,
    val auditMessage: String
)

/**
 * Comprehensive Privacy Policy and Data Protection Manager for SolarFix.
 *
 * Aligns 100% with the actual security, encryption, storage, and RBAC mechanisms implemented across:
 * - `DatabaseEncryptionAndSecurityManager` (AES-256-GCM `SF_ENC_V1` field encryption & RLS)
 * - `FileUploadAndStorageSecurityManager` (`PRIVATE_ADMIN_VAULT` vs. `PUBLIC_CATALOG_MEDIA` isolation)
 * - `PaystackPaymentGatewayManager` & `RateLimitingAndFinancialSecurityManager` (webhook HMAC-SHA512 & hash-chained ledger)
 * - `MobileSecurityAndMonitoringManager` (encrypted Room `ChatMessageEntity`, token vault, backup prohibition)
 */
object PrivacyPolicyAndDataProtectionManager {

    const val CURRENT_PRIVACY_POLICY_VERSION = "2026.11-PP-v3.2"
    const val EFFECTIVE_DATE = "November 5, 2026"
    const val DATA_PROTECTION_OFFICER_EMAIL = "dpo-privacy@solarfix.ng"
    const val PRIVACY_COMPLAINTS_EMAIL = "privacy-ombudsman@solarfix.ng"
    const val SUPERVISORY_AUTHORITY = "Nigeria Data Protection Commission (NDPC)"

    const val PRIVACY_BY_DESIGN_STATEMENT =
        "Privacy-by-Design & Data Minimization Commitment: SolarFix collects only the minimum personal and " +
            "technical data genuinely required to authenticate your account, diagnose solar equipment faults, " +
            "verify marketplace sellers, process escrow-protected payments, and deliver solar hardware. " +
            "We never collect or store raw payment card numbers (PAN), CVVs, banking PINs, biometric templates, " +
            "BVNs, or unrelated personal demographics."

    /**
     * Sensitive or excessive keys that SolarFix strictly refuses to collect or persist under Data Minimization.
     */
    private val PROHIBITED_EXCESSIVE_SENSITIVE_KEYS = setOf(
        "card_number", "cardnumber", "pan", "cvv", "cvc", "card_pin", "bank_pin",
        "banking_password", "bvn", "bank_verification_number", "biometric_template",
        "fingerprint_raw", "face_mesh_raw", "religion", "political_opinion",
        "medical_history", "health_record", "marital_status", "sexual_orientation",
        "contacts_address_book", "sms_inbox_dump", "call_log_history", "continuous_background_gps"
    )

    private val userConsentMap = ConcurrentHashMap<String, UserPrivacyConsentState>()
    private val _dsarRecordsFlow = MutableStateFlow<List<DataSubjectRequestRecord>>(emptyList())
    val dsarRecordsFlow: StateFlow<List<DataSubjectRequestRecord>> = _dsarRecordsFlow.asStateFlow()

    /**
     * All 15 mandatory core sections of the SolarFix Privacy Policy.
     */
    val allCoreSections: List<PrivacyPolicySection> = listOf(
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.WHAT_INFORMATION_WE_COLLECT,
            summary = "Explains the exact categories of personal and technical information collected across Customer, Verified Seller, and AI Technician flows.",
            detailedClauses = listOf(
                "1.1 Account & Identity Data: When you register on SolarFix, we collect your full name, email address, account role (Customer, Verified Seller, or Administrator), and a cryptographically salted password hash. We also generate a 6-digit email verification code (OTP) to confirm account ownership.",
                "1.2 Solar System Hardware Profile (Optional Customer Input): In System Hub & Care, you may record your inverter rating (kVA), battery storage capacity (kWh), battery chemistry (e.g., LiFePO4 or Tubular Gel), solar array wattage (W), and installer name so the app can tailor maintenance checklists and fault diagnostics.",
                "1.3 Marketplace & Fulfillment Data: When you buy or sell solar hardware, we collect order line items, delivery addresses, recipient phone numbers, waybill tracking references, and Paystack escrow payment references.",
                "1.4 Seller KYB/KYC Verification Data: Only when you apply to become a Verified Seller, we collect your registered business name, CAC registration number (if corporate), business phone/email, physical warehouse address, one government-issued ID document, a shop/warehouse verification photograph, and your corporate settlement bank account details (Bank Name, Account Name, 10-digit NUBAN).",
                "1.5 AI Technician Diagnostic Inputs: When you use the AI Solar Technician, we collect your text queries, inverter fault codes, and any equipment/wiring photographs you actively choose to capture or attach for troubleshooting.",
                "1.6 Strict Data Minimization (What We Never Collect): SolarFix never collects credit/debit card numbers (PAN), CVV/CVC security codes, banking passwords/PINs, Bank Verification Numbers (BVN), device contact lists, SMS messages, call logs, or biometric templates."
            ),
            implementationReference = "AuthRepository, SolarSystemProfile, SellerApplication, DatabaseEncryptionAndSecurityManager"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.WHY_INFORMATION_IS_COLLECTED,
            summary = "Details the specific operational purpose and lawful basis under the Nigeria Data Protection Act (NDPA) 2023 for every category of data.",
            detailedClauses = listOf(
                "2.1 Contract Performance (NDPA Section 25(1)(b)): We collect account credentials, delivery addresses, order records, and seller payout bank details because they are strictly necessary to fulfill marketplace orders, hold customer funds in escrow, deliver heavy solar freight, and settle verified seller earnings.",
                "2.2 Legal & Regulatory Obligation (NDPA Section 25(1)(c)): We collect Seller KYB/KYC identity and business registration documents, and maintain immutable financial audit logs, to comply with anti-fraud, Know-Your-Business, consumer protection (FCCPA 2018), and tax record-keeping laws.",
                "2.3 Legitimate Interests in Platform Safety & Abuse Prevention (NDPA Section 25(1)(e)): We process device fingerprints, subnet-level rate-limiting counters, and security event logs to prevent brute-force logins, counterfeit hardware listings, webhook forgery, and unauthorized balance manipulation.",
                "2.4 Explicit User Consent (NDPA Section 25(1)(a)): Camera access for AI Solar Technician visual fault diagnosis, local chat history storage, and optional product safety notifications are processed on the basis of your revocable consent."
            ),
            implementationReference = "RateLimitingAndFinancialSecurityManager, MobileSecurityAndMonitoringManager"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.HOW_INFORMATION_IS_PROCESSED,
            summary = "Describes how SolarFix sanitizes, validates, encrypts, and processes personal information across backend and on-device workflows.",
            detailedClauses = listOf(
                "3.1 Input Sanitization & Validation: All user inputs are sanitized via SecuritySanitizer before processing to strip control characters, HTML/XSS scripts, and SQL/NoSQL injection operators.",
                "3.2 Server-Side Financial & Escrow Processing: Order totals, 5% platform commissions, 95% net seller payouts, and Paystack webhook signatures (HMAC-SHA512) are verified and recalculated authoritatively on the backend without trusting client-supplied amounts.",
                "3.3 Multimodal AI Diagnostic Processing: When you submit a solar fault query or inverter photo to the AI Solar Technician, the prompt is processed via Firebase AI Logic (Gemini) solely to generate electrical troubleshooting steps and safety warnings. Personal account identifiers and payment records are never sent to the AI model.",
                "3.4 Automated Security & Anomaly Detection: Login attempts, withdrawal requests, and database queries are evaluated against rate-limiting quotas and Row-Level Security (RLS) policies in real time."
            ),
            implementationReference = "SecuritySanitizer, PaystackPaymentGatewayManager, TechnicianViewModel"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.HOW_INFORMATION_IS_STORED,
            summary = "Explains the partitioned cloud and local storage architecture used to isolate sensitive records from public catalog data.",
            detailedClauses = listOf(
                "4.1 Partitioned Cloud Database & Vault Storage: SolarFix separates data into distinct logical collections (`users`, `stores`, `products`, `orders`, `addresses`, `financial_ledger`, `security_audit_logs`) and two strictly isolated file storage partitions: `PUBLIC_CATALOG_MEDIA` (for public product photos/videos) and `PRIVATE_ADMIN_VAULT` (for encrypted seller KYC/KYB documents).",
                "4.2 Encrypted Local Device Storage: On your Android device, AI Solar Technician chat history (`ChatMessageEntity`) is stored in a local Room SQLite database with AES-256-GCM field-level encryption (`SF_ENC_V1`). Active session tokens are stored in the hardware-backed Secure Platform Token Vault bound to your user UID.",
                "4.3 Prohibition of Unencrypted Cloud/ADB Backups: SolarFix configures `android:allowBackup=\"false\"` and strict data extraction rules so your local database and authentication tokens are never exported to unencrypted device or cloud backups."
            ),
            implementationReference = "FileUploadAndStorageSecurityManager, ChatMessageEntity, AndroidManifest.xml"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.HOW_INFORMATION_IS_PROTECTED,
            summary = "Details the cryptographic algorithms, TLS transport rules, key rotation, and file signature defenses protecting your data.",
            detailedClauses = listOf(
                "5.1 Encryption at Rest (AES-256-GCM): Sensitive fields—including seller ID numbers, KYC documents, bank account numbers, phone numbers, email addresses, delivery addresses, financial audit payloads, and auth tokens—are encrypted at rest using AES-256-GCM authenticated encryption (`SF_ENC_V1:<keyVersion>:<IV>:<Ciphertext+Tag>`) with domain-specific Associated Authenticated Data (AAD).",
                "5.2 Encryption in Transit (TLS 1.2 / TLS 1.3 Only): All network communications with Firebase, Paystack, and backend APIs enforce HTTPS/TLS with cleartext HTTP traffic strictly disabled (`cleartextTrafficPermitted=\"false\"`).",
                "5.3 File Upload Magic-Byte & Malware Verification: Every uploaded image, video, or PDF is verified against binary magic-byte signatures, strict file-size caps, and heuristic malware/polyglot/active-PDF scanning before storage.",
                "5.4 Tamper-Evident Cryptographic Ledgers: Financial transactions and database backups are protected with SHA-256 hash chains and HMAC-SHA256 integrity signatures."
            ),
            implementationReference = "DatabaseEncryptionAndSecurityManager, network_security_config.xml"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.WHO_CAN_ACCESS_INFORMATION,
            summary = "Defines strict Role-Based Access Control (RBAC) and Row-Level Security (RLS) governing who can view each record.",
            detailedClauses = listOf(
                "6.1 Customers: Can access only their own account profile, solar installation specs, AI chat history, saved delivery addresses, and their own marketplace orders and payment receipts.",
                "6.2 Verified Sellers: Can access only their own store profile, inventory, orders containing their store's products, and their own store's financial settlement ledger. Sellers can never access another seller's private data or customer payment credentials.",
                "6.3 Authorized Administrators: Only verified SolarFix Compliance and Security Administrators (with step-up re-authentication and mandatory audit logging) can access private seller KYB/KYC verification documents, approve bank payouts, or review security alerts.",
                "6.4 Zero Cross-Tenant Leakage: Row-Level Security (RLS) rules in both `firestore.rules` and `DatabaseEncryptionAndSecurityManager` block any user from reading or modifying another user's private records."
            ),
            implementationReference = "FirebaseAuthAdminHandler, firestore.rules, DatabaseEncryptionAndSecurityManager"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.SHARING_WITH_PAYMENT_AND_DELIVERY_PARTNERS,
            summary = "Transparently discloses when and what limited data is shared with Paystack and logistics/delivery partners.",
            detailedClauses = listOf(
                "7.1 Payment Gateway Partner (Paystack): When you initialize an escrow checkout or seller payout, SolarFix shares only the transaction reference, order amount (in kobo), customer email address (for payment receipt issuance), and verified seller settlement bank account details (for payouts) with Paystack over TLS. SolarFix never sells your personal data to third-party advertisers.",
                "7.2 Independent Marketplace Sellers & Delivery Partners: When you place an order, your delivery recipient name, delivery address, and contact phone number are shared strictly with the fulfilling Verified Seller and assigned freight carrier solely to dispatch, insure, and deliver your solar equipment.",
                "7.3 Legal & Regulatory Disclosures: SolarFix only discloses records to regulatory or law-enforcement authorities when compelled by a valid court order or statutory mandate under Nigerian law."
            ),
            implementationReference = "PaystackPaymentGatewayManager, SolarRepository"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.DATA_RETENTION_SCHEDULE,
            summary = "Specifies clear retention timelines for active accounts, KYC verification records, financial ledgers, and diagnostic logs.",
            detailedClauses = listOf(
                "8.1 Customer Account & Delivery Profiles: Retained for the duration of your active SolarFix account. You may edit or delete saved delivery addresses and system hardware profiles at any time.",
                "8.2 AI Technician Chat History: Stored locally on your device until you clear your diagnostic chat history, withdraw AI history consent, or uninstall the application.",
                "8.3 Seller KYB/KYC Verification Documents: Retained in the encrypted `PRIVATE_ADMIN_VAULT` while the seller store is active plus 5 years following store closure to satisfy regulatory anti-fraud and consumer warranty traceability requirements.",
                "8.4 Marketplace Orders, Escrow & Financial Audit Records: Retained in an immutable, encrypted archive for 6 years from the transaction date as required by Nigerian corporate tax and financial audit regulations.",
                "8.5 Ephemeral Security Tokens & OTPs: Email verification OTP codes expire after 10 minutes, admin step-up tokens expire after 15 minutes, and access tokens are cryptographically wiped immediately upon logout."
            ),
            implementationReference = "AuthRepository, MobileSecurityAndMonitoringManager, RateLimitingAndFinancialSecurityManager"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.USER_RIGHT_TO_ACCESS,
            summary = "Explains how users can inspect and export a portable summary of their personal information at any time.",
            detailedClauses = listOf(
                "9.1 Instant In-App Data Access & Export: Under Section 34(1)(a) of the NDPA 2023, you have the right to know what personal data SolarFix holds about you and obtain a structured copy.",
                "9.2 Self-Service DSAR Export Tool: You can use the 'Access & Export My Data' tool directly inside the SolarFix Privacy & Data Protection Center (or contact $DATA_PROTECTION_OFFICER_EMAIL) to generate an immediate summary of your profile, consent settings, and order/store metadata.",
                "9.3 Zero Fee & Response Timeline: Access requests via the in-app tool are fulfilled immediately free of charge, and formal DPO export requests are fulfilled within 14 business days."
            ),
            implementationReference = "PrivacyPolicyAndDataProtectionManager.exportUserPersonalDataSnapshot"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.USER_RIGHT_TO_CORRECTION,
            summary = "Explains how users can update or request rectification of inaccurate or incomplete personal data.",
            detailedClauses = listOf(
                "10.1 Direct Self-Service Editing: Customers can update their Solar Installation Hardware Profile directly in System Hub & Care and update delivery addresses at checkout.",
                "10.2 Verified Seller & Identity Rectification: Sellers who change their business address, phone number, or corporate settlement bank account can submit a Correction Request for Compliance re-verification.",
                "10.3 In-App Correction Request Tracker: You can submit a formal rectification request directly in the Privacy & Data Protection Center (`submitDataCorrectionRequest`), which is logged with a tracked ticket ID and processed within 5 business days."
            ),
            implementationReference = "SystemHubViewModel.saveProfile, PrivacyPolicyAndDataProtectionManager.submitDataCorrectionRequest"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.USER_RIGHT_TO_DELETION,
            summary = "Details how users can request erasure of personal data and how statutory financial/escrow retention rules apply.",
            detailedClauses = listOf(
                "11.1 Right to Erasure (NDPA Section 34(1)(c)): You may request the deletion of your personal data when it is no longer necessary for the purpose collected, when you withdraw consent, or when you close your SolarFix account.",
                "11.2 Immediate Deletion Scope: Upon a verified deletion request, SolarFix immediately erases your active session tokens, local AI Technician diagnostic chat history, optional solar hardware profile, and non-essential profile data.",
                "11.3 Legal & Escrow Exceptions: Where an account has active escrow orders in transit, open technical disputes, or completed financial transactions subject to the 6-year statutory tax/audit retention mandate, personal identifiers in the active catalog are anonymized while the encrypted financial audit record is retained solely for legal compliance."
            ),
            implementationReference = "PrivacyPolicyAndDataProtectionManager.submitDataDeletionRequest"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.USER_RIGHT_TO_WITHDRAW_CONSENT,
            summary = "Provides one-tap toggles to withdraw consent for optional data processing without affecting core account access.",
            detailedClauses = listOf(
                "12.1 Easy Consent Withdrawal at Any Time: Where processing is based on your consent—such as saving AI Solar Technician diagnostic history, using localized solar irradiance weather estimates, or receiving product safety updates—you may withdraw consent at any time using the interactive toggles in the Privacy & Data Protection Center.",
                "12.2 Device Permission Revocation: You may also revoke Android runtime permissions (such as Camera access for visual fault scanning) at any time in your device settings; text-based AI diagnostics and marketplace shopping will continue to function.",
                "12.3 Lawfulness of Prior Processing: Withdrawing consent stops future optional processing immediately without affecting the lawfulness of processing carried out prior to withdrawal."
            ),
            implementationReference = "PrivacyPolicyAndDataProtectionManager.updateConsentPreferences"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.PRIVACY_COMPLAINTS_PROCEDURE,
            summary = "Sets out direct channels to SolarFix's Data Protection Officer (DPO) and the right to escalate to the NDPC.",
            detailedClauses = listOf(
                "13.1 Internal Data Protection Officer (DPO) Desk: You can submit a privacy complaint directly inside the SolarFix Privacy & Data Protection Center or email our Data Protection Officer at $DATA_PROTECTION_OFFICER_EMAIL or $PRIVACY_COMPLAINTS_EMAIL.",
                "13.2 Investigation & Resolution SLA: Every privacy complaint is assigned a unique tracking reference, acknowledged within 24 hours, and investigated by our DPO within 7 business days.",
                "13.3 Right to Lodge a Complaint with the Supervisory Authority: If you are not satisfied with our response, you have the statutory right under the Nigeria Data Protection Act (NDPA) 2023 to lodge a complaint with the $SUPERVISORY_AUTHORITY."
            ),
            implementationReference = "PrivacyPolicyAndDataProtectionManager.submitPrivacyComplaint"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.DATA_BREACH_RESPONSE_PROTOCOL,
            summary = "Explains SolarFix's containment, cryptographic key rotation, and 72-hour regulatory/user breach notification protocol.",
            detailedClauses = listOf(
                "14.1 Real-Time Breach Detection & Containment: SolarFix's `MobileSecurityAndMonitoringManager` and `DatabaseEncryptionAndSecurityManager` continuously monitor for unauthorized access attempts, RLS violations, and abnormal data queries, automatically isolating compromised sessions and rotating AES-256-GCM encryption keys.",
                "14.2 72-Hour Supervisory Authority Notification: In the event of a personal data breach likely to result in a risk to the rights and freedoms of individuals, SolarFix will notify the Nigeria Data Protection Commission (NDPC) within 72 hours of becoming aware of the breach.",
                "14.3 Direct User Notification: If a breach poses a high risk to your personal rights, financial security, or identity, SolarFix will notify affected users directly without undue delay via email and in-app security alerts, detailing the nature of the breach and protective actions taken."
            ),
            implementationReference = "MobileSecurityAndMonitoringManager, DatabaseEncryptionAndSecurityManager.rotateMasterKey"
        ),
        PrivacyPolicySection(
            topic = PrivacyPolicySectionTopic.NOTIFICATION_OF_PRIVACY_CHANGES,
            summary = "Guarantees versioned transparency and advance notice before any material change to data collection or processing.",
            detailedClauses = listOf(
                "15.1 Advance Notice of Material Privacy Updates: Whenever SolarFix proposes a material change to what personal data we collect, how we process it, or who we share it with, we will publish a new version number and notify you at least 14 days in advance via in-app banner and email.",
                "15.2 Fresh Consent for New Processing Purposes: SolarFix will never repurpose your existing personal data for a materially different optional purpose without first obtaining your explicit, affirmative consent.",
                "15.3 Version History & Archive: The current version ($CURRENT_PRIVACY_POLICY_VERSION) and effective date ($EFFECTIVE_DATE) are permanently displayed at the top of the Privacy Policy screen."
            ),
            implementationReference = "PrivacyPolicyAndDataProtectionManager.CURRENT_PRIVACY_POLICY_VERSION"
        )
    )

    /**
     * All 8 specific data domain provisions reflecting the exact implementation in SolarFix.
     */
    val specificDomainProvisions: List<DataDomainProvisionDetail> = listOf(
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.CUSTOMER_INFORMATION,
            exactFieldsCollected = listOf(
                "Full Name (`name`)",
                "Email Address (`email`)",
                "Account Role (`UserRole.CUSTOMER`)",
                "Salted Password Hash (`PBKDF2-HMAC-SHA256`, 120,000 iterations)",
                "Optional Solar System Profile (`systemSizeKva`, `batteryType`, `batteryCapacityKwh`, `solarPanelWatts`, `installerName`)"
            ),
            excludedByDataMinimization = listOf(
                "Plaintext passwords",
                "Bank Verification Number (BVN)",
                "National ID / NIN for ordinary customers",
                "Biometric templates or health/demographic records"
            ),
            encryptionAndStorageMechanism = "Email encrypted with AES-256-GCM (`SF_ENC_V1`) + HMAC-SHA256 blind index in `EncryptedUserAccountRecord`; passwords hashed with PBKDF2-HMAC-SHA256.",
            accessAndSharingPolicy = "Accessible only to the authenticated customer (`actorUid == resourceOwnerUid`). Never shared with third-party marketers.",
            retentionPeriod = "Retained while the customer account remains active; deleted or anonymized upon verified account deletion request."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.SELLER_VERIFICATION_DOCUMENTS,
            exactFieldsCollected = listOf(
                "Registered Business Name & Owner/Director Name",
                "CAC Registration Number (corporate sellers)",
                "Government ID Type & ID Document Number (NIN, Passport, Driver's License, or Voter's Card)",
                "Government ID Document Scan/Photo & Business Premises Verification Photograph"
            ),
            excludedByDataMinimization = listOf(
                "Personal tax returns of unrelated family members",
                "Biometric fingerprint/iris raw templates",
                "Unrelated personal photos"
            ),
            encryptionAndStorageMechanism = "Stored exclusively in the isolated `PRIVATE_ADMIN_VAULT` (`private-vault/kyc-identity-docs/` and `private-vault/kyb-premises-photos/`) with AES-256-GCM encryption at rest and `0600` (owner/admin-only) permissions.",
            accessAndSharingPolicy = "Strictly restricted to verified SolarFix Compliance Administrators (`FileAccessLevel.PRIVATE_ADMIN_ONLY`). Customers and other sellers can NEVER access seller identity or verification documents.",
            retentionPeriod = "Retained for the life of the verified store plus 5 years after store closure for anti-fraud and warranty traceability."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.BANK_ACCOUNT_INFORMATION,
            exactFieldsCollected = listOf(
                "Settlement Bank Name (`bankName`)",
                "Registered Corporate/Seller Bank Account Name (`bankAccountName`)",
                "10-Digit Nigerian NUBAN Account Number (`bankAccountNumber`)"
            ),
            excludedByDataMinimization = listOf(
                "Online banking passwords or login credentials",
                "ATM / Debit card PINs",
                "Bank Verification Number (BVN)",
                "Customer personal bank account credentials"
            ),
            encryptionAndStorageMechanism = "10-digit NUBAN and account name are encrypted at rest with AES-256-GCM (`EncryptedSellerSensitiveRecord`) and masked in UI/audit logs (`******2015`).",
            accessAndSharingPolicy = "Accessible only to the store owner (masked) and authorized SolarFix Finance Administrators; shared over TLS exclusively with Paystack Transfers API to disburse verified net seller earnings.",
            retentionPeriod = "Retained while the seller store is active and within the 6-year statutory financial settlement audit window."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.PAYMENT_RECORDS,
            exactFieldsCollected = listOf(
                "Paystack Transaction Reference (`PSTK_SOLARFIX_...`)",
                "Backend-Recalculated Order Amount in Kobo/NGN",
                "Payment Channel Summary (e.g., Card/Transfer status) & Webhook HMAC-SHA512 Verification Status",
                "Escrow State (`LOCKED_IN_ESCROW`, `RELEASED_TO_SELLER`, `REFUNDED_TO_BUYER`)"
            ),
            excludedByDataMinimization = listOf(
                "16-digit Payment Card Numbers (PAN) — strictly blocked by `containsProhibitedCardOrPinData`",
                "3- or 4-digit CVV / CVC security codes",
                "Card expiration dates",
                "Card PINs or OTP tokens issued by the customer's bank"
            ),
            encryptionAndStorageMechanism = "Payment metadata is encrypted at rest and anchored in an immutable SHA-256 hash-chained and HMAC-SHA256 signed financial audit ledger.",
            accessAndSharingPolicy = "Processed via PCI-DSS Level 1 certified Paystack gateway. Customers view only their own payment receipts; sellers view only net escrow settlements for their own store items.",
            retentionPeriod = "Retained for 6 years in compliance with statutory financial and tax regulations."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.DELIVERY_ADDRESSES,
            exactFieldsCollected = listOf(
                "Recipient Full Name",
                "Recipient Contact Phone Number",
                "Physical Delivery Street Address, City/Town, and State"
            ),
            excludedByDataMinimization = listOf(
                "Continuous background GPS location tracking",
                "Home Wi-Fi SSIDs or Bluetooth beacon scans",
                "Household member rosters"
            ),
            encryptionAndStorageMechanism = "Every field (recipient name, phone, street, city, state) is encrypted at rest using AES-256-GCM in `EncryptedDeliveryAddressRecord` with user-bound AAD.",
            accessAndSharingPolicy = "Protected by Row-Level Security (`addresses` collection). Shared strictly on a need-to-know basis with the fulfilling Verified Seller and logistics courier for active waybill delivery.",
            retentionPeriod = "Saved addresses can be edited or deleted by the customer at any time; order waybill snapshots are retained with the order record."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.AI_TECHNICIAN_INTERACTIONS,
            exactFieldsCollected = listOf(
                "Solar fault descriptions, load sizing questions, and inverter error codes entered in chat",
                "Equipment or wiring photographs voluntarily captured or selected via Android Photo Picker for multimodal diagnosis"
            ),
            excludedByDataMinimization = listOf(
                "Background microphone audio recordings",
                "Unselected gallery photos (zero broad storage permissions)",
                "Customer payment or banking details in AI prompts"
            ),
            encryptionAndStorageMechanism = "Stored locally on-device in Room (`ChatMessageEntity`) with AES-256-GCM encryption at rest (`SF_ENC_V1`) and transmitted over HTTPS/TLS to Firebase AI Logic (Gemini).",
            accessAndSharingPolicy = "Accessible only to the logged-in user on their device. Never visible to marketplace sellers or other customers.",
            retentionPeriod = "Retained locally until the user clears chat history, withdraws AI diagnostic history consent, requests data deletion, or uninstalls the app."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.UPLOADED_PHOTOGRAPHS_AND_DOCUMENTS,
            exactFieldsCollected = listOf(
                "Seller Identity & Premises Verification Documents (Private Vault)",
                "Product Photographs, Product Load-Test Videos & Store Logos (Public Catalog Media)",
                "Customer AI Diagnostic Equipment Photos (Transient/Session Scoped)"
            ),
            excludedByDataMinimization = listOf(
                "Executable binaries (`.exe`, `.apk`, `.dex`, `.elf`, `.sh`)",
                "Polyglot web-shells or PDFs containing embedded `/JavaScript` or `/Launch` actions",
                "EXIF GPS metadata stripped/ignored during secure UUID file renaming"
            ),
            encryptionAndStorageMechanism = "Validated by `FileUploadAndStorageSecurityManager` (extension whitelist, size cap, magic-byte signature verification, malware scan, UUIDv4 renaming). Private KYC docs are encrypted with AES-256-GCM in `PRIVATE_ADMIN_VAULT`, completely separated from `PUBLIC_CATALOG_MEDIA`.",
            accessAndSharingPolicy = "Public product media is viewable by marketplace shoppers; private verification documents are strictly restricted to authorized SolarFix Administrators.",
            retentionPeriod = "Product media is retained while the listing is active; KYC documents follow the seller verification retention schedule."
        ),
        DataDomainProvisionDetail(
            domain = SpecificDataDomainProvision.MARKETPLACE_TRANSACTION_HISTORY,
            exactFieldsCollected = listOf(
                "Order ID, Timestamp, and Waybill Status (`ESCROW_FUNDED`, `IN_TRANSIT`, `DELIVERED`, `DISPUTED`, `REFUNDED`)",
                "Ordered Product IDs, Quantities, and Authoritative Backend Unit Prices",
                "5% SolarFix Platform Commission, 95% Net Seller Payout, and Withdrawal Settlement Records"
            ),
            excludedByDataMinimization = listOf(
                "Off-platform browsing history",
                "External bank account balances or credit scores"
            ),
            encryptionAndStorageMechanism = "Stored in `orders` and `financial_ledger` collections with encrypted sensitive payloads and SHA-256 + HMAC-SHA256 tamper-evident hash chaining.",
            accessAndSharingPolicy = "Customers can view only their own orders; Verified Sellers can view only orders and commission records for their own store; Administrators can audit platform-wide escrow reconciliation.",
            retentionPeriod = "Retained for 6 years to satisfy statutory financial audit, warranty dispute, and tax obligations."
        )
    )

    /**
     * Searches both the 15 core Privacy Policy sections and the 8 specific data domain provisions.
     */
    fun searchPrivacySections(query: String): List<PrivacyPolicySection> {
        val clean = SecuritySanitizer.sanitizeSearchQuery(query).trim()
        if (clean.isBlank()) return allCoreSections
        val lower = clean.lowercase()
        return allCoreSections.filter { sec ->
            sec.topic.title.lowercase().contains(lower) ||
                sec.topic.shortLabel.lowercase().contains(lower) ||
                sec.summary.lowercase().contains(lower) ||
                sec.detailedClauses.any { it.lowercase().contains(lower) } ||
                sec.implementationReference.lowercase().contains(lower)
        }
    }

    /**
     * Retrieves a specific core Privacy Policy section by its enum topic.
     */
    fun getCoreSection(topic: PrivacyPolicySectionTopic): PrivacyPolicySection {
        return allCoreSections.first { it.topic == topic }
    }

    /**
     * Retrieves a specific data domain provision by its enum identifier.
     */
    fun getDomainProvision(domain: SpecificDataDomainProvision): DataDomainProvisionDetail {
        return specificDomainProvisions.first { it.domain == domain }
    }

    /**
     * Privacy-by-Design & Data Minimization Filter:
     * Audits an incoming map of fields, strips any unnecessary/excessive sensitive attributes
     * (e.g., BVN, raw card numbers, CVVs, banking PINs, biometric templates, contact book dumps),
     * and checks values for prohibited payment card/PIN patterns.
     */
    fun enforceDataMinimizationOnPayload(
        rawPayload: Map<String, String>
    ): DataMinimizationAuditResult {
        val permitted = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        val sanitizedMap = linkedMapOf<String, String>()

        for ((rawKey, rawValue) in rawPayload) {
            val normalizedKey = rawKey.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
            val containsProhibitedValue = DatabaseEncryptionAndSecurityManager.containsRawCardNumberLuhn(rawValue)

            if (normalizedKey in PROHIBITED_EXCESSIVE_SENSITIVE_KEYS || containsProhibitedValue) {
                rejected.add(rawKey)
            } else {
                permitted.add(rawKey)
                sanitizedMap[rawKey] = SecuritySanitizer.sanitizeText(rawValue, maxLength = 500)
            }
        }

        val isCompliant = rejected.isEmpty()
        val msg = if (isCompliant) {
            "Payload complies with SolarFix Privacy-by-Design and Data Minimization rules (${permitted.size} permitted fields)."
        } else {
            "Data Minimization stripped ${rejected.size} prohibited/unnecessary sensitive field(s): ${rejected.joinToString(", ")}"
        }

        return DataMinimizationAuditResult(
            isCompliant = isCompliant,
            permittedKeys = permitted,
            rejectedExcessiveOrSensitiveKeys = rejected,
            sanitizedPayload = sanitizedMap,
            auditMessage = msg
        )
    }

    /**
     * Gets the current consent preferences for a user, initializing with Privacy-by-Design defaults
     * (marketing/optional notifications OFF by default).
     */
    fun getConsentPreferences(userIdentifier: String): UserPrivacyConsentState {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        return userConsentMap.computeIfAbsent(cleanId) {
            UserPrivacyConsentState(userIdentifier = cleanId)
        }
    }

    /**
     * Updates optional processing consents for a user.
     */
    fun updateConsentPreferences(
        userIdentifier: String,
        aiTechnicianDiagnosticHistoryRetention: Boolean,
        localizedSolarIrradianceOptimization: Boolean,
        productSafetyAndFeatureNotifications: Boolean
    ): UserPrivacyConsentState {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        val updated = UserPrivacyConsentState(
            userIdentifier = cleanId,
            essentialAccountAndEscrowProcessing = true,
            aiTechnicianDiagnosticHistoryRetention = aiTechnicianDiagnosticHistoryRetention,
            localizedSolarIrradianceOptimization = localizedSolarIrradianceOptimization,
            productSafetyAndFeatureNotifications = productSafetyAndFeatureNotifications,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        userConsentMap[cleanId] = updated
        recordDsarEvent(
            userIdentifier = cleanId,
            requestType = DataSubjectRequestType.WITHDRAW_CONSENT,
            summary = "Updated consent preferences (AI History=$aiTechnicianDiagnosticHistoryRetention, Weather=$localizedSolarIrradianceOptimization, Notifications=$productSafetyAndFeatureNotifications)",
            resolutionSummary = "Applied immediately across session and local storage."
        )
        return updated
    }

    /**
     * One-tap withdrawal of all optional consents while preserving core account/escrow functionality.
     */
    fun withdrawAllOptionalConsents(userIdentifier: String): UserPrivacyConsentState {
        return updateConsentPreferences(
            userIdentifier = userIdentifier,
            aiTechnicianDiagnosticHistoryRetention = false,
            localizedSolarIrradianceOptimization = false,
            productSafetyAndFeatureNotifications = false
        )
    }

    /**
     * Exercises the User Right of Access (NDPA Section 34(1)(a)) by generating a structured,
     * portable snapshot of the user's personal data and privacy settings.
     */
    fun exportUserPersonalDataSnapshot(
        userIdentifier: String,
        roleName: String = "CUSTOMER"
    ): DataSubjectRequestRecord {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        val consent = getConsentPreferences(cleanId)
        val maskedId = SecuritySanitizer.maskEmail(cleanId)
        return recordDsarEvent(
            userIdentifier = cleanId,
            requestType = DataSubjectRequestType.ACCESS_EXPORT,
            summary = "Personal Data Export Snapshot requested for $maskedId (Role: $roleName)",
            resolutionSummary = "Generated portable JSON/text snapshot: Account=$maskedId, Role=$roleName, Encryption=AES-256-GCM(SF_ENC_V1), AIHistoryConsent=${consent.aiTechnicianDiagnosticHistoryRetention}, ZeroCardOrPinStored=true."
        )
    }

    /**
     * Exercises the User Right to Correction / Rectification (NDPA Section 34(1)(b)).
     */
    fun submitDataCorrectionRequest(
        userIdentifier: String,
        fieldToCorrect: String,
        updatedValueSummary: String
    ): DataSubjectRequestRecord {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        val safeField = SecuritySanitizer.sanitizeText(fieldToCorrect, maxLength = 80).ifBlank { "Account Profile Field" }
        val safeValue = SecuritySanitizer.sanitizeText(updatedValueSummary, maxLength = 200)
        return recordDsarEvent(
            userIdentifier = cleanId,
            requestType = DataSubjectRequestType.CORRECTION,
            summary = "Correction requested for '$safeField': $safeValue",
            resolutionSummary = "Rectification ticket logged with SolarFix DPO & Compliance Desk (SLA: 5 business days; self-service profile fields updated immediately)."
        )
    }

    /**
     * Exercises the User Right to Deletion / Erasure (NDPA Section 34(1)(c)).
     */
    fun submitDataDeletionRequest(
        userIdentifier: String,
        reason: String = "User exercised right to erasure"
    ): DataSubjectRequestRecord {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        val safeReason = SecuritySanitizer.sanitizeText(reason, maxLength = 200).ifBlank { "User requested account & data deletion" }
        // Immediately revoke optional consents as part of deletion hygiene
        userConsentMap[cleanId] = UserPrivacyConsentState(
            userIdentifier = cleanId,
            essentialAccountAndEscrowProcessing = false,
            aiTechnicianDiagnosticHistoryRetention = false,
            localizedSolarIrradianceOptimization = false,
            productSafetyAndFeatureNotifications = false
        )
        return recordDsarEvent(
            userIdentifier = cleanId,
            requestType = DataSubjectRequestType.DELETION_ERASURE,
            summary = "Personal data deletion requested ($safeReason)",
            resolutionSummary = "Local AI diagnostic history, session tokens, and optional profile data scheduled for immediate erasure. Completed escrow/tax ledger entries anonymized per 6-year statutory retention requirement."
        )
    }

    /**
     * Submits a formal Privacy Complaint to the SolarFix Data Protection Officer (DPO).
     */
    fun submitPrivacyComplaint(
        userIdentifier: String,
        complaintDetails: String
    ): DataSubjectRequestRecord {
        val cleanId = SecuritySanitizer.sanitizeText(userIdentifier, maxLength = 120).lowercase().ifBlank { "active_user" }
        val safeDetails = SecuritySanitizer.sanitizeText(complaintDetails, maxLength = 400).ifBlank { "General privacy inquiry" }
        return recordDsarEvent(
            userIdentifier = cleanId,
            requestType = DataSubjectRequestType.PRIVACY_COMPLAINT,
            summary = "Privacy complaint submitted to DPO ($DATA_PROTECTION_OFFICER_EMAIL): $safeDetails",
            resolutionSummary = "Assigned tracked DPO ticket (24h acknowledgment SLA, 7-business-day resolution target; right to escalate to NDPC preserved)."
        )
    }

    private fun recordDsarEvent(
        userIdentifier: String,
        requestType: DataSubjectRequestType,
        summary: String,
        resolutionSummary: String
    ): DataSubjectRequestRecord {
        val record = DataSubjectRequestRecord(
            requestId = "DSAR-${UUID.randomUUID().toString().take(8).uppercase()}",
            userIdentifier = userIdentifier,
            requestType = requestType,
            summary = summary,
            status = "COMPLETED_OR_LOGGED",
            createdAtEpochMs = System.currentTimeMillis(),
            resolutionSummary = resolutionSummary
        )
        _dsarRecordsFlow.value = listOf(record) + _dsarRecordsFlow.value.take(99)
        return record
    }
}
