package com.example.data.payment

/**
 * Supported Nigerian Payment Gateways
 */
enum class PaymentGatewayType(val displayName: String, val providerApiBase: String) {
    PAYSTACK("Paystack Nigeria", "https://api.paystack.co"),
    FLUTTERWAVE("Flutterwave Africa", "https://api.flutterwave.com/v3")
}

/**
 * Status of a Gateway Transaction
 */
enum class GatewayPaymentStatus(val label: String) {
    PENDING("Pending Authorization"),
    INITIALIZED("Gateway Initialized"),
    AWAITING_TRANSFER("Awaiting Bank Transfer"),
    SUCCESS("Verified & Paid"),
    FAILED("Payment Failed"),
    REFUNDED("Refunded"),
    REVERSED("Reversed")
}

/**
 * Dynamic Dedicated Virtual Account Details for Paystack "Pay with Bank Transfer"
 */
data class DedicatedVirtualAccount(
    val bankName: String = "Wema Bank / Providus Bank",
    val accountNumber: String,
    val accountName: String = "SolarFix Escrow / Paystack",
    val reference: String,
    val amountDueNgn: Long,
    val expiresAtTimestamp: String = "30 minutes remaining"
)

/**
 * Paystack Subaccount for Marketplace Split Payments
 */
data class PaystackSubaccount(
    val subaccountCode: String, // e.g. "ACCT_nekes_001"
    val storeId: String,
    val storeName: String,
    val settlementBank: String,
    val accountNumber: String,
    val percentageCharge: Double = 7.5 // SolarFix commission retained
) {
    val businessName: String get() = storeName
}

/**
 * Secure Gateway Initialization Result
 */
data class PaymentInitializationResult(
    val reference: String,
    val accessCode: String,
    val authorizationUrl: String,
    val amountKobo: Long,
    val amountNgn: Long,
    val currency: String = "NGN",
    val gateway: PaymentGatewayType = PaymentGatewayType.PAYSTACK,
    val virtualAccount: DedicatedVirtualAccount? = null,
    val splitSubaccounts: List<PaystackSubaccount> = emptyList()
)

/**
 * Server-Side Payment Verification Response
 * Never trust client success messages; this object is only constructed
 * after server-side cryptographic/API verification against the payment gateway.
 */
data class PaymentVerificationResult(
    val success: Boolean,
    val reference: String,
    val gatewayReference: String,
    val amountKobo: Long,
    val amountNgn: Long,
    val currency: String,
    val channel: String, // "card", "bank_transfer", "ussd"
    val paidAt: String,
    val customerEmail: String,
    val customerName: String,
    val gatewayResponse: String,
    val isSplitApplied: Boolean = false,
    val sellerPayoutNgn: Long = 0L,
    val platformFeeNgn: Long = 0L,
    val reconciliationId: String = "",
    val errorMessage: String? = null
) {
    val isVerified: Boolean get() = success
    val status: GatewayPaymentStatus get() = if (success) GatewayPaymentStatus.SUCCESS else GatewayPaymentStatus.FAILED
    val gatewayId: String get() = gatewayReference
}

/**
 * Payment Webhook Payload for Asynchronous Gateway Notifications
 */
data class PaymentWebhookEvent(
    val eventId: String,
    val event: String, // "charge.success", "transfer.success", "refund.processed"
    val reference: String,
    val amountKobo: Long,
    val currency: String,
    val status: String,
    val channel: String,
    val customerEmail: String,
    val paidAt: String,
    val hmacSignature: String,
    val signatureValid: Boolean = true
)

/**
 * Status of Daily/Monthly Payment Reconciliation
 */
enum class ReconciliationStatus(val label: String) {
    BALANCED("Reconciled & Balanced"),
    DISCREPANCY_DETECTED("Audit Discrepancy Flagged"),
    PENDING_SETTLEMENT("Pending Bank Settlement")
}

/**
 * Payment Reconciliation Record for SolarFix Escrow & Marketplace Audit
 */
data class PaymentReconciliationRecord(
    val reconciliationId: String,
    val dateLabel: String,
    val gateway: PaymentGatewayType = PaymentGatewayType.PAYSTACK,
    val totalTransactionsCount: Int,
    val successfulPaymentsCount: Int,
    val grossCapturedNgn: Long,
    val netSellerSplitPayoutsNgn: Long,
    val solarfixEscrowCommissionsNgn: Long,
    val totalRefundsNgn: Long,
    val unreconciledCount: Int,
    val status: ReconciliationStatus,
    val generatedAt: String
) {
    val reconciliationDate: String get() = dateLabel
    val grossVolumeKobo: Long get() = grossCapturedNgn * 100L
    val netSellerPayoutKobo: Long get() = netSellerSplitPayoutsNgn * 100L
    val solarfixCommissionKobo: Long get() = solarfixEscrowCommissionsNgn * 100L
    val totalRefundsKobo: Long get() = totalRefundsNgn * 100L
    val discrepancyKobo: Long get() = 0L
}

/**
 * Gateway Refund Record
 */
data class GatewayRefundRecord(
    val refundId: String,
    val originalReference: String,
    val orderId: String,
    val amountNgn: Long,
    val storeId: String,
    val storeName: String,
    val customerEmail: String,
    val reason: String,
    val timestamp: String,
    val status: String = "SUCCESS",
    val gatewayResponse: String = "Refund processed successfully via Paystack API"
) {
    val amountKobo: Long get() = amountNgn * 100L
}
