package com.example.util

import android.util.Log
import com.example.data.model.FirestoreUser
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "FirebaseAuthAdminHandler"

/**
 * Result of an administrative verification check.
 */
sealed class AdminVerificationStatus {
    data class Authorized(
        val uid: String,
        val email: String,
        val source: AdminClaimSource,
        val verifiedAtEpoch: Long = System.currentTimeMillis()
    ) : AdminVerificationStatus()

    data class Denied(
        val reason: String,
        val email: String? = null
    ) : AdminVerificationStatus()

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : AdminVerificationStatus()
}

/**
 * Indicates which trusted mechanism verified the administrator status.
 */
enum class AdminClaimSource(val description: String) {
    AUTH_CUSTOM_CLAIM("Firebase Auth Token Custom Claim (admin=true)"),
    FIRESTORE_IS_ADMIN("Firestore User Document (isAdmin=true)"),
    PLATFORM_OWNER("Verified Platform Owner Clearance")
}

/**
 * Handler responsible for inspecting Firebase Auth custom claims,
 * cross-verifying Firestore 'isAdmin' document flags, and securing
 * administrative routes across the application.
 */
class FirebaseAuthAdminHandler(
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null },
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
) {

    companion object {
        const val CLAIM_ADMIN = "admin"
        const val CLAIM_IS_ADMIN = "isAdmin"
        const val CLAIM_ROLE = "role"

        const val PLATFORM_OWNER_EMAIL = "nekeskingsley@gmail.com"
        const val DEFAULT_ADMIN_EMAIL = "admin@solarfix.africa"

        /**
         * Pure function to evaluate if an auth claims map contains valid administrator privileges.
         * Used by both runtime checks and unit test suites.
         */
        fun evaluateCustomClaims(claims: Map<String, Any?>?): Boolean {
            if (claims == null) return false
            val hasAdminBool = claims[CLAIM_ADMIN] == true
            val hasIsAdminBool = claims[CLAIM_IS_ADMIN] == true
            val hasRoleAdmin = (claims[CLAIM_ROLE] as? String)?.equals("admin", ignoreCase = true) == true
            return hasAdminBool || hasIsAdminBool || hasRoleAdmin
        }

        /**
         * Verifies if the email belongs to the app owner or default system administrator.
         * SECURITY HARDENING (VULN-01): Enforces exact canonical equality and rejects
         * prefix/substring matches (e.g., nekeskingsley@attacker.com or evil_admin@domain.com).
         */
        fun isOwnerOrSystemAdminEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val lower = email.trim().lowercase()
            return lower == PLATFORM_OWNER_EMAIL || lower == DEFAULT_ADMIN_EMAIL
        }
    }

    /**
     * Inspects the current Firebase User's ID token and retrieves custom claims.
     */
    suspend fun getCustomClaims(
        user: FirebaseUser,
        forceRefresh: Boolean = false
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            val result = user.getIdToken(forceRefresh).await()
            result.claims
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve ID token claims")
            emptyMap()
        }
    }

    /**
     * Verifies if the authenticated user has an active administrative custom claim in their token.
     */
    suspend fun verifyCustomAuthClaim(
        user: FirebaseUser,
        forceRefresh: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val claims = getCustomClaims(user, forceRefresh)
        val isClaimValid = evaluateCustomClaims(claims)
        Log.d(TAG, "Custom Auth Claim verified=$isClaimValid")
        isClaimValid
    }

    /**
     * Queries the Firestore document at `users/{uid}` to verify if `isAdmin == true`.
     */
    suspend fun verifyFirestoreAdminStatus(uid: String): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        if (uid.isBlank() || uid.contains("/")) return@withContext false
        try {
            val snapshot = db.collection(FirestoreUser.COLLECTION_NAME).document(uid).get().await()
            if (!snapshot.exists()) {
                return@withContext false
            }
            val firestoreUser = FirestoreUser.fromDocument(snapshot)
            val isAdminInDoc = firestoreUser?.isAdmin == true ||
                    firestoreUser?.role.equals(UserRole.ADMIN.name, ignoreCase = true)
            isAdminInDoc
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch Firestore admin status")
            false
        }
    }

    /**
     * Writes or updates the User record in Firestore `users/{uid}`.
     * SECURITY HARDENING (VULN-06): Prevents client-side privilege escalation by stripping
     * unverified `isAdmin = true` or `ADMIN` role flags unless the account matches the
     * verified platform owner/system administrator whitelist.
     */
    suspend fun syncUserToFirestore(account: UserAccount): Result<FirestoreUser> = withContext(Dispatchers.IO) {
        val db = firestore
        val isVerifiedOwnerOrAdmin = isOwnerOrSystemAdminEmail(account.email)
        val safeRole = if (!isVerifiedOwnerOrAdmin && account.role == UserRole.ADMIN) {
            UserRole.CUSTOMER
        } else {
            account.role
        }
        val safeAccount = account.copy(
            name = SecuritySanitizer.sanitizeText(account.name, maxLength = 120),
            role = safeRole,
            isAdmin = isVerifiedOwnerOrAdmin && account.isAdmin
        )
        val firestoreUser = safeAccount.toFirestoreUser()

        if (db == null) {
            return@withContext Result.success(firestoreUser)
        }

        try {
            val docRef = db.collection(FirestoreUser.COLLECTION_NAME).document(safeAccount.uid)
            docRef.set(firestoreUser.toMap(), SetOptions.merge()).await()
            Log.i(TAG, "Synced user ${SecuritySanitizer.maskEmail(safeAccount.email)} to Firestore")
            Result.success(firestoreUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing user to Firestore")
            Result.failure(e)
        }
    }

    /**
     * Comprehensive multi-tier verification to secure app-wide administrative routes:
     * 1. Checks Platform Owner whitelist (nekeskingsley@gmail.com).
     * 2. Checks Firebase Auth Custom ID Token claims (admin=true, isAdmin=true).
     * 3. Checks Firestore User Document `isAdmin` field in `users/{uid}`.
     * 4. Checks local user domain account privileges.
     */
    suspend fun verifyAdministrativeStatus(
        userAccount: UserAccount? = null,
        forceTokenRefresh: Boolean = false
    ): AdminVerificationStatus = withContext(Dispatchers.IO) {
        val fbUser = auth?.currentUser
        val effectiveEmail = userAccount?.email ?: fbUser?.email

        // 1. App Owner / System Admin Clearance
        if (isOwnerOrSystemAdminEmail(effectiveEmail)) {
            val uid = userAccount?.uid ?: fbUser?.uid ?: "owner_admin"
            return@withContext AdminVerificationStatus.Authorized(
                uid = uid,
                email = effectiveEmail.orEmpty(),
                source = AdminClaimSource.PLATFORM_OWNER
            )
        }

        // 2. Firebase Auth Token Custom Claim Check
        if (fbUser != null) {
            val hasCustomClaim = verifyCustomAuthClaim(fbUser, forceRefresh = forceTokenRefresh)
            if (hasCustomClaim) {
                return@withContext AdminVerificationStatus.Authorized(
                    uid = fbUser.uid,
                    email = fbUser.email.orEmpty(),
                    source = AdminClaimSource.AUTH_CUSTOM_CLAIM
                )
            }

            // 3. Firestore Document 'isAdmin' Check
            val isFirestoreAdmin = verifyFirestoreAdminStatus(fbUser.uid)
            if (isFirestoreAdmin) {
                return@withContext AdminVerificationStatus.Authorized(
                    uid = fbUser.uid,
                    email = fbUser.email.orEmpty(),
                    source = AdminClaimSource.FIRESTORE_IS_ADMIN
                )
            }
        }

        // 4. Fallback check for session model
        if (userAccount != null) {
            val isFirestoreAdmin = verifyFirestoreAdminStatus(userAccount.uid)
            if (isFirestoreAdmin || userAccount.isAdmin) {
                return@withContext AdminVerificationStatus.Authorized(
                    uid = userAccount.uid,
                    email = userAccount.email,
                    source = AdminClaimSource.FIRESTORE_IS_ADMIN
                )
            }
        }

        AdminVerificationStatus.Denied(
            reason = "Administrative clearance required. Custom Auth claim or Firestore 'isAdmin' verification failed.",
            email = effectiveEmail
        )
    }

    /**
     * Guard helper for administrative routes and actions.
     */
    suspend fun requireAdmin(
        currentUser: UserAccount?,
        onAuthorized: suspend () -> Unit,
        onDenied: (String) -> Unit
    ) {
        when (val result = verifyAdministrativeStatus(currentUser)) {
            is AdminVerificationStatus.Authorized -> {
                onAuthorized()
            }
            is AdminVerificationStatus.Denied -> {
                onDenied(result.reason)
            }
            is AdminVerificationStatus.Error -> {
                onDenied("Verification error: ${result.message}")
            }
        }
    }
}
