package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.util.SecuritySanitizer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "AuthRepository"
private const val PREFS_NAME = "solarfix_auth_prefs"
private const val KEY_UID = "user_uid"
private const val KEY_NAME = "user_name"
private const val KEY_EMAIL = "user_email"
private const val KEY_ROLE = "user_role"
private const val KEY_IS_ADMIN = "user_is_admin"
private const val KEY_CREATED_EPOCH = "user_created_epoch"
private const val KEY_SAFETY_ACCEPTED = "user_safety_accepted"
private const val KEY_EMAIL_VERIFIED = "user_email_verified"
private const val KEY_SESSION_ID = "user_session_id"
private const val KEY_ACCESS_TOKEN_HASH = "user_access_token_hash"
private const val KEY_REFRESH_TOKEN_HASH = "user_refresh_token_hash"
private const val KEY_ACCESS_EXPIRES_EPOCH = "user_access_expires_epoch"
private const val KEY_SESSION_ABSOLUTE_EXPIRES_EPOCH = "user_session_abs_expires_epoch"

/**
 * Pending email verification state.
 * SECURITY HARDENING: Stores ONLY the salted cryptographic `passwordHash` — never the plaintext password.
 */
data class PendingEmailVerification(
    val name: String,
    val email: String,
    val passwordHash: String,
    val verificationCodeHash: String,
    val expiryEpoch: Long,
    val failedAttempts: Int = 0,
    val verificationCode: String = ""
) {
    constructor(
        name: String,
        email: String,
        password: String,
        verificationCode: String,
        expiryEpoch: Long,
        failedAttempts: Int = 0
    ) : this(
        name = name,
        email = email,
        passwordHash = if (SecuritySanitizer.isPasswordHashed(password)) password else SecuritySanitizer.hashPasswordArgon2id(password),
        verificationCodeHash = SecuritySanitizer.hashOpaqueToken(verificationCode),
        expiryEpoch = expiryEpoch,
        failedAttempts = failedAttempts,
        verificationCode = verificationCode
    )
}

data class PendingPhoneVerification(
    val phoneNumber: String,
    val verificationCodeHash: String,
    val expiryEpoch: Long,
    val failedAttempts: Int = 0
)

data class PendingPasswordRecovery(
    val email: String,
    val recoveryCodeHash: String,
    val expiryEpoch: Long,
    val failedAttempts: Int = 0
)

enum class LoginAttemptOutcome {
    SUCCESS,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED_OUT,
    CREDENTIAL_STUFFING_BLOCKED,
    INVALID_INPUT,
    SESSION_REVOKED
}

/**
 * Login attempt audit record. Never contains plaintext passwords or raw tokens.
 */
data class LoginAttemptRecord(
    val id: String = "att_${UUID.randomUUID().toString().take(8)}",
    val timestampEpoch: Long = System.currentTimeMillis(),
    val maskedEmail: String,
    val outcome: LoginAttemptOutcome,
    val reason: String
)

/**
 * Active session record with hashed token digests and explicit expiration windows.
 * Raw tokens are returned to the caller only at issuance/rotation time and are never stored in plaintext.
 */
data class AuthSessionRecord(
    val sessionId: String,
    val uid: String,
    val email: String,
    val accessTokenHash: String,
    val refreshTokenHash: String,
    val issuedAtEpoch: Long,
    val accessTokenExpiresAtEpoch: Long,
    val absoluteSessionExpiresAtEpoch: Long,
    val isRevoked: Boolean = false
)

data class IssuedSessionTokens(
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpoch: Long,
    val absoluteSessionExpiresAtEpoch: Long
)

/**
 * Scopes requiring Step-Up Multi-Factor Authentication (MFA) for administrators and sensitive seller operations.
 */
enum class MfaOperationScope(val description: String) {
    ADMIN_DASHBOARD_ACCESS("Administrator Dashboard & Security Controls"),
    ADMIN_COMMISSION_CONFIGURATION("Platform & Seller Commission Modification"),
    ADMIN_WITHDRAWAL_APPROVAL("Administrator Seller Payout Approval"),
    SELLER_BANK_PAYOUT_WITHDRAWAL("Seller Bank Account Withdrawal Request"),
    SELLER_BANK_ACCOUNT_CHANGE("Seller Settlement Bank / NUBAN Modification")
}

data class MfaChallenge(
    val challengeId: String,
    val email: String,
    val maskedEmail: String,
    val scope: MfaOperationScope,
    val codeHash: String,
    val expiresAtEpoch: Long,
    val failedAttempts: Int = 0
)

data class MfaStepUpGrant(
    val grantId: String,
    val email: String,
    val scope: MfaOperationScope,
    val issuedAtEpoch: Long,
    val expiresAtEpoch: Long
)

class AuthRepository(context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth? = try {
        if (context != null) FirebaseAuth.getInstance() else null
    } catch (_: Throwable) {
        null
    }

    val adminClaimHandler = com.example.util.FirebaseAuthAdminHandler(
        auth = firebaseAuth,
        firestore = if (context != null) {
            try { com.google.firebase.firestore.FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
        } else null
    )

    private fun safeLogInfo(msg: String) {
        try {
            Log.i(TAG, SecuritySanitizer.redactTokensFromTextOrUrl(msg))
        } catch (_: Throwable) {}
    }

    private fun safeLogWarn(msg: String) {
        try {
            Log.w(TAG, SecuritySanitizer.redactTokensFromTextOrUrl(msg))
        } catch (_: Throwable) {}
    }

    private fun safeLogError(msg: String) {
        try {
            Log.e(TAG, SecuritySanitizer.redactTokensFromTextOrUrl(msg))
        } catch (_: Throwable) {}
    }

    // In-memory store fallback when running in local JVM unit tests without Android Context
    private val inMemoryPasswordHashes = mutableMapOf<String, String>()
    private val failedLoginCounts = mutableMapOf<String, Int>()
    private val lockoutUntilEpochs = mutableMapOf<String, Long>()

    // Credential stuffing monitor: tracks recent failed login timestamps per distinct target email
    private val recentGlobalFailedAttempts = mutableListOf<Pair<String, Long>>()
    private var credentialStuffingBlockedUntilEpoch: Long = 0L

    // Account recovery rate limiter: email -> list of recovery request timestamps
    private val recoveryRequestTimestamps = mutableMapOf<String, MutableList<Long>>()
    private val pendingRecoveries = mutableMapOf<String, PendingPasswordRecovery>()
    private val pendingPhoneVerifications = mutableMapOf<String, PendingPhoneVerification>()
    private val verifiedPhoneNumbers = mutableSetOf<String>()

    // Session & Token Management state
    private val activeSessionsById = mutableMapOf<String, AuthSessionRecord>()
    private val revokedSessionIds = mutableSetOf<String>()
    private val revokedTokenHashes = mutableSetOf<String>()
    private var _latestIssuedTokens: IssuedSessionTokens? = null
    val latestIssuedTokens: IssuedSessionTokens? get() = _latestIssuedTokens

    // Step-Up MFA state for Administrators and Sensitive Seller Operations
    private val activeMfaChallenges = mutableMapOf<String, MfaChallenge>()
    private val activeMfaGrants = mutableMapOf<String, MfaStepUpGrant>()

    // Login Attempt Audit Trail
    private val _loginAttemptAuditLog = MutableStateFlow<List<LoginAttemptRecord>>(emptyList())
    val loginAttemptAuditLog: StateFlow<List<LoginAttemptRecord>> = _loginAttemptAuditLog.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(loadPersistedSession())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _pendingVerification = MutableStateFlow<PendingEmailVerification?>(null)
    val pendingVerification: StateFlow<PendingEmailVerification?> = _pendingVerification.asStateFlow()

    val isAuthenticated: Boolean
        get() = _currentUser.value != null && isCurrentSessionValid()

    companion object {
        const val OWNER_EMAIL = "nekeskingsley@gmail.com"
        const val DEFAULT_ADMIN_EMAIL = "admin@solarfix.africa"
        const val MAX_FAILED_LOGIN_ATTEMPTS = 5
        const val MAX_OTP_ATTEMPTS = 5
        const val MAX_MFA_ATTEMPTS = 3
        const val MAX_RECOVERY_REQUESTS_PER_WINDOW = 3
        const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L
        const val OTP_EXPIRY_DURATION_MS = 15 * 60 * 1000L
        const val MFA_CHALLENGE_EXPIRY_MS = 5 * 60 * 1000L
        const val MFA_GRANT_VALIDITY_MS = 15 * 60 * 1000L
        const val ACCESS_TOKEN_TTL_MS = 60 * 60 * 1000L // 1 hour
        const val ADMIN_ACCESS_TOKEN_TTL_MS = 30 * 60 * 1000L // 30 minutes for admin sessions
        const val ABSOLUTE_SESSION_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val CREDENTIAL_STUFFING_WINDOW_MS = 5 * 60 * 1000L
        const val CREDENTIAL_STUFFING_DISTINCT_EMAILS_THRESHOLD = 3
        const val CREDENTIAL_STUFFING_TOTAL_FAILS_THRESHOLD = 6

        /**
         * SECURITY HARDENING (VULN-01): Enforces exact canonical equality for owner/admin email
         * and prevents privilege escalation via prefix/substring matching.
         */
        fun isAppOwnerOrAdminEmail(email: String): Boolean {
            val lower = email.trim().lowercase()
            return lower == OWNER_EMAIL || lower == DEFAULT_ADMIN_EMAIL
        }
    }

    suspend fun extractAdminClaim(
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        forceRefresh: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val email = firebaseUser.email.orEmpty()
        if (isAppOwnerOrAdminEmail(email)) {
            return@withContext true
        }
        try {
            val tokenResult = firebaseUser.getIdToken(forceRefresh).await()
            val claims = tokenResult.claims
            val hasClaim = claims["admin"] == true ||
                claims["isAdmin"] == true ||
                (claims["role"] as? String)?.equals("admin", ignoreCase = true) == true
            safeLogInfo("Custom Firebase Auth claim evaluation: admin=$hasClaim")
            hasClaim
        } catch (e: Exception) {
            safeLogWarn("Could not fetch Firebase Auth token claims")
            false
        }
    }

    private fun resolveRoleForEmail(email: String, savedRoleName: String?): UserRole {
        if (isAppOwnerOrAdminEmail(email)) {
            return UserRole.ADMIN
        }
        if (!savedRoleName.isNullOrBlank()) {
            try {
                val parsed = UserRole.valueOf(savedRoleName)
                // SECURITY HARDENING: Never trust a saved ADMIN role for a non-admin email
                if (parsed != UserRole.ADMIN) {
                    return parsed
                }
            } catch (_: Exception) {}
        }
        val lower = email.lowercase()
        return when {
            lower.contains("tech") -> UserRole.TECHNICIAN
            else -> UserRole.CUSTOMER
        }
    }

    private fun loadPersistedSession(): UserAccount? {
        val p = prefs ?: return null
        val now = System.currentTimeMillis()

        // Enforce session expiration before restoring any persisted session
        val savedSessionId = p.getString(KEY_SESSION_ID, null)
        val accessExpires = p.getLong(KEY_ACCESS_EXPIRES_EPOCH, 0L)
        val absExpires = p.getLong(KEY_SESSION_ABSOLUTE_EXPIRES_EPOCH, 0L)
        if (savedSessionId != null && revokedSessionIds.contains(savedSessionId)) {
            clearPersistedSessionPrefs()
            return null
        }
        if ((accessExpires in 1 until now) || (absExpires in 1 until now)) {
            clearPersistedSessionPrefs()
            return null
        }

        // First check active Firebase Auth session
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null && fbUser.email != null) {
            val email = fbUser.email!!
            val isOwner = isAppOwnerOrAdminEmail(email)
            val name = fbUser.displayName.takeUnless { it.isNullOrBlank() }
                ?: p.getString(KEY_NAME, "")?.takeUnless { it.isBlank() }
                ?: if (isOwner) "Nekes Kingsley" else email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val savedRole = p.getString(KEY_ROLE, null)
            val role = if (isOwner) UserRole.ADMIN else resolveRoleForEmail(email, savedRole)
            val savedIsAdmin = isOwner && (role == UserRole.ADMIN)
            val account = UserAccount(
                uid = fbUser.uid,
                name = SecuritySanitizer.sanitizeText(name, maxLength = 120),
                email = email,
                role = role,
                isAdmin = savedIsAdmin,
                createdAtEpoch = fbUser.metadata?.creationTimestamp ?: now,
                safetyTermsAccepted = p.getBoolean(KEY_SAFETY_ACCEPTED, true)
            )
            issueAndPersistSession(account, now)
            return account
        }

        // Second: check SharedPreferences persistent local session
        val savedUid = p.getString(KEY_UID, null)
        val savedEmail = p.getString(KEY_EMAIL, null)
        val savedName = p.getString(KEY_NAME, null)
        val savedEpoch = p.getLong(KEY_CREATED_EPOCH, 0L)
        val savedRole = p.getString(KEY_ROLE, null)
        val safetyAccepted = p.getBoolean(KEY_SAFETY_ACCEPTED, true)

        return if (!savedUid.isNullOrBlank() && !savedEmail.isNullOrBlank()) {
            val isOwner = isAppOwnerOrAdminEmail(savedEmail)
            val role = if (isOwner) UserRole.ADMIN else resolveRoleForEmail(savedEmail, savedRole)
            val savedIsAdmin = isOwner && (role == UserRole.ADMIN)
            val resolvedName = if (isOwner && (savedName.isNullOrBlank() || savedName == savedEmail.substringBefore("@"))) {
                "Nekes Kingsley"
            } else {
                savedName ?: savedEmail.substringBefore("@")
            }
            val account = UserAccount(
                uid = savedUid,
                name = SecuritySanitizer.sanitizeText(resolvedName, maxLength = 120),
                email = savedEmail,
                role = role,
                isAdmin = savedIsAdmin,
                createdAtEpoch = if (savedEpoch > 0) savedEpoch else now,
                safetyTermsAccepted = safetyAccepted,
                isEmailVerified = p.getBoolean(KEY_EMAIL_VERIFIED, true)
            )
            if (savedSessionId != null && !activeSessionsById.containsKey(savedSessionId)) {
                activeSessionsById[savedSessionId] = AuthSessionRecord(
                    sessionId = savedSessionId,
                    uid = account.uid,
                    email = account.email,
                    accessTokenHash = p.getString(KEY_ACCESS_TOKEN_HASH, "").orEmpty(),
                    refreshTokenHash = p.getString(KEY_REFRESH_TOKEN_HASH, "").orEmpty(),
                    issuedAtEpoch = account.createdAtEpoch,
                    accessTokenExpiresAtEpoch = if (accessExpires > 0L) accessExpires else now + ACCESS_TOKEN_TTL_MS,
                    absoluteSessionExpiresAtEpoch = if (absExpires > 0L) absExpires else now + ABSOLUTE_SESSION_TTL_MS
                )
            }
            account
        } else {
            null
        }
    }

    // =========================================================================
    // 1. Secure Registration & Email / Phone Verification
    // =========================================================================

    fun initiateSignUpWithVerification(name: String, email: String, password: String): Result<String> {
        val validation = SecuritySanitizer.validateRegistrationInput(name, email, password)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid registration input."))
        }
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val cleanName = SecuritySanitizer.sanitizeText(name, maxLength = 120)

        // Generate 6-digit OTP with cryptographic SecureRandom and hash both password and OTP immediately
        val code = SecuritySanitizer.generateSecureSixDigitCode()
        val passwordHash = SecuritySanitizer.hashPasswordArgon2id(password)
        val codeHash = SecuritySanitizer.hashOpaqueToken(code)

        val pending = PendingEmailVerification(
            name = cleanName,
            email = cleanEmail,
            passwordHash = passwordHash,
            verificationCodeHash = codeHash,
            expiryEpoch = System.currentTimeMillis() + OTP_EXPIRY_DURATION_MS,
            failedAttempts = 0
        )
        _pendingVerification.value = pending
        safeLogInfo("Dispatched verification code for ${SecuritySanitizer.maskEmail(cleanEmail)}: ${SecuritySanitizer.maskSensitiveValue(code, 2)}")
        return Result.success(code)
    }

    suspend fun verifyEmailCode(enteredCode: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val pending = _pendingVerification.value
            ?: return@withContext Result.failure(IllegalStateException("No pending verification session found. Please sign up again."))

        if (System.currentTimeMillis() > pending.expiryEpoch) {
            _pendingVerification.value = null
            return@withContext Result.failure(IllegalArgumentException("Verification code has expired. Please request a new code."))
        }

        if (pending.failedAttempts >= MAX_OTP_ATTEMPTS) {
            _pendingVerification.value = null
            return@withContext Result.failure(IllegalStateException("Too many invalid verification attempts. Please request a new verification code."))
        }

        val enteredHash = SecuritySanitizer.hashOpaqueToken(enteredCode.trim())
        val isCodeValid = SecuritySanitizer.constantTimeEquals(enteredHash, pending.verificationCodeHash)

        if (!isCodeValid) {
            val nextAttempts = pending.failedAttempts + 1
            if (nextAttempts >= MAX_OTP_ATTEMPTS) {
                _pendingVerification.value = null
                return@withContext Result.failure(IllegalStateException("Maximum verification attempts exceeded. Verification code invalidated."))
            } else {
                _pendingVerification.value = pending.copy(failedAttempts = nextAttempts)
                return@withContext Result.failure(IllegalArgumentException("Invalid verification code. Please check your email or request a new code."))
            }
        }

        val result = finalizeVerifiedSignUp(
            name = pending.name,
            email = pending.email,
            precomputedPasswordHash = pending.passwordHash,
            isEmailVerified = true
        )
        if (result.isSuccess) {
            _pendingVerification.value = null
        }
        result
    }

    fun resendVerificationCode(): Result<String> {
        val pending = _pendingVerification.value
            ?: return Result.failure(IllegalStateException("No active registration session."))
        val newCode = SecuritySanitizer.generateSecureSixDigitCode()
        val updated = pending.copy(
            verificationCodeHash = SecuritySanitizer.hashOpaqueToken(newCode),
            expiryEpoch = System.currentTimeMillis() + OTP_EXPIRY_DURATION_MS,
            failedAttempts = 0
        )
        _pendingVerification.value = updated
        safeLogInfo("Resent verification code for ${SecuritySanitizer.maskEmail(pending.email)}")
        return Result.success(newCode)
    }

    fun clearPendingVerification() {
        _pendingVerification.value = null
    }

    /**
     * Initiates SMS/Phone OTP verification for a user or seller phone number.
     */
    fun initiatePhoneVerification(phoneNumber: String): Result<String> {
        if (!SecuritySanitizer.isValidPhoneFormat(phoneNumber)) {
            return Result.failure(IllegalArgumentException("Please enter a valid phone number."))
        }
        val cleanPhone = phoneNumber.trim()
        val otp = SecuritySanitizer.generateSecureSixDigitCode()
        pendingPhoneVerifications[cleanPhone] = PendingPhoneVerification(
            phoneNumber = cleanPhone,
            verificationCodeHash = SecuritySanitizer.hashOpaqueToken(otp),
            expiryEpoch = System.currentTimeMillis() + 10 * 60 * 1000L,
            failedAttempts = 0
        )
        return Result.success(otp)
    }

    fun verifyPhoneCode(phoneNumber: String, enteredCode: String): Result<Boolean> {
        val cleanPhone = phoneNumber.trim()
        val pending = pendingPhoneVerifications[cleanPhone]
            ?: return Result.failure(IllegalStateException("No pending phone verification found."))
        if (System.currentTimeMillis() > pending.expiryEpoch) {
            pendingPhoneVerifications.remove(cleanPhone)
            return Result.failure(IllegalArgumentException("Phone verification code has expired."))
        }
        if (pending.failedAttempts >= MAX_OTP_ATTEMPTS) {
            pendingPhoneVerifications.remove(cleanPhone)
            return Result.failure(IllegalStateException("Maximum phone verification attempts exceeded."))
        }
        val enteredHash = SecuritySanitizer.hashOpaqueToken(enteredCode.trim())
        if (!SecuritySanitizer.constantTimeEquals(enteredHash, pending.verificationCodeHash)) {
            val next = pending.failedAttempts + 1
            if (next >= MAX_OTP_ATTEMPTS) {
                pendingPhoneVerifications.remove(cleanPhone)
            } else {
                pendingPhoneVerifications[cleanPhone] = pending.copy(failedAttempts = next)
            }
            return Result.failure(IllegalArgumentException("Invalid phone verification code."))
        }
        pendingPhoneVerifications.remove(cleanPhone)
        verifiedPhoneNumbers.add(cleanPhone)
        return Result.success(true)
    }

    fun isPhoneVerified(phoneNumber: String): Boolean = verifiedPhoneNumbers.contains(phoneNumber.trim())

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        isEmailVerified: Boolean = true
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val validation = SecuritySanitizer.validateRegistrationInput(name, email, password)
        if (!validation.isValid) {
            return@withContext Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid registration input."))
        }
        val passwordHash = SecuritySanitizer.hashPasswordArgon2id(password)
        finalizeVerifiedSignUp(
            name = name,
            email = email,
            precomputedPasswordHash = passwordHash,
            isEmailVerified = isEmailVerified
        )
    }

    private suspend fun finalizeVerifiedSignUp(
        name: String,
        email: String,
        precomputedPasswordHash: String,
        isEmailVerified: Boolean
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
            val cleanName = SecuritySanitizer.sanitizeText(name, maxLength = 120)
            val isOwner = isAppOwnerOrAdminEmail(cleanEmail)
            val role = if (isOwner) UserRole.ADMIN else resolveRoleForEmail(cleanEmail, null)
            val resolvedName = cleanName.ifEmpty {
                if (isOwner) "Nekes Kingsley" else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            }

            val uid = "user_${UUID.nameUUIDFromBytes(cleanEmail.toByteArray()).toString().take(12)}"
            val account = UserAccount(
                uid = uid,
                name = resolvedName,
                email = cleanEmail,
                role = role,
                isAdmin = isOwner,
                createdAtEpoch = System.currentTimeMillis(),
                safetyTermsAccepted = true,
                isEmailVerified = isEmailVerified
            )

            storePasswordHash(cleanEmail, precomputedPasswordHash)
            clearFailedLoginAttempts(cleanEmail)
            issueAndPersistSession(account)
            _currentUser.value = account

            try {
                adminClaimHandler.syncUserToFirestore(account)
            } catch (e: Exception) {
                safeLogWarn("Background Firestore sync deferred")
            }

            Result.success(account)
        } catch (e: Exception) {
            safeLogError("Sign up error")
            Result.failure(e)
        }
    }

    private fun storePasswordHash(cleanEmail: String, passwordHash: String) {
        require(SecuritySanitizer.isPasswordHashed(passwordHash)) {
            "Plaintext passwords must never be stored."
        }
        inMemoryPasswordHashes[cleanEmail] = passwordHash
        prefs?.edit()?.putString("auth_pwd_$cleanEmail", passwordHash)?.apply()
    }

    fun getStoredPasswordRecordForAudit(email: String): String? {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        return inMemoryPasswordHashes[cleanEmail] ?: prefs?.getString("auth_pwd_$cleanEmail", null)
    }

    // =========================================================================
    // 2. Brute-Force Protection, Credential Stuffing Defense & Login Monitoring
    // =========================================================================

    fun isAccountLockedOut(email: String, nowEpoch: Long = System.currentTimeMillis()): Boolean {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val lockoutEpoch = lockoutUntilEpochs[cleanEmail]
            ?: prefs?.getLong("auth_lockout_$cleanEmail", 0L)
            ?: 0L
        if (lockoutEpoch > nowEpoch) {
            return true
        }
        if (lockoutEpoch > 0L) {
            clearFailedLoginAttempts(cleanEmail)
        }
        return false
    }

    fun getFailedLoginAttempts(email: String): Int {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        return failedLoginCounts[cleanEmail] ?: prefs?.getInt("auth_fail_cnt_$cleanEmail", 0) ?: 0
    }

    /**
     * Detects credential-stuffing attacks where an automated attacker rapidly tests credentials
     * across multiple distinct user accounts within a short time window.
     */
    fun isCredentialStuffingBlocked(nowEpoch: Long = System.currentTimeMillis()): Boolean {
        if (credentialStuffingBlockedUntilEpoch > nowEpoch) {
            return true
        }
        recentGlobalFailedAttempts.removeAll { (_, ts) -> (nowEpoch - ts) > CREDENTIAL_STUFFING_WINDOW_MS }
        val distinctAccountsTargeted = recentGlobalFailedAttempts.map { it.first }.toSet().size
        if (recentGlobalFailedAttempts.size >= CREDENTIAL_STUFFING_TOTAL_FAILS_THRESHOLD &&
            distinctAccountsTargeted >= CREDENTIAL_STUFFING_DISTINCT_EMAILS_THRESHOLD
        ) {
            credentialStuffingBlockedUntilEpoch = nowEpoch + LOCKOUT_DURATION_MS
            return true
        }
        return false
    }

    private fun recordFailedLoginAttempt(cleanEmail: String, nowEpoch: Long = System.currentTimeMillis()) {
        val nextCount = getFailedLoginAttempts(cleanEmail) + 1
        failedLoginCounts[cleanEmail] = nextCount
        val editor = prefs?.edit()?.putInt("auth_fail_cnt_$cleanEmail", nextCount)
        if (nextCount >= MAX_FAILED_LOGIN_ATTEMPTS) {
            val unlockAt = nowEpoch + LOCKOUT_DURATION_MS
            lockoutUntilEpochs[cleanEmail] = unlockAt
            editor?.putLong("auth_lockout_$cleanEmail", unlockAt)
            com.example.util.MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = com.example.util.SecurityMonitoringCategory.REPEATED_FAILED_LOGINS,
                eventType = "BRUTE_FORCE_ACCOUNT_LOCKOUT",
                actorIdentifier = cleanEmail,
                targetResourceId = "auth/login",
                details = "Account temporarily locked after $nextCount repeated failed login attempts"
            )
        }
        editor?.apply()

        recentGlobalFailedAttempts.add(cleanEmail to nowEpoch)
        if (isCredentialStuffingBlocked(nowEpoch)) {
            com.example.util.MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = com.example.util.SecurityMonitoringCategory.REPEATED_FAILED_LOGINS,
                eventType = "CREDENTIAL_STUFFING_MULTI_ACCOUNT_BURST",
                actorIdentifier = cleanEmail,
                targetResourceId = "auth/login",
                details = "Global credential-stuffing protection triggered across multiple accounts"
            )
        }
    }

    fun clearFailedLoginAttempts(email: String) {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        failedLoginCounts.remove(cleanEmail)
        lockoutUntilEpochs.remove(cleanEmail)
        prefs?.edit()
            ?.remove("auth_fail_cnt_$cleanEmail")
            ?.remove("auth_lockout_$cleanEmail")
            ?.apply()
    }

    fun resetCredentialStuffingGuard() {
        recentGlobalFailedAttempts.clear()
        credentialStuffingBlockedUntilEpoch = 0L
    }

    private fun logLoginAttempt(email: String, outcome: LoginAttemptOutcome, reason: String) {
        val record = LoginAttemptRecord(
            maskedEmail = SecuritySanitizer.maskEmail(email),
            outcome = outcome,
            reason = SecuritySanitizer.redactTokensFromTextOrUrl(reason)
        )
        _loginAttemptAuditLog.update { (listOf(record) + it).take(200) }
    }

    // =========================================================================
    // 3. Secure Login
    // =========================================================================

    suspend fun login(email: String, password: String): Result<UserAccount> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val loginValidation = SecuritySanitizer.validateLoginInput(email, password)
                if (!loginValidation.isValid) {
                    logLoginAttempt(email, LoginAttemptOutcome.INVALID_INPUT, loginValidation.errorMessage ?: "Invalid login input")
                    return@withContext Result.failure(IllegalArgumentException(loginValidation.errorMessage ?: "Invalid login input."))
                }
                val cleanEmail = SecuritySanitizer.sanitizeEmail(email)

                // Check global credential-stuffing defense
                if (isCredentialStuffingBlocked(now)) {
                    logLoginAttempt(cleanEmail, LoginAttemptOutcome.CREDENTIAL_STUFFING_BLOCKED, "Automated multi-account credential stuffing detected.")
                    return@withContext Result.failure(
                        IllegalStateException("Unusual multi-account login activity detected. Login requests are temporarily rate-limited.")
                    )
                }

                // Check per-account brute-force lockout protection
                if (isAccountLockedOut(cleanEmail, now)) {
                    logLoginAttempt(cleanEmail, LoginAttemptOutcome.ACCOUNT_LOCKED_OUT, "Account locked due to repeated failed attempts.")
                    return@withContext Result.failure(
                        IllegalStateException("Account temporarily locked due to multiple failed login attempts. Please try again in 15 minutes.")
                    )
                }

                val isOwner = isAppOwnerOrAdminEmail(cleanEmail)
                val savedPasswordRecord = getStoredPasswordRecordForAudit(cleanEmail)

                if (savedPasswordRecord != null) {
                    val isPasswordValid = SecuritySanitizer.verifyPassword(password, savedPasswordRecord)
                    if (!isPasswordValid) {
                        recordFailedLoginAttempt(cleanEmail, now)
                        logLoginAttempt(cleanEmail, LoginAttemptOutcome.INVALID_CREDENTIALS, "Password hash verification failed.")
                        return@withContext Result.failure(IllegalArgumentException("Incorrect email or password. Please verify your credentials."))
                    }
                    if (!SecuritySanitizer.isPasswordHashed(savedPasswordRecord)) {
                        storePasswordHash(cleanEmail, SecuritySanitizer.hashPasswordArgon2id(password))
                    }
                } else {
                    val pwdError = SecuritySanitizer.validatePasswordStrength(password)
                    if (pwdError != null) {
                        recordFailedLoginAttempt(cleanEmail, now)
                        logLoginAttempt(cleanEmail, LoginAttemptOutcome.INVALID_CREDENTIALS, pwdError)
                        return@withContext Result.failure(IllegalArgumentException(pwdError))
                    }
                    storePasswordHash(cleanEmail, SecuritySanitizer.hashPasswordArgon2id(password))
                }

                clearFailedLoginAttempts(cleanEmail)

                val savedEmail = prefs?.getString(KEY_EMAIL, null)
                val savedName = prefs?.getString(KEY_NAME, null)
                val resolvedName = if (savedEmail?.equals(cleanEmail, ignoreCase = true) == true && !savedName.isNullOrBlank()) {
                    savedName
                } else if (isOwner) {
                    "Nekes Kingsley"
                } else {
                    cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }

                val role = if (isOwner) UserRole.ADMIN else resolveRoleForEmail(cleanEmail, prefs?.getString(KEY_ROLE, null))
                val isAdmin = isOwner && (role == UserRole.ADMIN)

                val savedUid = if (savedEmail?.equals(cleanEmail, ignoreCase = true) == true) {
                    prefs?.getString(KEY_UID, null)
                } else null
                val uid = savedUid ?: "user_${UUID.nameUUIDFromBytes(cleanEmail.toByteArray()).toString().take(12)}"

                val account = UserAccount(
                    uid = uid,
                    name = SecuritySanitizer.sanitizeText(resolvedName, maxLength = 120),
                    email = cleanEmail,
                    role = role,
                    isAdmin = isAdmin,
                    createdAtEpoch = now,
                    safetyTermsAccepted = true
                )

                issueAndPersistSession(account, now)
                _currentUser.value = account
                logLoginAttempt(cleanEmail, LoginAttemptOutcome.SUCCESS, "Authenticated session established.")

                try {
                    adminClaimHandler.syncUserToFirestore(account)
                } catch (e: Exception) {
                    safeLogWarn("Background Firestore sync deferred")
                }

                Result.success(account)
            } catch (e: Exception) {
                safeLogError("Login error")
                Result.failure(e)
            }
        }

    // =========================================================================
    // 4. Secure Password Reset & Account Recovery Protection
    // =========================================================================

    /**
     * Initiates a rate-limited password recovery flow and generates a time-limited, single-use
     * cryptographic 6-digit recovery code.
     */
    fun initiatePasswordRecovery(email: String, nowEpoch: Long = System.currentTimeMillis()): Result<String> {
        val validation = SecuritySanitizer.validatePasswordResetInput(email)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid email address."))
        }
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)

        // Rate-limit password reset requests per account to prevent recovery flooding
        val history = recoveryRequestTimestamps.getOrPut(cleanEmail) { mutableListOf() }
        history.removeAll { ts -> (nowEpoch - ts) > OTP_EXPIRY_DURATION_MS }
        if (history.size >= MAX_RECOVERY_REQUESTS_PER_WINDOW) {
            return Result.failure(
                IllegalStateException("Too many password recovery requests. Please wait 15 minutes before trying again.")
            )
        }
        history.add(nowEpoch)

        val recoveryCode = SecuritySanitizer.generateSecureSixDigitCode()
        pendingRecoveries[cleanEmail] = PendingPasswordRecovery(
            email = cleanEmail,
            recoveryCodeHash = SecuritySanitizer.hashOpaqueToken(recoveryCode),
            expiryEpoch = nowEpoch + OTP_EXPIRY_DURATION_MS,
            failedAttempts = 0
        )
        safeLogInfo("Password recovery initiated for ${SecuritySanitizer.maskEmail(cleanEmail)}")
        return Result.success(recoveryCode)
    }

    /**
     * Verifies the single-use recovery code, enforces strong password requirements on the new password,
     * updates the stored Argon2id password hash, and revokes all existing sessions for the account.
     */
    fun completePasswordReset(
        email: String,
        recoveryCode: String,
        newPassword: String,
        nowEpoch: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val pending = pendingRecoveries[cleanEmail]
            ?: return Result.failure(IllegalStateException("No active password reset request found for this account."))

        if (nowEpoch > pending.expiryEpoch) {
            pendingRecoveries.remove(cleanEmail)
            return Result.failure(IllegalArgumentException("Password recovery code has expired. Please request a new code."))
        }

        if (pending.failedAttempts >= MAX_OTP_ATTEMPTS) {
            pendingRecoveries.remove(cleanEmail)
            return Result.failure(IllegalStateException("Maximum recovery attempts exceeded. Recovery token invalidated."))
        }

        val codeHash = SecuritySanitizer.hashOpaqueToken(recoveryCode.trim())
        if (!SecuritySanitizer.constantTimeEquals(codeHash, pending.recoveryCodeHash)) {
            val nextAttempts = pending.failedAttempts + 1
            if (nextAttempts >= MAX_OTP_ATTEMPTS) {
                pendingRecoveries.remove(cleanEmail)
            } else {
                pendingRecoveries[cleanEmail] = pending.copy(failedAttempts = nextAttempts)
            }
            return Result.failure(IllegalArgumentException("Invalid password recovery code."))
        }

        val pwdError = SecuritySanitizer.validatePasswordStrength(newPassword)
        if (pwdError != null) {
            return Result.failure(IllegalArgumentException(pwdError))
        }

        // Store new salted Argon2id hash, invalidate recovery token, clear lockouts, and revoke all prior sessions
        storePasswordHash(cleanEmail, SecuritySanitizer.hashPasswordArgon2id(newPassword))
        pendingRecoveries.remove(cleanEmail)
        clearFailedLoginAttempts(cleanEmail)
        revokeAllSessionsForEmail(cleanEmail, reason = "Password reset completed — prior sessions revoked")

        return Result.success(Unit)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            initiatePasswordRecovery(email).map { Unit }
        }

    // =========================================================================
    // 5. Session Management, Expiration, Token Refresh Rotation & Revocation
    // =========================================================================

    private fun issueAndPersistSession(
        account: UserAccount,
        nowEpoch: Long = System.currentTimeMillis()
    ): IssuedSessionTokens {
        val sessionId = "sess_${SecuritySanitizer.generateOpaqueCryptoToken(18)}"
        val rawAccessToken = "sf_at_${SecuritySanitizer.generateOpaqueCryptoToken(32)}"
        val rawRefreshToken = "sf_rt_${SecuritySanitizer.generateOpaqueCryptoToken(32)}"

        val accessTokenHash = SecuritySanitizer.hashOpaqueToken(rawAccessToken)
        val refreshTokenHash = SecuritySanitizer.hashOpaqueToken(rawRefreshToken)

        val accessTtl = if (account.isAdmin) ADMIN_ACCESS_TOKEN_TTL_MS else ACCESS_TOKEN_TTL_MS
        val accessExpires = nowEpoch + accessTtl
        val absExpires = nowEpoch + ABSOLUTE_SESSION_TTL_MS

        val record = AuthSessionRecord(
            sessionId = sessionId,
            uid = account.uid,
            email = account.email,
            accessTokenHash = accessTokenHash,
            refreshTokenHash = refreshTokenHash,
            issuedAtEpoch = nowEpoch,
            accessTokenExpiresAtEpoch = accessExpires,
            absoluteSessionExpiresAtEpoch = absExpires,
            isRevoked = false
        )
        activeSessionsById[sessionId] = record

        val issued = IssuedSessionTokens(
            sessionId = sessionId,
            accessToken = rawAccessToken,
            refreshToken = rawRefreshToken,
            accessTokenExpiresAtEpoch = accessExpires,
            absoluteSessionExpiresAtEpoch = absExpires
        )
        _latestIssuedTokens = issued

        // Persist raw tokens ONLY inside the AES-256-GCM / AndroidKeyStore Secure Platform Token Vault
        com.example.util.MobileSecurityAndMonitoringManager.storeTokenInSecurePlatformStorage(
            tokenKey = "access_token_$sessionId",
            rawTokenValue = rawAccessToken,
            userUid = account.uid
        )
        com.example.util.MobileSecurityAndMonitoringManager.storeTokenInSecurePlatformStorage(
            tokenKey = "refresh_token_$sessionId",
            rawTokenValue = rawRefreshToken,
            userUid = account.uid
        )

        prefs?.edit()
            ?.putString(KEY_UID, account.uid)
            ?.putString(KEY_NAME, account.name)
            ?.putString(KEY_EMAIL, account.email)
            ?.putString(KEY_ROLE, account.role.name)
            ?.putBoolean(KEY_IS_ADMIN, account.isAdmin)
            ?.putLong(KEY_CREATED_EPOCH, account.createdAtEpoch)
            ?.putBoolean(KEY_SAFETY_ACCEPTED, account.safetyTermsAccepted)
            ?.putBoolean(KEY_EMAIL_VERIFIED, account.isEmailVerified)
            ?.putString(KEY_SESSION_ID, sessionId)
            ?.putString(KEY_ACCESS_TOKEN_HASH, accessTokenHash)
            ?.putString(KEY_REFRESH_TOKEN_HASH, refreshTokenHash)
            ?.putLong(KEY_ACCESS_EXPIRES_EPOCH, accessExpires)
            ?.putLong(KEY_SESSION_ABSOLUTE_EXPIRES_EPOCH, absExpires)
            ?.apply()

        return issued
    }

    /**
     * Verifies whether the current session (or a specific session ID) is active, non-revoked,
     * and within both its access token TTL and absolute session lifetime.
     */
    fun isCurrentSessionValid(nowEpoch: Long = System.currentTimeMillis()): Boolean {
        val sessionId = _latestIssuedTokens?.sessionId ?: prefs?.getString(KEY_SESSION_ID, null) ?: return false
        return isSessionValid(sessionId, nowEpoch)
    }

    fun isSessionValid(sessionId: String, nowEpoch: Long = System.currentTimeMillis()): Boolean {
        if (revokedSessionIds.contains(sessionId)) return false
        val session = activeSessionsById[sessionId] ?: return false
        if (session.isRevoked) return false
        if (nowEpoch > session.accessTokenExpiresAtEpoch || nowEpoch > session.absoluteSessionExpiresAtEpoch) {
            // Automatically expire and clean up stale session
            if (_latestIssuedTokens?.sessionId == sessionId) {
                logout()
            }
            return false
        }
        return true
    }

    /**
     * Rotates the session's access token and refresh token using a valid, non-expired refresh token.
     * Immediately invalidates the previous refresh and access token hashes to prevent token replay.
     */
    fun refreshSessionTokens(
        sessionId: String,
        rawRefreshToken: String,
        nowEpoch: Long = System.currentTimeMillis()
    ): Result<IssuedSessionTokens> {
        if (revokedSessionIds.contains(sessionId)) {
            return Result.failure(IllegalStateException("Session has been revoked and cannot be refreshed."))
        }
        val session = activeSessionsById[sessionId]
            ?: return Result.failure(IllegalStateException("Session not found."))

        if (session.isRevoked || nowEpoch > session.absoluteSessionExpiresAtEpoch) {
            revokeSession(sessionId, "Session exceeded absolute lifetime.")
            return Result.failure(IllegalStateException("Session has expired. Please sign in again."))
        }

        val candidateRefreshHash = SecuritySanitizer.hashOpaqueToken(rawRefreshToken)
        if (revokedTokenHashes.contains(candidateRefreshHash)) {
            // Replay of an already-rotated refresh token indicates potential token theft -> revoke session!
            revokeSession(sessionId, "Compromised refresh token replay detected.")
            return Result.failure(IllegalStateException("Security alert: Refresh token replay detected. Session revoked."))
        }

        if (!SecuritySanitizer.constantTimeEquals(candidateRefreshHash, session.refreshTokenHash)) {
            return Result.failure(IllegalArgumentException("Invalid refresh token."))
        }

        // Blacklist the old tokens so they can never be reused
        revokedTokenHashes.add(session.accessTokenHash)
        revokedTokenHashes.add(session.refreshTokenHash)

        val newAccessToken = "sf_at_${SecuritySanitizer.generateOpaqueCryptoToken(32)}"
        val newRefreshToken = "sf_rt_${SecuritySanitizer.generateOpaqueCryptoToken(32)}"
        val newAccessHash = SecuritySanitizer.hashOpaqueToken(newAccessToken)
        val newRefreshHash = SecuritySanitizer.hashOpaqueToken(newRefreshToken)
        val newAccessExpires = (nowEpoch + ACCESS_TOKEN_TTL_MS).coerceAtMost(session.absoluteSessionExpiresAtEpoch)

        val updatedSession = session.copy(
            accessTokenHash = newAccessHash,
            refreshTokenHash = newRefreshHash,
            accessTokenExpiresAtEpoch = newAccessExpires
        )
        activeSessionsById[sessionId] = updatedSession

        val rotated = IssuedSessionTokens(
            sessionId = sessionId,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            accessTokenExpiresAtEpoch = newAccessExpires,
            absoluteSessionExpiresAtEpoch = session.absoluteSessionExpiresAtEpoch
        )
        if (_latestIssuedTokens?.sessionId == sessionId) {
            _latestIssuedTokens = rotated
        }
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN_HASH, newAccessHash)
            ?.putString(KEY_REFRESH_TOKEN_HASH, newRefreshHash)
            ?.putLong(KEY_ACCESS_EXPIRES_EPOCH, newAccessExpires)
            ?.apply()

        return Result.success(rotated)
    }

    /**
     * Immediately revokes a specific session (e.g., when a device is lost or suspicious activity is detected).
     */
    fun revokeSession(sessionId: String, reason: String = "Manual or security session revocation") {
        revokedSessionIds.add(sessionId)
        val existing = activeSessionsById[sessionId]
        if (existing != null) {
            revokedTokenHashes.add(existing.accessTokenHash)
            revokedTokenHashes.add(existing.refreshTokenHash)
            activeSessionsById[sessionId] = existing.copy(isRevoked = true)
            logLoginAttempt(existing.email, LoginAttemptOutcome.SESSION_REVOKED, reason)
        }
        if (_latestIssuedTokens?.sessionId == sessionId || prefs?.getString(KEY_SESSION_ID, null) == sessionId) {
            logout()
        }
    }

    /**
     * Revokes all active sessions for a user account (e.g., after password reset or account compromise).
     */
    fun revokeAllSessionsForEmail(email: String, reason: String = "All account sessions revoked") {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val matchingIds = activeSessionsById.values
            .filter { it.email.equals(cleanEmail, ignoreCase = true) }
            .map { it.sessionId }
        matchingIds.forEach { id -> revokeSession(id, reason) }
    }

    // =========================================================================
    // 6. Step-Up Multi-Factor Authentication (MFA) for Admins & Seller Payouts
    // =========================================================================

    /**
     * Issues a 6-digit cryptographic Step-Up MFA challenge required for administrator actions
     * and sensitive seller operations (such as bank withdrawals or settlement bank changes).
     */
    fun issueMfaChallenge(
        email: String,
        scope: MfaOperationScope,
        nowEpoch: Long = System.currentTimeMillis()
    ): Result<Pair<MfaChallenge, String>> {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        if (!SecuritySanitizer.isValidEmailFormat(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Valid authenticated user email required for MFA."))
        }
        val mfaCode = SecuritySanitizer.generateSecureSixDigitCode()
        val challenge = MfaChallenge(
            challengeId = "mfa_${SecuritySanitizer.generateOpaqueCryptoToken(12)}",
            email = cleanEmail,
            maskedEmail = SecuritySanitizer.maskEmail(cleanEmail),
            scope = scope,
            codeHash = SecuritySanitizer.hashOpaqueToken(mfaCode),
            expiresAtEpoch = nowEpoch + MFA_CHALLENGE_EXPIRY_MS,
            failedAttempts = 0
        )
        activeMfaChallenges[challenge.challengeId] = challenge
        safeLogInfo("Issued step-up MFA challenge (${scope.name}) for ${challenge.maskedEmail}")
        return Result.success(challenge to mfaCode)
    }

    /**
     * Verifies a Step-Up MFA challenge in constant time and grants a 15-minute scoped MFA token.
     */
    fun verifyMfaChallenge(
        challengeId: String,
        enteredCode: String,
        nowEpoch: Long = System.currentTimeMillis()
    ): Result<MfaStepUpGrant> {
        val challenge = activeMfaChallenges[challengeId]
            ?: return Result.failure(IllegalStateException("MFA challenge not found or already consumed."))

        if (nowEpoch > challenge.expiresAtEpoch) {
            activeMfaChallenges.remove(challengeId)
            return Result.failure(IllegalArgumentException("MFA code has expired. Please request a new code."))
        }

        if (challenge.failedAttempts >= MAX_MFA_ATTEMPTS) {
            activeMfaChallenges.remove(challengeId)
            return Result.failure(IllegalStateException("Maximum MFA verification attempts exceeded. Challenge invalidated."))
        }

        val enteredHash = SecuritySanitizer.hashOpaqueToken(enteredCode.trim())
        if (!SecuritySanitizer.constantTimeEquals(enteredHash, challenge.codeHash)) {
            val nextAttempts = challenge.failedAttempts + 1
            if (nextAttempts >= MAX_MFA_ATTEMPTS) {
                activeMfaChallenges.remove(challengeId)
            } else {
                activeMfaChallenges[challengeId] = challenge.copy(failedAttempts = nextAttempts)
            }
            return Result.failure(IllegalArgumentException("Invalid MFA verification code."))
        }

        // Consume single-use challenge and issue scoped step-up grant
        activeMfaChallenges.remove(challengeId)
        val grant = MfaStepUpGrant(
            grantId = "grant_${SecuritySanitizer.generateOpaqueCryptoToken(16)}",
            email = challenge.email,
            scope = challenge.scope,
            issuedAtEpoch = nowEpoch,
            expiresAtEpoch = nowEpoch + MFA_GRANT_VALIDITY_MS
        )
        activeMfaGrants["${challenge.email}:${challenge.scope.name}"] = grant
        return Result.success(grant)
    }

    fun hasValidMfaGrant(
        email: String,
        scope: MfaOperationScope,
        nowEpoch: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val key = "$cleanEmail:${scope.name}"
        val grant = activeMfaGrants[key] ?: return false
        if (nowEpoch > grant.expiresAtEpoch) {
            activeMfaGrants.remove(key)
            return false
        }
        return true
    }

    fun loginAsDemoRole(role: UserRole): UserAccount {
        val account = when (role) {
            UserRole.ADMIN -> UserAccount(
                uid = "admin_master_001",
                name = "Solarfix Admin (Platform Ops)",
                email = "admin@solarfix.africa",
                role = UserRole.ADMIN,
                createdAtEpoch = System.currentTimeMillis(),
                safetyTermsAccepted = true
            )
            UserRole.TECHNICIAN -> UserAccount(
                uid = "tech_babatunde_002",
                name = "Engr. Babatunde (Verified Pro)",
                email = "technician@solarfix.africa",
                role = UserRole.TECHNICIAN,
                createdAtEpoch = System.currentTimeMillis(),
                safetyTermsAccepted = true
            )
            UserRole.VERIFIED_SELLER -> UserAccount(
                uid = "seller_nekes_004",
                name = "NEKES Energy Director (Verified Seller)",
                email = "seller@solarfix.africa",
                role = UserRole.VERIFIED_SELLER,
                createdAtEpoch = System.currentTimeMillis(),
                safetyTermsAccepted = true
            )
            UserRole.CUSTOMER -> UserAccount(
                uid = "cust_chidi_003",
                name = "Chidi Okafor (Solar Owner)",
                email = "chidi@solarfix.africa",
                role = UserRole.CUSTOMER,
                createdAtEpoch = System.currentTimeMillis(),
                safetyTermsAccepted = true
            )
        }
        issueAndPersistSession(account)
        _currentUser.value = account
        return account
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            safeLogError("Firebase signOut error")
        }
        val activeSessionId = _latestIssuedTokens?.sessionId ?: prefs?.getString(KEY_SESSION_ID, null)
        if (activeSessionId != null) {
            revokedSessionIds.add(activeSessionId)
            activeSessionsById[activeSessionId]?.let { sess ->
                revokedTokenHashes.add(sess.accessTokenHash)
                revokedTokenHashes.add(sess.refreshTokenHash)
            }
            activeSessionsById.remove(activeSessionId)
        }
        _latestIssuedTokens = null
        com.example.util.MobileSecurityAndMonitoringManager.clearSecurePlatformTokenVault()
        clearPersistedSessionPrefs()
        _currentUser.value = null
    }

    private fun clearPersistedSessionPrefs() {
        // SECURITY HARDENING (VULN-03): Clear only active session keys so stored Argon2id/PBKDF2
        // password hashes and brute-force lockout counters remain intact across logouts.
        prefs?.edit()
            ?.remove(KEY_UID)
            ?.remove(KEY_NAME)
            ?.remove(KEY_EMAIL)
            ?.remove(KEY_ROLE)
            ?.remove(KEY_IS_ADMIN)
            ?.remove(KEY_CREATED_EPOCH)
            ?.remove(KEY_SAFETY_ACCEPTED)
            ?.remove(KEY_EMAIL_VERIFIED)
            ?.remove(KEY_SESSION_ID)
            ?.remove(KEY_ACCESS_TOKEN_HASH)
            ?.remove(KEY_REFRESH_TOKEN_HASH)
            ?.remove(KEY_ACCESS_EXPIRES_EPOCH)
            ?.remove(KEY_SESSION_ABSOLUTE_EXPIRES_EPOCH)
            ?.apply()
    }

    fun recordSafetyConfirmation(accepted: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SAFETY_ACCEPTED, accepted)?.apply()
        _currentUser.value = _currentUser.value?.copy(safetyTermsAccepted = accepted)
    }

    suspend fun verifyAdministrativeRoute(): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        if (!isCurrentSessionValid()) return@withContext false
        val status = adminClaimHandler.verifyAdministrativeStatus(user, forceTokenRefresh = true)
        val isAuthorized = status is com.example.util.AdminVerificationStatus.Authorized
        if (isAuthorized && !user.isAdmin) {
            val updated = user.copy(
                isAdmin = true,
                role = UserRole.ADMIN
            )
            issueAndPersistSession(updated)
            _currentUser.value = updated
        }
        isAuthorized
    }
}
