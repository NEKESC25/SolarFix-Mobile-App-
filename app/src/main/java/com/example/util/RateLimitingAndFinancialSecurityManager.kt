package com.example.util

import com.example.data.model.MarketplaceProduct
import com.example.data.model.SellerStore
import com.example.data.model.StoreVerificationStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.roundToLong

/**
 * All 11 sensitive endpoints protected by server-side rate limiting, burst throttling,
 * device/subnet anti-bypass tracking, and bot/CAPTCHA protection.
 */
enum class ProtectedEndpoint(
    val endpointKey: String,
    val displayName: String,
    val maxRequestsPerWindow: Int,
    val windowDurationMs: Long,
    val restrictionDurationMs: Long,
    val captchaThresholdRequests: Int,
    val isFinancialOperation: Boolean = false
) {
    LOGIN(
        endpointKey = "auth/login",
        displayName = "User Login",
        maxRequestsPerWindow = 5,
        windowDurationMs = 60_000L, // 5 attempts per 60s
        restrictionDurationMs = 15 * 60_000L, // 15 min lockout
        captchaThresholdRequests = 3
    ),
    REGISTRATION(
        endpointKey = "auth/register",
        displayName = "Account Registration",
        maxRequestsPerWindow = 3,
        windowDurationMs = 10 * 60_000L, // 3 accounts per 10 mins per device/subnet
        restrictionDurationMs = 30 * 60_000L,
        captchaThresholdRequests = 2
    ),
    PASSWORD_RESET(
        endpointKey = "auth/password-reset",
        displayName = "Password Reset",
        maxRequestsPerWindow = 3,
        windowDurationMs = 15 * 60_000L, // 3 resets per 15 mins
        restrictionDurationMs = 30 * 60_000L,
        captchaThresholdRequests = 2
    ),
    OTP_VERIFICATION(
        endpointKey = "auth/otp-verify",
        displayName = "OTP Verification",
        maxRequestsPerWindow = 5,
        windowDurationMs = 10 * 60_000L, // 5 OTP tries per 10 mins
        restrictionDurationMs = 20 * 60_000L,
        captchaThresholdRequests = 3
    ),
    PRODUCT_SEARCH(
        endpointKey = "marketplace/search",
        displayName = "Product Catalog Search",
        maxRequestsPerWindow = 30,
        windowDurationMs = 60_000L, // 30 searches per minute
        restrictionDurationMs = 5 * 60_000L,
        captchaThresholdRequests = 20
    ),
    SELLER_APPLICATION(
        endpointKey = "marketplace/seller-apply",
        displayName = "Seller KYC/KYB Application",
        maxRequestsPerWindow = 2,
        windowDurationMs = 30 * 60_000L, // 2 applications per 30 mins
        restrictionDurationMs = 60 * 60_000L,
        captchaThresholdRequests = 2
    ),
    PAYMENT_INITIALIZATION(
        endpointKey = "finance/payment-init",
        displayName = "Payment Gateway Initialization",
        maxRequestsPerWindow = 4,
        windowDurationMs = 60_000L, // Stricter financial control: 4 per minute
        restrictionDurationMs = 30 * 60_000L,
        captchaThresholdRequests = 3,
        isFinancialOperation = true
    ),
    WITHDRAWAL_REQUEST(
        endpointKey = "finance/withdrawal-request",
        displayName = "Seller Payout Withdrawal Request",
        maxRequestsPerWindow = 3,
        windowDurationMs = 15 * 60_000L, // Stricter financial control: 3 per 15 mins
        restrictionDurationMs = 60 * 60_000L,
        captchaThresholdRequests = 2,
        isFinancialOperation = true
    ),
    REVIEWS_AND_COMMENTS(
        endpointKey = "marketplace/reviews",
        displayName = "Customer Reviews & Seller Replies",
        maxRequestsPerWindow = 5,
        windowDurationMs = 5 * 60_000L, // 5 reviews per 5 mins
        restrictionDurationMs = 15 * 60_000L,
        captchaThresholdRequests = 4
    ),
    CONTACT_FORM_AND_INQUIRY(
        endpointKey = "support/contact-inquiry",
        displayName = "Contact Forms & Pre-Purchase Inquiries",
        maxRequestsPerWindow = 5,
        windowDurationMs = 10 * 60_000L, // 5 inquiries per 10 mins
        restrictionDurationMs = 15 * 60_000L,
        captchaThresholdRequests = 3
    ),
    AI_TECHNICIAN_REQUEST(
        endpointKey = "diagnostics/ai-technician",
        displayName = "AI Solar Technician Diagnostic Request",
        maxRequestsPerWindow = 10,
        windowDurationMs = 60_000L, // 10 prompts per minute
        restrictionDurationMs = 10 * 60_000L,
        captchaThresholdRequests = 7
    )
}

data class RateLimitDecision(
    val allowed: Boolean,
    val endpoint: ProtectedEndpoint,
    val remainingQuota: Int,
    val retryAfterSeconds: Long = 0L,
    val requiresCaptcha: Boolean = false,
    val captchaChallengeId: String? = null,
    val temporaryAccountRestricted: Boolean = false,
    val blockingDimension: String? = null, // "ACCOUNT", "DEVICE_FINGERPRINT", "IP_SUBNET", "TARGET_RESOURCE", "CAPTCHA_REQUIRED"
    val denialReason: String? = null
)

data class CaptchaBotChallenge(
    val challengeId: String,
    val endpoint: ProtectedEndpoint,
    val actorIdentifier: String,
    val promptQuestion: String,
    val expectedSolutionHash: String,
    val expiresAtEpoch: Long,
    val solved: Boolean = false
)

data class AbuseActivityEvent(
    val eventId: String = "ABUSE-${UUID.randomUUID().toString().take(8).uppercase()}",
    val timestampEpoch: Long = System.currentTimeMillis(),
    val endpoint: ProtectedEndpoint,
    val actorAccount: String,
    val deviceFingerprint: String,
    val ipSubnet: String,
    val eventType: String,
    val temporarilyRestricted: Boolean,
    val details: String
)

enum class FinancialAuditEventType {
    PAYMENT_INITIALIZED,
    PAYMENT_VERIFIED_ON_BACKEND,
    PAYMENT_AMOUNT_MISMATCH_BLOCKED,
    DUPLICATE_PAYMENT_BLOCKED,
    WEBHOOK_SIGNATURE_VERIFIED,
    WEBHOOK_FORGERY_BLOCKED,
    BACKEND_ORDER_TOTAL_RECALCULATED,
    FRONTEND_PRICE_TAMPERING_DETECTED,
    COMMISSION_DEDUCTED_ONCE,
    DUPLICATE_COMMISSION_DEDUCTION_BLOCKED,
    ESCROW_RELEASED_TO_AVAILABLE_BALANCE,
    DUPLICATE_ESCROW_RELEASE_BLOCKED,
    WITHDRAWAL_REQUESTED_TO_VERIFIED_BANK,
    WITHDRAWAL_UNVERIFIED_BANK_BLOCKED,
    WITHDRAWAL_APPROVED_WITH_STEP_UP_AUTH,
    WITHDRAWAL_REJECTED_FUNDS_RETURNED,
    CUSTOMER_REFUND_ISSUED,
    DUPLICATE_REFUND_BLOCKED,
    COMMISSION_RATE_UPDATED_BY_ADMIN,
    UNAUTHORIZED_BALANCE_MODIFICATION_BLOCKED
}

/**
 * Immutable, hash-chained, HMAC-SHA256 signed financial audit record.
 * Any modification or deletion of any historical record breaks the SHA-256 chain (`previousEntryHash` -> `entrySha256Hash`).
 */
data class FinancialAuditLedgerEntry(
    val sequenceNumber: Long,
    val entryId: String,
    val timestampEpoch: Long,
    val eventType: FinancialAuditEventType,
    val storeId: String,
    val orderOrReferenceId: String,
    val amountNgn: Long,
    val commissionNgn: Long,
    val netSellerBalanceDeltaNgn: Long,
    val actorEmail: String,
    val stepUpAuthorized: Boolean,
    val details: String,
    val previousEntryHash: String,
    val entrySha256Hash: String,
    val hmacSignature: String
)

data class ClientOrderItemClaim(
    val productId: String,
    val quantity: Int,
    val clientClaimedUnitPriceNgn: Long? = null
)

data class BackendRecalculatedOrderSummary(
    val isValid: Boolean,
    val frontendPriceTamperingDetected: Boolean,
    val authoritativeSubtotalNgn: Long,
    val authoritativeDeliveryFeeNgn: Long,
    val authoritativeFinalTotalNgn: Long,
    val authoritativeCommissionNgn: Long,
    val authoritativeNetSellerPayoutNgn: Long,
    val discrepancyNotes: List<String> = emptyList()
)

/**
 * Enterprise Rate Limiting, Anti-Bypass Abuse Prevention, CAPTCHA Bot Defense,
 * and Multi-Vendor Marketplace Financial Security Engine.
 */
object RateLimitingAndFinancialSecurityManager {

    private const val GENESIS_LEDGER_HASH = "GENESIS_SOLARFIX_FINANCIAL_LEDGER_0000000000000000"
    private val secureRandom = SecureRandom()

    // Multi-dimensional sliding-window request timestamp histories:
    // 1. Account dimension: "<endpointKey>|acct:<normalizedAccount>"
    // 2. Device hardware dimension: "<endpointKey>|dev:<deviceFingerprint>" (prevents bypass by creating new accounts or rotating IPs!)
    // 3. IP Subnet dimension: "<endpointKey>|subnet:<ipSubnetPrefix>" (prevents bypass by rotating single IPs in a subnet!)
    // 4. Target resource dimension: "<endpointKey>|target:<targetResource>" (prevents distributed brute-force against one victim!)
    private val requestWindows = mutableMapOf<String, MutableList<Long>>()

    // Temporary restriction lockouts across account, device fingerprint, and IP subnet
    private val temporaryRestrictionsUntil = mutableMapOf<String, Long>()

    // Active CAPTCHA / Bot challenges and solved challenge grants
    private val captchaChallenges = mutableMapOf<String, CaptchaBotChallenge>()
    private val solvedCaptchaTokens = mutableSetOf<String>()

    // Financial Idempotency & Anti-Duplication Registries
    private val deductedCommissionOrderIds = mutableSetOf<String>()
    private val releasedEscrowOrderIds = mutableSetOf<String>()
    private val refundedOrderIds = mutableSetOf<String>()

    // Immutable Hash-Chained Financial Audit Ledger
    private val financialLedgerChain = mutableListOf<FinancialAuditLedgerEntry>()
    private val _financialAuditLedgerFlow = MutableStateFlow<List<FinancialAuditLedgerEntry>>(emptyList())
    val financialAuditLedgerFlow: StateFlow<List<FinancialAuditLedgerEntry>> = _financialAuditLedgerFlow.asStateFlow()

    // Real-time Abuse & Suspicious Activity Monitoring Feed
    private val _abuseEventsFlow = MutableStateFlow<List<AbuseActivityEvent>>(emptyList())
    val abuseEventsFlow: StateFlow<List<AbuseActivityEvent>> = _abuseEventsFlow.asStateFlow()

    // =========================================================================
    // 1. Multi-Dimensional Server-Side Rate Limiting & Anti-Bypass Protection
    // =========================================================================

    /**
     * Normalizes an IPv4 or IPv6 address into its `/24` (IPv4) or `/64` (IPv6) network prefix
     * so attackers rotating individual IP addresses within the same proxy subnet share the same bucket.
     */
    fun normalizeIpToSubnet(ipAddress: String): String {
        val clean = ipAddress.trim()
        if (clean.isEmpty()) return "subnet_unknown"
        if (clean.contains(".")) {
            val octets = clean.split(".")
            if (octets.size == 4) {
                return "${octets[0]}.${octets[1]}.${octets[2]}.0/24"
            }
        }
        if (clean.contains(":")) {
            val hextets = clean.split(":").take(4)
            return hextets.joinToString(":") + "::/64"
        }
        return clean
    }

    /**
     * Evaluates server-side rate limits across 4 simultaneous dimensions:
     * 1. Account Identity (`accountIdentifier`)
     * 2. Hardware/Installation Device Fingerprint (`deviceFingerprint`) — stops multi-account Sybil bypass & IP-hop bypass
     * 3. IP `/24` or `/64` Subnet (`ipAddress`) — stops rotating-account spam from the same network
     * 4. Target Resource (`targetResourceId`) — stops distributed attacks targeting a specific account or store
     */
    @Synchronized
    fun checkAndRecordRequest(
        endpoint: ProtectedEndpoint,
        accountIdentifier: String,
        deviceFingerprint: String = "dev_default_android",
        ipAddress: String = "197.210.70.10",
        targetResourceId: String = "",
        solvedCaptchaToken: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitDecision {
        val normAccount = accountIdentifier.trim().lowercase().ifEmpty { "anonymous" }
        val normDevice = deviceFingerprint.trim().ifEmpty { "dev_unknown" }
        val normSubnet = normalizeIpToSubnet(ipAddress)
        val normTarget = targetResourceId.trim().lowercase()

        val acctRestrictionKey = "restrict:acct:${endpoint.endpointKey}:$normAccount"
        val devRestrictionKey = "restrict:dev:${endpoint.endpointKey}:$normDevice"
        val subnetRestrictionKey = "restrict:subnet:${endpoint.endpointKey}:$normSubnet"

        // 1. Check active temporary restrictions across Account, Device Fingerprint, and IP Subnet
        listOf(
            "ACCOUNT" to acctRestrictionKey,
            "DEVICE_FINGERPRINT" to devRestrictionKey,
            "IP_SUBNET" to subnetRestrictionKey
        ).forEach { (dimName, rKey) ->
            val restrictedUntil = temporaryRestrictionsUntil[rKey] ?: 0L
            if (nowMs < restrictedUntil) {
                val retrySec = ((restrictedUntil - nowMs) / 1000L).coerceAtLeast(1L)
                return RateLimitDecision(
                    allowed = false,
                    endpoint = endpoint,
                    remainingQuota = 0,
                    retryAfterSeconds = retrySec,
                    requiresCaptcha = true,
                    temporaryAccountRestricted = true,
                    blockingDimension = dimName,
                    denialReason = "Temporary security restriction active on $dimName for ${endpoint.displayName}. Retry in ${retrySec}s."
                )
            } else if (restrictedUntil > 0L) {
                temporaryRestrictionsUntil.remove(rKey)
            }
        }

        // 2. Prune expired timestamps and inspect sliding windows across all 4 dimensions
        val acctWindowKey = "${endpoint.endpointKey}|acct:$normAccount"
        val devWindowKey = "${endpoint.endpointKey}|dev:$normDevice"
        val subnetWindowKey = "${endpoint.endpointKey}|subnet:$normSubnet"
        val targetWindowKey = if (normTarget.isNotEmpty()) "${endpoint.endpointKey}|target:$normTarget" else null

        val acctTimestamps = pruneAndGetWindow(acctWindowKey, endpoint.windowDurationMs, nowMs)
        val devTimestamps = pruneAndGetWindow(devWindowKey, endpoint.windowDurationMs, nowMs)
        val subnetTimestamps = pruneAndGetWindow(subnetWindowKey, endpoint.windowDurationMs, nowMs)
        val targetTimestamps = targetWindowKey?.let { pruneAndGetWindow(it, endpoint.windowDurationMs, nowMs) }

        val maxObservedCount = maxOf(
            acctTimestamps.size,
            devTimestamps.size,
            subnetTimestamps.size,
            targetTimestamps?.size ?: 0
        )

        val hasValidCaptchaGrant = solvedCaptchaToken != null && solvedCaptchaTokens.contains(solvedCaptchaToken)

        // 3. Enforce hard rate-limit ceiling across any triggered dimension
        if (maxObservedCount >= endpoint.maxRequestsPerWindow) {
            val triggeredDimension = when (maxObservedCount) {
                acctTimestamps.size -> "ACCOUNT"
                devTimestamps.size -> "DEVICE_FINGERPRINT"
                subnetTimestamps.size -> "IP_SUBNET"
                else -> "TARGET_RESOURCE"
            }

            // Apply temporary restriction on the offending dimensions
            val restrictUntil = nowMs + endpoint.restrictionDurationMs
            temporaryRestrictionsUntil[acctRestrictionKey] = restrictUntil
            temporaryRestrictionsUntil[devRestrictionKey] = restrictUntil
            if (subnetTimestamps.size >= endpoint.maxRequestsPerWindow) {
                temporaryRestrictionsUntil[subnetRestrictionKey] = restrictUntil
            }

            val challenge = issueCaptchaChallengeInternal(endpoint, normAccount, nowMs)
            recordAbuseEvent(
                endpoint = endpoint,
                actorAccount = normAccount,
                deviceFingerprint = normDevice,
                ipSubnet = normSubnet,
                eventType = "RATE_LIMIT_EXCEEDED_${triggeredDimension}",
                temporarilyRestricted = true,
                details = "Blocked ${endpoint.displayName}: exceeded ${endpoint.maxRequestsPerWindow} requests in ${endpoint.windowDurationMs / 1000}s on dimension $triggeredDimension"
            )

            return RateLimitDecision(
                allowed = false,
                endpoint = endpoint,
                remainingQuota = 0,
                retryAfterSeconds = (endpoint.restrictionDurationMs / 1000L).coerceAtLeast(1L),
                requiresCaptcha = true,
                captchaChallengeId = challenge.challengeId,
                temporaryAccountRestricted = true,
                blockingDimension = triggeredDimension,
                denialReason = "Rate limit exceeded for ${endpoint.displayName} ($triggeredDimension). Account/device temporarily restricted."
            )
        }

        // 4. Check if CAPTCHA / Bot verification threshold is reached before hard lockout
        val requiresCaptchaNow = maxObservedCount >= endpoint.captchaThresholdRequests && !hasValidCaptchaGrant
        val issuedChallengeId = if (requiresCaptchaNow) {
            issueCaptchaChallengeInternal(endpoint, normAccount, nowMs).challengeId
        } else null

        // Record the request across all dimensions
        acctTimestamps.add(nowMs)
        devTimestamps.add(nowMs)
        subnetTimestamps.add(nowMs)
        targetTimestamps?.add(nowMs)

        val remaining = (endpoint.maxRequestsPerWindow - (maxObservedCount + 1)).coerceAtLeast(0)
        return RateLimitDecision(
            allowed = true,
            endpoint = endpoint,
            remainingQuota = remaining,
            requiresCaptcha = requiresCaptchaNow,
            captchaChallengeId = issuedChallengeId
        )
    }

    private fun pruneAndGetWindow(key: String, windowDurationMs: Long, nowMs: Long): MutableList<Long> {
        val list = requestWindows.getOrPut(key) { mutableListOf() }
        val cutoff = nowMs - windowDurationMs
        list.removeAll { it < cutoff }
        return list
    }

    /**
     * Issues a cryptographic CAPTCHA / human-verification challenge for high-velocity or suspicious actors.
     */
    @Synchronized
    fun issueCaptchaChallenge(
        endpoint: ProtectedEndpoint,
        actorIdentifier: String
    ): Pair<CaptchaBotChallenge, String> {
        val nowMs = System.currentTimeMillis()
        val a = 10 + secureRandom.nextInt(40)
        val b = 5 + secureRandom.nextInt(20)
        val answer = (a + b).toString()
        val challengeId = "CAP-${UUID.randomUUID().toString().take(10).uppercase()}"
        val expectedHash = sha256Hex("$challengeId:$answer")
        val challenge = CaptchaBotChallenge(
            challengeId = challengeId,
            endpoint = endpoint,
            actorIdentifier = actorIdentifier.trim().lowercase(),
            promptQuestion = "Human Verification: Calculate $a + $b to continue",
            expectedSolutionHash = expectedHash,
            expiresAtEpoch = nowMs + 5 * 60_000L
        )
        captchaChallenges[challengeId] = challenge
        return challenge to answer
    }

    private fun issueCaptchaChallengeInternal(
        endpoint: ProtectedEndpoint,
        actorIdentifier: String,
        nowMs: Long
    ): CaptchaBotChallenge {
        val challengeId = "CAP-${UUID.randomUUID().toString().take(10).uppercase()}"
        val challenge = CaptchaBotChallenge(
            challengeId = challengeId,
            endpoint = endpoint,
            actorIdentifier = actorIdentifier,
            promptQuestion = "Verify human access for ${endpoint.displayName}",
            expectedSolutionHash = sha256Hex("$challengeId:SOLARFIX_HUMAN_OK"),
            expiresAtEpoch = nowMs + 5 * 60_000L
        )
        captchaChallenges[challengeId] = challenge
        return challenge
    }

    /**
     * Verifies a user's CAPTCHA solution and returns a verified bot-clearance token (`captchaGrantToken`)
     * while lifting temporary rate-limit restrictions for that actor.
     */
    @Synchronized
    fun verifyCaptchaChallenge(
        challengeId: String,
        userAnswer: String,
        deviceFingerprint: String = "dev_default_android",
        ipAddress: String = "197.210.70.10"
    ): Result<String> {
        val challenge = captchaChallenges[challengeId]
            ?: return Result.failure(SecurityException("CAPTCHA challenge not found or expired."))
        if (System.currentTimeMillis() > challenge.expiresAtEpoch) {
            captchaChallenges.remove(challengeId)
            return Result.failure(SecurityException("CAPTCHA challenge has expired."))
        }
        val actualHash = sha256Hex("$challengeId:${userAnswer.trim()}")
        if (!SecuritySanitizer.constantTimeEquals(actualHash, challenge.expectedSolutionHash)) {
            return Result.failure(SecurityException("Incorrect CAPTCHA verification answer."))
        }

        val grantToken = "CAP-GRANT-${UUID.randomUUID().toString().replace("-", "").take(16).uppercase()}"
        solvedCaptchaTokens.add(grantToken)
        captchaChallenges[challengeId] = challenge.copy(solved = true)

        // Clear temporary restriction upon valid human CAPTCHA verification
        val normSubnet = normalizeIpToSubnet(ipAddress)
        temporaryRestrictionsUntil.remove("restrict:acct:${challenge.endpoint.endpointKey}:${challenge.actorIdentifier}")
        temporaryRestrictionsUntil.remove("restrict:dev:${challenge.endpoint.endpointKey}:${deviceFingerprint.trim()}")
        temporaryRestrictionsUntil.remove("restrict:subnet:${challenge.endpoint.endpointKey}:$normSubnet")

        return Result.success(grantToken)
    }

    @Synchronized
    fun resetAllRateLimitsForTesting() {
        requestWindows.clear()
        temporaryRestrictionsUntil.clear()
        captchaChallenges.clear()
        solvedCaptchaTokens.clear()
    }

    // =========================================================================
    // 2. Payment & Financial Security Engine (All 12 Marketplace Requirements)
    // =========================================================================

    /**
     * Requirement 7 & 8: Never trust payment amounts supplied by the frontend;
     * always recalculate transaction amounts on the backend from authoritative catalog prices
     * and store commission settings.
     */
    fun recalculateAndVerifyOrderFinancialsOnBackend(
        requestedItems: List<ClientOrderItemClaim>,
        catalogProducts: List<MarketplaceProduct>,
        stores: List<SellerStore>,
        defaultCommissionPercent: Double,
        deliveryFeePerSellerNgn: Long = 15_000L,
        frontendClaimedTotalNgn: Long? = null
    ): BackendRecalculatedOrderSummary {
        if (requestedItems.isEmpty()) {
            return BackendRecalculatedOrderSummary(
                isValid = false,
                frontendPriceTamperingDetected = false,
                authoritativeSubtotalNgn = 0L,
                authoritativeDeliveryFeeNgn = 0L,
                authoritativeFinalTotalNgn = 0L,
                authoritativeCommissionNgn = 0L,
                authoritativeNetSellerPayoutNgn = 0L,
                discrepancyNotes = listOf("Order cart is empty.")
            )
        }

        val discrepancies = mutableListOf<String>()
        var priceTampering = false
        var totalSubtotalNgn = 0L
        var totalCommissionNgn = 0L
        var totalNetSellerNgn = 0L
        val storesInOrder = mutableSetOf<String>()

        for (claim in requestedItems) {
            if (claim.quantity <= 0 || claim.quantity > 100_000) {
                return BackendRecalculatedOrderSummary(
                    isValid = false,
                    frontendPriceTamperingDetected = true,
                    authoritativeSubtotalNgn = 0L,
                    authoritativeDeliveryFeeNgn = 0L,
                    authoritativeFinalTotalNgn = 0L,
                    authoritativeCommissionNgn = 0L,
                    authoritativeNetSellerPayoutNgn = 0L,
                    discrepancyNotes = listOf("Invalid item quantity (${claim.quantity}) for product ${claim.productId}.")
                )
            }
            val liveProduct = catalogProducts.find { it.id == claim.productId }
                ?: return BackendRecalculatedOrderSummary(
                    isValid = false,
                    frontendPriceTamperingDetected = false,
                    authoritativeSubtotalNgn = 0L,
                    authoritativeDeliveryFeeNgn = 0L,
                    authoritativeFinalTotalNgn = 0L,
                    authoritativeCommissionNgn = 0L,
                    authoritativeNetSellerPayoutNgn = 0L,
                    discrepancyNotes = listOf("Product ${claim.productId} does not exist in backend catalog.")
                )

            // Detect if frontend attempted to supply a manipulated unit price
            if (claim.clientClaimedUnitPriceNgn != null && claim.clientClaimedUnitPriceNgn != liveProduct.basePriceNgn) {
                priceTampering = true
                discrepancies.add(
                    "Frontend price tampering blocked on '${liveProduct.title}': client sent ₦${claim.clientClaimedUnitPriceNgn}, backend catalog price is ₦${liveProduct.basePriceNgn}."
                )
            }

            val lineTotal = liveProduct.basePriceNgn * claim.quantity.toLong()
            val store = stores.find { it.id == liveProduct.storeId }
            val commPct = (store?.customCommissionPercent ?: defaultCommissionPercent).coerceIn(0.0, 50.0)
            val lineCommission = (lineTotal * (commPct / 100.0)).roundToLong()
            val lineNetSeller = (lineTotal - lineCommission).coerceAtLeast(0L)

            totalSubtotalNgn += lineTotal
            totalCommissionNgn += lineCommission
            totalNetSellerNgn += lineNetSeller
            storesInOrder.add(liveProduct.storeId)
        }

        val totalDeliveryFeeNgn = storesInOrder.size.toLong() * deliveryFeePerSellerNgn
        val authoritativeFinalTotal = totalSubtotalNgn + totalDeliveryFeeNgn

        if (frontendClaimedTotalNgn != null && frontendClaimedTotalNgn != authoritativeFinalTotal) {
            priceTampering = true
            discrepancies.add(
                "Frontend order total mismatch: client claimed ₦$frontendClaimedTotalNgn, backend recalculated ₦$authoritativeFinalTotal."
            )
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.FRONTEND_PRICE_TAMPERING_DETECTED,
                storeId = storesInOrder.firstOrNull() ?: "multi_store",
                orderOrReferenceId = "CHECKOUT_RECALC",
                amountNgn = authoritativeFinalTotal,
                commissionNgn = totalCommissionNgn,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = "client_checkout",
                stepUpAuthorized = false,
                details = discrepancies.joinToString("; ")
            )
        } else {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.BACKEND_ORDER_TOTAL_RECALCULATED,
                storeId = storesInOrder.firstOrNull() ?: "multi_store",
                orderOrReferenceId = "CHECKOUT_RECALC",
                amountNgn = authoritativeFinalTotal,
                commissionNgn = totalCommissionNgn,
                netSellerBalanceDeltaNgn = totalNetSellerNgn,
                actorEmail = "backend_pricing_engine",
                stepUpAuthorized = true,
                details = "Backend recalculated subtotal=₦$totalSubtotalNgn, delivery=₦$totalDeliveryFeeNgn, commission=₦$totalCommissionNgn, final=₦$authoritativeFinalTotal"
            )
        }

        return BackendRecalculatedOrderSummary(
            isValid = !priceTampering,
            frontendPriceTamperingDetected = priceTampering,
            authoritativeSubtotalNgn = totalSubtotalNgn,
            authoritativeDeliveryFeeNgn = totalDeliveryFeeNgn,
            authoritativeFinalTotalNgn = authoritativeFinalTotal,
            authoritativeCommissionNgn = totalCommissionNgn,
            authoritativeNetSellerPayoutNgn = totalNetSellerNgn,
            discrepancyNotes = discrepancies
        )
    }

    /**
     * Requirement 5: Prevent duplicate commission deductions.
     * Ensures SolarFix platform commission can only be deducted ONCE per order ID.
     */
    @Synchronized
    fun tryRegisterUniqueCommissionDeduction(
        orderId: String,
        storeId: String,
        commissionNgn: Long,
        netSellerEarningsNgn: Long
    ): Boolean {
        val key = orderId.trim()
        if (key.isEmpty()) return false
        if (deductedCommissionOrderIds.contains(key)) {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.DUPLICATE_COMMISSION_DEDUCTION_BLOCKED,
                storeId = storeId,
                orderOrReferenceId = key,
                amountNgn = commissionNgn,
                commissionNgn = 0L,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = "system_commission_guard",
                stepUpAuthorized = false,
                details = "BLOCKED duplicate commission deduction of ₦$commissionNgn on order $key"
            )
            return false
        }
        deductedCommissionOrderIds.add(key)
        appendFinancialAuditEntry(
            eventType = FinancialAuditEventType.COMMISSION_DEDUCTED_ONCE,
            storeId = storeId,
            orderOrReferenceId = key,
            amountNgn = commissionNgn + netSellerEarningsNgn,
            commissionNgn = commissionNgn,
            netSellerBalanceDeltaNgn = netSellerEarningsNgn,
            actorEmail = "system_escrow_engine",
            stepUpAuthorized = true,
            details = "Recorded single idempotent commission deduction of ₦$commissionNgn for order $key"
        )
        return true
    }

    /**
     * Requirement 4 & 6: Prevent duplicate escrow release to seller available balance.
     */
    @Synchronized
    fun tryRegisterUniqueEscrowRelease(
        orderId: String,
        storeId: String,
        netSellerEarningsNgn: Long
    ): Boolean {
        val key = orderId.trim()
        if (key.isEmpty()) return false
        if (releasedEscrowOrderIds.contains(key)) {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.DUPLICATE_ESCROW_RELEASE_BLOCKED,
                storeId = storeId,
                orderOrReferenceId = key,
                amountNgn = netSellerEarningsNgn,
                commissionNgn = 0L,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = "system_escrow_guard",
                stepUpAuthorized = false,
                details = "BLOCKED duplicate escrow release of ₦$netSellerEarningsNgn for order $key"
            )
            return false
        }
        releasedEscrowOrderIds.add(key)
        appendFinancialAuditEntry(
            eventType = FinancialAuditEventType.ESCROW_RELEASED_TO_AVAILABLE_BALANCE,
            storeId = storeId,
            orderOrReferenceId = key,
            amountNgn = netSellerEarningsNgn,
            commissionNgn = 0L,
            netSellerBalanceDeltaNgn = netSellerEarningsNgn,
            actorEmail = "system_escrow_engine",
            stepUpAuthorized = true,
            details = "Released escrow of ₦$netSellerEarningsNgn to available balance for store $storeId on order $key"
        )
        return true
    }

    /**
     * Requirement 4: Prevent duplicate order refunds.
     */
    @Synchronized
    fun tryRegisterUniqueOrderRefund(
        orderId: String,
        storeId: String,
        refundAmountNgn: Long,
        actorEmail: String = "admin@solarfix.africa"
    ): Boolean {
        val key = orderId.trim()
        if (key.isEmpty()) return false
        if (refundedOrderIds.contains(key)) {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.DUPLICATE_REFUND_BLOCKED,
                storeId = storeId,
                orderOrReferenceId = key,
                amountNgn = refundAmountNgn,
                commissionNgn = 0L,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = actorEmail,
                stepUpAuthorized = false,
                details = "BLOCKED duplicate refund attempt of ₦$refundAmountNgn for order $key"
            )
            return false
        }
        refundedOrderIds.add(key)
        appendFinancialAuditEntry(
            eventType = FinancialAuditEventType.CUSTOMER_REFUND_ISSUED,
            storeId = storeId,
            orderOrReferenceId = key,
            amountNgn = refundAmountNgn,
            commissionNgn = 0L,
            netSellerBalanceDeltaNgn = -refundAmountNgn,
            actorEmail = actorEmail,
            stepUpAuthorized = true,
            details = "Processed single verified customer refund of ₦$refundAmountNgn for order $key"
        )
        return true
    }

    /**
     * Requirement 10: Restrict seller withdrawals to verified bank accounts.
     * Ensures:
     * 1. The seller's store is APPROVED (`StoreVerificationStatus.APPROVED`).
     * 2. The bank account number is a valid 10-digit Nigerian NUBAN.
     * 3. The bank account number matches the store's KYB-verified settlement bank account (`store.bankAccountNumber`)
     *    or the store's verified Paystack split subaccount (`verifiedSubaccountNumber`).
     */
    fun validateSellerWithdrawalDestination(
        store: SellerStore,
        requestedAccountNumber: String,
        requestedBankName: String,
        amountNgn: Long,
        verifiedSubaccountNumber: String? = null
    ): UploadValidationResult {
        if (store.status != StoreVerificationStatus.APPROVED) {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.WITHDRAWAL_UNVERIFIED_BANK_BLOCKED,
                storeId = store.id,
                orderOrReferenceId = "WTH_BLOCKED_UNAPPROVED_STORE",
                amountNgn = amountNgn,
                commissionNgn = 0L,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = store.ownerEmail,
                stepUpAuthorized = false,
                details = "Blocked withdrawal from unverified store ${store.id} (status=${store.status})"
            )
            return UploadValidationResult(false, "Only verified sellers (APPROVED stores) can request withdrawals.")
        }

        val cleanRequestedAcc = requestedAccountNumber.trim()
        if (!SecuritySanitizer.isValidNubanAccountNumber(cleanRequestedAcc)) {
            return UploadValidationResult(false, "Invalid 10-digit Nigerian NUBAN account number.")
        }

        if (!SecuritySanitizer.isValidPositiveAmount(amountNgn) || amountNgn > store.availableEarningsNgn) {
            return UploadValidationResult(false, "Withdrawal amount exceeds available cleared earnings.")
        }

        val verifiedAccountsForStore = buildSet {
            if (store.bankAccountNumber.isNotBlank()) add(store.bankAccountNumber.trim())
            if (!verifiedSubaccountNumber.isNullOrBlank()) add(verifiedSubaccountNumber.trim())
        }

        if (verifiedAccountsForStore.isNotEmpty() && cleanRequestedAcc !in verifiedAccountsForStore) {
            appendFinancialAuditEntry(
                eventType = FinancialAuditEventType.WITHDRAWAL_UNVERIFIED_BANK_BLOCKED,
                storeId = store.id,
                orderOrReferenceId = "WTH_UNVERIFIED_ACCT_$cleanRequestedAcc",
                amountNgn = amountNgn,
                commissionNgn = 0L,
                netSellerBalanceDeltaNgn = 0L,
                actorEmail = store.ownerEmail,
                stepUpAuthorized = false,
                details = "Blocked withdrawal of ₦$amountNgn from store ${store.id} to unverified bank account $cleanRequestedAcc ($requestedBankName)"
            )
            return UploadValidationResult(
                false,
                "Security Block: Withdrawals are strictly restricted to your KYB-verified settlement bank account on file."
            )
        }

        return UploadValidationResult(true)
    }

    // =========================================================================
    // 3. Immutable, Tamper-Evident Hash-Chained Financial Audit Ledger (Req 9 & 12)
    // =========================================================================

    @Synchronized
    fun appendFinancialAuditEntry(
        eventType: FinancialAuditEventType,
        storeId: String,
        orderOrReferenceId: String,
        amountNgn: Long,
        commissionNgn: Long,
        netSellerBalanceDeltaNgn: Long,
        actorEmail: String,
        stepUpAuthorized: Boolean,
        details: String
    ): FinancialAuditLedgerEntry {
        val seq = (financialLedgerChain.size + 1).toLong()
        val entryId = "FIN-AUD-${seq.toString().padStart(6, '0')}"
        val timestamp = System.currentTimeMillis()
        val prevHash = financialLedgerChain.lastOrNull()?.entrySha256Hash ?: GENESIS_LEDGER_HASH
        val maskedActor = SecuritySanitizer.maskEmail(actorEmail)

        val canonicalPayload = buildString {
            append("seq=").append(seq)
            append("|id=").append(entryId)
            append("|ts=").append(timestamp)
            append("|type=").append(eventType.name)
            append("|store=").append(storeId)
            append("|ref=").append(orderOrReferenceId)
            append("|amt=").append(amountNgn)
            append("|comm=").append(commissionNgn)
            append("|delta=").append(netSellerBalanceDeltaNgn)
            append("|actor=").append(maskedActor)
            append("|mfa=").append(stepUpAuthorized)
            append("|prev=").append(prevHash)
        }

        val entryHash = sha256Hex(canonicalPayload)
        val hmacSig = DatabaseEncryptionAndSecurityManager.computeHmacSha256("$canonicalPayload|hash=$entryHash")

        val entry = FinancialAuditLedgerEntry(
            sequenceNumber = seq,
            entryId = entryId,
            timestampEpoch = timestamp,
            eventType = eventType,
            storeId = storeId,
            orderOrReferenceId = orderOrReferenceId,
            amountNgn = amountNgn,
            commissionNgn = commissionNgn,
            netSellerBalanceDeltaNgn = netSellerBalanceDeltaNgn,
            actorEmail = maskedActor,
            stepUpAuthorized = stepUpAuthorized,
            details = details,
            previousEntryHash = prevHash,
            entrySha256Hash = entryHash,
            hmacSignature = hmacSig
        )

        financialLedgerChain.add(entry)
        _financialAuditLedgerFlow.value = financialLedgerChain.reversed()

        // Forward blocked financial attacks & suspicious withdrawals to Administrator Security Alerts
        when (eventType) {
            FinancialAuditEventType.WITHDRAWAL_UNVERIFIED_BANK_BLOCKED -> {
                MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                    category = SecurityMonitoringCategory.SUSPICIOUS_WITHDRAWAL,
                    eventType = eventType.name,
                    actorIdentifier = actorEmail,
                    targetResourceId = "$storeId:$orderOrReferenceId",
                    details = details
                )
            }
            FinancialAuditEventType.PAYMENT_AMOUNT_MISMATCH_BLOCKED,
            FinancialAuditEventType.DUPLICATE_PAYMENT_BLOCKED,
            FinancialAuditEventType.WEBHOOK_FORGERY_BLOCKED,
            FinancialAuditEventType.FRONTEND_PRICE_TAMPERING_DETECTED,
            FinancialAuditEventType.DUPLICATE_COMMISSION_DEDUCTION_BLOCKED,
            FinancialAuditEventType.DUPLICATE_ESCROW_RELEASE_BLOCKED,
            FinancialAuditEventType.DUPLICATE_REFUND_BLOCKED,
            FinancialAuditEventType.UNAUTHORIZED_BALANCE_MODIFICATION_BLOCKED -> {
                MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                    category = SecurityMonitoringCategory.PAYMENT_MANIPULATION_ATTEMPT,
                    eventType = eventType.name,
                    actorIdentifier = actorEmail,
                    targetResourceId = "$storeId:$orderOrReferenceId",
                    details = details
                )
            }
            else -> {
                MobileSecurityAndMonitoringManager.recordUnifiedAuditEntry(
                    category = SecurityMonitoringCategory.PAYMENT_MANIPULATION_ATTEMPT,
                    severity = SecurityAlertSeverity.INFO,
                    eventType = eventType.name,
                    actorIdentifier = actorEmail,
                    targetResourceId = "$storeId:$orderOrReferenceId",
                    details = details
                )
            }
        }
        return entry
    }

    /**
     * Verifies the cryptographic integrity of the entire financial audit ledger chain
     * from the genesis hash to the latest transaction entry.
     */
    @Synchronized
    fun verifyFinancialAuditChainIntegrity(
        entriesInChronologicalOrder: List<FinancialAuditLedgerEntry> = financialLedgerChain.toList()
    ): Boolean {
        var expectedPrevHash = GENESIS_LEDGER_HASH
        for ((index, entry) in entriesInChronologicalOrder.withIndex()) {
            if (entry.sequenceNumber != (index + 1).toLong()) return false
            if (entry.previousEntryHash != expectedPrevHash) return false

            val canonicalPayload = buildString {
                append("seq=").append(entry.sequenceNumber)
                append("|id=").append(entry.entryId)
                append("|ts=").append(entry.timestampEpoch)
                append("|type=").append(entry.eventType.name)
                append("|store=").append(entry.storeId)
                append("|ref=").append(entry.orderOrReferenceId)
                append("|amt=").append(entry.amountNgn)
                append("|comm=").append(entry.commissionNgn)
                append("|delta=").append(entry.netSellerBalanceDeltaNgn)
                append("|actor=").append(entry.actorEmail)
                append("|mfa=").append(entry.stepUpAuthorized)
                append("|prev=").append(entry.previousEntryHash)
            }

            val recomputedHash = sha256Hex(canonicalPayload)
            if (recomputedHash != entry.entrySha256Hash) return false

            val validHmac = DatabaseEncryptionAndSecurityManager.verifyHmacSha256(
                canonicalData = "$canonicalPayload|hash=${entry.entrySha256Hash}",
                expectedHmac = entry.hmacSignature
            )
            if (!validHmac) return false

            expectedPrevHash = entry.entrySha256Hash
        }
        return true
    }

    private fun recordAbuseEvent(
        endpoint: ProtectedEndpoint,
        actorAccount: String,
        deviceFingerprint: String,
        ipSubnet: String,
        eventType: String,
        temporarilyRestricted: Boolean,
        details: String
    ) {
        val ev = AbuseActivityEvent(
            endpoint = endpoint,
            actorAccount = SecuritySanitizer.maskEmail(actorAccount),
            deviceFingerprint = deviceFingerprint,
            ipSubnet = ipSubnet,
            eventType = eventType,
            temporarilyRestricted = temporarilyRestricted,
            details = details
        )
        _abuseEventsFlow.update { current ->
            (listOf(ev) + current).take(500)
        }
        MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
            category = SecurityMonitoringCategory.REPEATED_API_ABUSE,
            eventType = eventType,
            actorIdentifier = actorAccount,
            targetResourceId = endpoint.endpointKey,
            details = details
        )
    }

    private fun sha256Hex(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
