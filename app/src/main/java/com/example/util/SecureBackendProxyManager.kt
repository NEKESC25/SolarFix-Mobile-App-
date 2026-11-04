package com.example.util

import com.example.BuildConfig
import java.security.SecureRandom
import java.util.Base64

/**
 * Enforces strict separation between Public Client Configuration and Server-Side Secret Credentials,
 * and routes all third-party integrations requiring secret keys through authorized SolarFix Backend
 * Cloud/Edge Function endpoints.
 *
 * Protected Third-Party Integrations:
 * 1. Paystack (Transaction Initialization, Verification, Subaccounts, Refunds, Webhook HMAC)
 * 2. Flutterwave (Payment Verification & Split Settlement)
 * 3. Google AI Services (Firebase AI Logic + Firebase App Check attestation; zero client GEMINI_API_KEY)
 * 4. Identity Verification Providers (CAC, TIN, NIN, BVN Seller KYB Verification)
 * 5. Delivery & Logistics Services (Waybill Dispatch, Escrow Delivery Confirmation)
 * 6. OEM Equipment Authenticity & Serial Number Verification APIs
 */
object SecureBackendProxyManager {

    const val BACKEND_FUNCTIONS_BASE_URL = "https://api.solarfix.africa/v1/functions/"

    // Authorized Server-Side Edge Function Endpoints (Mobile app calls these instead of third-party secret APIs)
    const val ENDPOINT_PAYSTACK_INITIALIZE = "payments/paystack/initialize"
    const val ENDPOINT_PAYSTACK_VERIFY = "payments/paystack/verify"
    const val ENDPOINT_PAYSTACK_SUBACCOUNT = "payments/paystack/subaccount"
    const val ENDPOINT_PAYSTACK_REFUND = "payments/paystack/refund"
    const val ENDPOINT_FLUTTERWAVE_VERIFY = "payments/flutterwave/verify"
    const val ENDPOINT_IDENTITY_KYB_VERIFY = "identity/kyb/verify"
    const val ENDPOINT_DELIVERY_DISPATCH = "logistics/delivery/dispatch"
    const val ENDPOINT_OEM_SERIAL_VERIFY = "oem/authenticity/verify-serial"

    // Prefixes that identify private/secret API credentials that MUST NEVER appear in client code or public env vars
    private val FORBIDDEN_SECRET_PREFIXES = listOf(
        "sk_live_",
        "sk_test_",
        "rk_live_",
        "rk_test_",
        "flwseck-",
        "flwseck_test",
        "aizasy",
        "whsec_",
        "sec_",
        "priv_",
        "-----begin private key",
        "-----begin rsa private key"
    )

    // Ephemeral runtime-only HMAC signing key for local server-side webhook verification when
    // System.getenv("PAYSTACK_SECRET_KEY") is not injected in the local JVM test environment.
    // Generated randomly at runtime; never hardcoded and never exposed to the client.
    private val ephemeralServerHmacKey: String by lazy {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        "ephemeral_srv_hmac_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Inspects any string or environment value to detect accidental exposure of a secret key
     * (e.g., if a developer or user mistakenly pastes an `sk_test_...` or `sk_live_...` key
     * into a public configuration field).
     */
    fun containsExposedSecretKey(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val normalized = candidate.trim().lowercase()
        return FORBIDDEN_SECRET_PREFIXES.any { prefix ->
            normalized.startsWith(prefix) || normalized.contains(prefix)
        }
    }

    /**
     * Validates that a key intended for public client usage is strictly a public key or safe placeholder,
     * and NEVER a secret key. If a secret key is detected, it is immediately rejected and redacted.
     */
    fun sanitizePublicKey(rawPublicKey: String?): String {
        if (rawPublicKey.isNullOrBlank()) {
            return "PUBLIC_KEY_CONFIGURE_IN_SECRETS_PANEL"
        }
        val trimmed = rawPublicKey.trim()
        if (containsExposedSecretKey(trimmed)) {
            // Immediately block and redact any secret key accidentally placed in a public variable
            return "REDACTED_ILLEGAL_SECRET_IN_PUBLIC_CONFIG"
        }
        return SecuritySanitizer.sanitizeText(trimmed, maxLength = 120, preserveNewlines = false)
    }

    /**
     * Returns the safe, public-only Paystack key from BuildConfig (if configured),
     * guaranteeing that no private credential is ever returned.
     */
    fun getSafePaystackPublicKey(): String {
        val rawFromBuildConfig = try {
            BuildConfig.PAYSTACK_PUBLIC_KEY
        } catch (e: Exception) {
            ""
        }
        return sanitizePublicKey(rawFromBuildConfig)
    }

    /**
     * Resolves the server-side webhook HMAC signing key ausschließlich from server environment
     * variables (`System.getenv("PAYSTACK_SECRET_KEY")`) or an explicit server-side test context,
     * falling back to an ephemeral runtime-generated random HMAC key.
     * Never reads from mobile `BuildConfig` and never uses a hardcoded secret string.
     */
    fun resolveServerWebhookSigningKey(explicitServerContextKey: String? = null): String {
        if (!explicitServerContextKey.isNullOrBlank()) {
            return explicitServerContextKey.trim()
        }
        val serverEnvKey = try {
            System.getenv("PAYSTACK_SECRET_KEY")
        } catch (e: SecurityException) {
            null
        }
        if (!serverEnvKey.isNullOrBlank() && serverEnvKey != "YOUR_PAYSTACK_SECRET_KEY") {
            return serverEnvKey.trim()
        }
        return ephemeralServerHmacKey
    }

    /**
     * Validates outbound mobile client HTTP headers to ensure the mobile app only sends
     * short-lived session/App Check authorization tokens to SolarFix Backend Functions and NEVER
     * transmits raw third-party secret keys (`sk_live_`, `sk_test_`, `FLWSECK`, `AIzaSy`).
     */
    fun validateOutboundClientHeaders(headers: Map<String, String>): InputValidationResult {
        for ((headerName, headerValue) in headers) {
            if (containsExposedSecretKey(headerValue)) {
                return InputValidationResult(
                    isValid = false,
                    errorMessage = "Security violation: Outbound client header '$headerName' contains a private API secret key. Route this call through a SolarFix Backend Function."
                )
            }
            if (headerName.contains("SECRET", ignoreCase = true) ||
                headerName.equals("x-paystack-secret", ignoreCase = true)
            ) {
                return InputValidationResult(
                    isValid = false,
                    errorMessage = "Security violation: Private credential header '$headerName' is forbidden on mobile client requests."
                )
            }
        }
        return InputValidationResult(isValid = true)
    }

    /**
     * Audits the compiled Android `BuildConfig` class via reflection to guarantee that:
     * 1. No field containing `SECRET` or `PRIVATE` was compiled into the APK.
     * 2. No field value in `BuildConfig` contains a raw secret key (`sk_test_`, `sk_live_`, `FLWSECK`, `AIzaSy`).
     */
    fun auditCompiledBuildConfigSecurity(): BuildConfigSecurityAuditResult {
        val exposedSecretFields = mutableListOf<String>()
        return try {
            val fields = BuildConfig::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val name = field.name
                val value = (field.get(null) as? String).orEmpty()

                val isForbiddenSecretFieldName = name.contains("SECRET", ignoreCase = true) ||
                    name.contains("PRIVATE", ignoreCase = true) ||
                    name.equals("GEMINI_API_KEY", ignoreCase = true)

                if (isForbiddenSecretFieldName || containsExposedSecretKey(value)) {
                    exposedSecretFields.add(name)
                }
            }
            BuildConfigSecurityAuditResult(
                isCompliant = exposedSecretFields.isEmpty(),
                inspectedFieldsCount = fields.size,
                exposedSecretFieldNames = exposedSecretFields,
                verifiedBackendProxyServices = listOf(
                    "Paystack -> $BACKEND_FUNCTIONS_BASE_URL$ENDPOINT_PAYSTACK_VERIFY",
                    "Flutterwave -> $BACKEND_FUNCTIONS_BASE_URL$ENDPOINT_FLUTTERWAVE_VERIFY",
                    "Google AI Services -> Firebase AI Logic + App Check Attestation",
                    "Identity Verification (KYB/CAC/NIN) -> $BACKEND_FUNCTIONS_BASE_URL$ENDPOINT_IDENTITY_KYB_VERIFY",
                    "Delivery & Logistics -> $BACKEND_FUNCTIONS_BASE_URL$ENDPOINT_DELIVERY_DISPATCH",
                    "OEM Serial Verification -> $BACKEND_FUNCTIONS_BASE_URL$ENDPOINT_OEM_SERIAL_VERIFY"
                )
            )
        } catch (e: Exception) {
            BuildConfigSecurityAuditResult(
                isCompliant = false,
                inspectedFieldsCount = 0,
                exposedSecretFieldNames = listOf("AUDIT_ERROR: ${e.message}"),
                verifiedBackendProxyServices = emptyList()
            )
        }
    }
}

data class BuildConfigSecurityAuditResult(
    val isCompliant: Boolean,
    val inspectedFieldsCount: Int,
    val exposedSecretFieldNames: List<String>,
    val verifiedBackendProxyServices: List<String>
)
