package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.PropertyName

enum class UserRole(val displayName: String) {
    CUSTOMER("Solar System Owner"),
    VERIFIED_SELLER("Verified Equipment Seller"),
    TECHNICIAN("Certified Technician"),
    ADMIN("Platform Administrator");

    val isCustomer: Boolean get() = this == CUSTOMER
    val isSeller: Boolean get() = this == VERIFIED_SELLER
    val isTechnician: Boolean get() = this == TECHNICIAN
    val isAdmin: Boolean get() = this == ADMIN

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()) {
                "ADMIN" -> ADMIN
                "VERIFIED_SELLER", "SELLER" -> VERIFIED_SELLER
                "TECHNICIAN", "TECH" -> TECHNICIAN
                else -> CUSTOMER
            }
        }
    }
}

/**
 * Domain representation of an authenticated user in Solarfix.
 */
data class UserAccount(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.CUSTOMER,
    val isAdmin: Boolean = (role == UserRole.ADMIN),
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val safetyTermsAccepted: Boolean = true,
    val isEmailVerified: Boolean = true
) {
    val isCustomer: Boolean get() = role == UserRole.CUSTOMER
    val isSeller: Boolean get() = role == UserRole.VERIFIED_SELLER
    val isTechnician: Boolean get() = role == UserRole.TECHNICIAN
    fun canAccessSellerHub(): Boolean = isSeller || isAdmin
    fun canAccessAdmin(): Boolean = isAdmin

    fun toFirestoreUser(): FirestoreUser {
        return FirestoreUser(
            uid = uid,
            name = name,
            email = email,
            role = role.name,
            isAdmin = isAdmin,
            createdAtEpoch = createdAtEpoch,
            safetyTermsAccepted = safetyTermsAccepted,
            isEmailVerified = isEmailVerified,
            updatedAtEpoch = System.currentTimeMillis()
        )
    }

    fun toFirestoreMap(): Map<String, Any> = toFirestoreUser().toMap()
}

/**
 * Firestore document entity stored in the "users" collection (users/{uid}).
 * Includes 'isAdmin' boolean to enforce administrative privileges and zero-trust security.
 *
 * All parameters have default values to satisfy Firebase Firestore deserialization requirements.
 */
data class FirestoreUser(
    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("role")
    @set:PropertyName("role")
    var role: String = UserRole.CUSTOMER.name,

    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    @get:PropertyName("createdAtEpoch")
    @set:PropertyName("createdAtEpoch")
    var createdAtEpoch: Long = 0L,

    @get:PropertyName("safetyTermsAccepted")
    @set:PropertyName("safetyTermsAccepted")
    var safetyTermsAccepted: Boolean = true,

    @get:PropertyName("isEmailVerified")
    @set:PropertyName("isEmailVerified")
    var isEmailVerified: Boolean = true,

    @get:PropertyName("updatedAtEpoch")
    @set:PropertyName("updatedAtEpoch")
    var updatedAtEpoch: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "isAdmin" to isAdmin,
            "createdAtEpoch" to if (createdAtEpoch > 0L) createdAtEpoch else System.currentTimeMillis(),
            "safetyTermsAccepted" to safetyTermsAccepted,
            "isEmailVerified" to isEmailVerified,
            "updatedAtEpoch" to System.currentTimeMillis()
        )
    }

    fun toUserAccount(): UserAccount {
        val parsedRole = UserRole.fromString(role)
        return UserAccount(
            uid = uid,
            name = name,
            email = email,
            role = parsedRole,
            isAdmin = isAdmin || (parsedRole == UserRole.ADMIN),
            createdAtEpoch = if (createdAtEpoch > 0L) createdAtEpoch else System.currentTimeMillis(),
            safetyTermsAccepted = safetyTermsAccepted,
            isEmailVerified = isEmailVerified
        )
    }

    companion object {
        const val COLLECTION_NAME = "users"
        const val FIELD_UID = "uid"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_IS_ADMIN = "isAdmin"
        const val FIELD_CREATED_AT_EPOCH = "createdAtEpoch"
        const val FIELD_SAFETY_TERMS_ACCEPTED = "safetyTermsAccepted"
        const val FIELD_IS_EMAIL_VERIFIED = "isEmailVerified"
        const val FIELD_UPDATED_AT_EPOCH = "updatedAtEpoch"

        fun fromDocument(doc: DocumentSnapshot): FirestoreUser? {
            if (!doc.exists()) return null
            return try {
                doc.toObject(FirestoreUser::class.java)?.copy(uid = doc.id)
            } catch (_: Exception) {
                // Fallback direct map extraction
                val data = doc.data ?: return null
                fromMap(doc.id, data)
            }
        }

        fun fromMap(docId: String, data: Map<String, Any?>): FirestoreUser {
            val roleStr = (data[FIELD_ROLE] as? String) ?: UserRole.CUSTOMER.name
            val adminFlag = (data[FIELD_IS_ADMIN] as? Boolean)
                ?: (data["admin"] as? Boolean)
                ?: (roleStr.equals(UserRole.ADMIN.name, ignoreCase = true))

            return FirestoreUser(
                uid = (data[FIELD_UID] as? String) ?: docId,
                name = (data[FIELD_NAME] as? String).orEmpty(),
                email = (data[FIELD_EMAIL] as? String).orEmpty(),
                role = roleStr,
                isAdmin = adminFlag,
                createdAtEpoch = (data[FIELD_CREATED_AT_EPOCH] as? Number)?.toLong() ?: 0L,
                safetyTermsAccepted = (data[FIELD_SAFETY_TERMS_ACCEPTED] as? Boolean) ?: true,
                isEmailVerified = (data[FIELD_IS_EMAIL_VERIFIED] as? Boolean) ?: true,
                updatedAtEpoch = (data[FIELD_UPDATED_AT_EPOCH] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
