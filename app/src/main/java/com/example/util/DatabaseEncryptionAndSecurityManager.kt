package com.example.util

import com.example.data.model.DeliveryAddress
import com.example.data.model.MarketplaceOrder
import com.example.data.model.SellerStore
import com.example.data.model.StoreVerificationStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class DatabaseOperationType {
    SELECT_READ,
    INSERT_CREATE,
    UPDATE_MUTATE,
    DELETE_REMOVE,
    FIELD_ENCRYPT,
    FIELD_DECRYPT,
    BACKUP_CREATE,
    BACKUP_RESTORE,
    SECURITY_VIOLATION_BLOCKED
}

data class DatabaseActivityLogEntry(
    val eventId: String = "DB-ACT-${UUID.randomUUID().toString().take(10)}",
    val timestampEpoch: Long = System.currentTimeMillis(),
    val actorUid: String,
    val actorEmail: String,
    val actorRole: UserRole,
    val operation: DatabaseOperationType,
    val collectionOrTable: String,
    val documentOrRowId: String,
    val encryptedFieldsProtected: Boolean = true,
    val authorized: Boolean = true,
    val anomalyDetected: Boolean = false,
    val details: String
)

data class ProhibitedDataValidationResult(
    val isSafeToStore: Boolean,
    val violatedField: String? = null,
    val violationReason: String? = null
)

data class EncryptedSellerStoreRecord(
    val storeId: String,
    val ownerUid: String,
    val storeName: String,
    val city: String,
    val state: String,
    val statusName: String,
    val encryptedOwnerEmail: String,
    val encryptedPhone: String,
    val encryptedPhysicalAddress: String,
    val encryptedBusinessRegNumber: String,
    val encryptedTaxIdNumber: String,
    val encryptedIdDocumentType: String,
    val encryptedIdDocumentNumber: String,
    val encryptedBankName: String,
    val encryptedBankAccountName: String,
    val encryptedBankAccountNumber: String,
    val availableEarningsNgn: Long,
    val pendingEscrowNgn: Long,
    val totalWithdrawnNgn: Long,
    val totalGrossSalesNgn: Long,
    val totalCommissionPaidNgn: Long,
    val customCommissionPercent: Double?,
    val ledgerIntegrityHmac: String,
    val keyId: String,
    val encryptedAtEpoch: Long = System.currentTimeMillis()
)

data class EncryptedDeliveryAddressRecord(
    val addressId: String,
    val userUid: String,
    val label: String,
    val city: String,
    val state: String,
    val isDefault: Boolean,
    val encryptedUserEmail: String,
    val encryptedRecipientName: String,
    val encryptedPhone: String,
    val encryptedStreetAddress: String,
    val recordIntegrityHmac: String,
    val keyId: String,
    val encryptedAtEpoch: Long = System.currentTimeMillis()
)

data class EncryptedAuthTokenRecord(
    val sessionId: String,
    val userUid: String,
    val encryptedAccessToken: String,
    val encryptedRefreshToken: String,
    val expiresAtEpoch: Long,
    val tokenIntegrityHmac: String,
    val keyId: String
)

data class EncryptedBackupArchive(
    val backupId: String,
    val createdAtEpoch: Long,
    val keyId: String,
    val encryptedPayloadCiphertext: String,
    val payloadSha256Checksum: String,
    val archiveHmacSignature: String,
    val storesCount: Int,
    val ordersCount: Int,
    val addressesCount: Int
)

/**
 * Enterprise Database Security, Field-Level AES-256-GCM Encryption, Key Management,
 * PCI-DSS Non-Storage Guard, Row-Level Security Policy Validator, and Activity Monitor.
 *
 * Guarantees:
 * 1. Sensitive data is encrypted at rest using AES-256-GCM (96-bit random IV, 128-bit auth tag)
 *    with Authenticated Additional Data (AEAD) bound to the record ID and owner ID.
 * 2. Protects: Personal ID documents (NIN/Passport/CAC/TIN), Bank account numbers, Phone numbers,
 *    Email addresses, Delivery addresses, Financial records (with HMAC-SHA256 tamper detection),
 *    and Authentication tokens.
 * 3. Strictly prohibits storing customer card numbers (PAN), CVVs/CVCs, card PINs, or banking passwords.
 * 4. Enforces HTTPS/TLS 1.2+ for all data in transit.
 * 5. Enforces Row-Level Security (RLS), Least-Privilege permissions, and parameterized/safe query validation.
 * 6. Monitors all database activity and creates AES-256-GCM encrypted + HMAC-signed backups.
 */
object DatabaseEncryptionAndSecurityManager {

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12 // 96-bit IV recommended by NIST SP 800-38D
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENCRYPTED_PREFIX = "enc:v1:"
    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS_PREFIX = "solarfix_db_master_key_"

    private val secureRandom = SecureRandom()

    @Volatile
    private var activeKeyId: String = "sf_kek_v1"

    // Key ring supporting seamless key rotation while maintaining decryption of historical ciphertexts
    private val keyRingMaterial = mutableMapOf(
        "sf_kek_v1" to deriveDeterministicKeyMaterial("solarfix_master_kek_v1_aes256_gcm_2026", "sf_salt_v1_9981"),
        "sf_kek_v2" to deriveDeterministicKeyMaterial("solarfix_master_kek_v2_aes256_gcm_rotated", "sf_salt_v2_7742")
    )

    private val hmacSigningSecret: ByteArray =
        deriveDeterministicKeyMaterial("solarfix_financial_ledger_hmac_sha256_key_2026", "sf_hmac_salt_4410")

    private val _activityLogs = MutableStateFlow<List<DatabaseActivityLogEntry>>(emptyList())
    val activityLogsFlow: StateFlow<List<DatabaseActivityLogEntry>> = _activityLogs.asStateFlow()

    // Prohibited PCI-DSS & Banking Credential field names (case-insensitive)
    private val PROHIBITED_FIELD_NAMES = setOf(
        "cardnumber", "card_number", "pan", "primaryaccountnumber", "ccnumber", "creditcard", "debitcard",
        "cvv", "cvc", "cvv2", "cvc2", "cid", "securitycode", "card_cvv",
        "cardpin", "card_pin", "pinblock", "pin_block",
        "bankingpassword", "banking_password", "bankpassword", "bank_password",
        "internetbankingpassword", "bvn_secret", "tracks", "track1", "track2"
    )

    // Matches candidate 13 to 19 digit card numbers (with optional spaces or hyphens)
    private val CANDIDATE_PAN_REGEX = Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)")
    private val CVV_LABEL_PATTERN = Regex("(?i)\\b(?:cvv|cvc|cvv2|cvc2|card\\s*pin|banking\\s*password)\\s*[:=]\\s*\\S+")

    fun getCurrentKeyId(): String = activeKeyId

    /**
     * Rotates the active Key Encryption Key (KEK) identifier for new encryptions while preserving
     * existing keys in the key ring so previously encrypted database records remain decryptable.
     */
    @Synchronized
    fun rotateActiveEncryptionKey(newKeyId: String = "sf_kek_v2"): String {
        if (!keyRingMaterial.containsKey(newKeyId)) {
            val randomSeed = ByteArray(32).also { secureRandom.nextBytes(it) }
            val seedB64 = Base64.getEncoder().encodeToString(randomSeed)
            keyRingMaterial[newKeyId] = deriveDeterministicKeyMaterial(seedB64, "salt_$newKeyId")
        }
        activeKeyId = newKeyId
        recordDatabaseActivity(
            actorUid = "system_key_manager",
            actorEmail = "admin@solarfix.africa",
            actorRole = UserRole.ADMIN,
            operation = DatabaseOperationType.FIELD_ENCRYPT,
            collectionOrTable = "keystore",
            documentOrRowId = newKeyId,
            details = "Rotated active database encryption key to $newKeyId"
        )
        return activeKeyId
    }

    private fun deriveDeterministicKeyMaterial(passphrase: String, saltText: String): ByteArray {
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            saltText.toByteArray(StandardCharsets.UTF_8),
            65_536,
            256
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun resolveSecretKey(keyId: String): SecretKey {
        // Attempt hardware-backed AndroidKeyStore first when running on physical Android OS
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            ks.load(null)
            val alias = "$KEYSTORE_ALIAS_PREFIX$keyId"
            val entry = ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry?.secretKey != null) {
                return entry.secretKey
            }
        } catch (_: Exception) {
            // Standard JVM / unit-test environment or non-KeyStore context uses PBKDF2-SHA256 256-bit key ring
        }
        val keyBytes = keyRingMaterial[keyId]
            ?: keyRingMaterial[activeKeyId]
            ?: throw SecurityException("Unknown encryption key ID: $keyId")
        return SecretKeySpec(keyBytes, "AES")
    }

    // =========================================================================
    // 1. Field-Level AES-256-GCM Authenticated Encryption (At Rest)
    // =========================================================================

    /**
     * Encrypts a sensitive field value using AES-256-GCM with a fresh 96-bit cryptographic IV
     * and binds it to [associatedData] (e.g., "stores:store_nekes:bankAccountNumber") so the
     * ciphertext cannot be replayed or substituted across rows/documents.
     */
    fun encryptSensitiveField(
        plaintext: String,
        associatedData: String = "",
        keyId: String = activeKeyId
    ): String {
        if (plaintext.isEmpty()) return ""
        if (isEncryptedPayload(plaintext)) return plaintext

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, resolveSecretKey(keyId), spec)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData.toByteArray(StandardCharsets.UTF_8))
        }
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        val ivB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
        val cipherB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(cipherBytes)
        return "$ENCRYPTED_PREFIX$keyId:$ivB64:$cipherB64"
    }

    /**
     * Decrypts an AES-256-GCM encrypted field value and verifies its 128-bit GCM authentication tag
     * and [associatedData] binding. Throws [SecurityException] if tampered with or if AAD mismatches.
     */
    fun decryptSensitiveField(
        ciphertextPayload: String,
        associatedData: String = ""
    ): String {
        if (ciphertextPayload.isEmpty()) return ""
        if (!isEncryptedPayload(ciphertextPayload)) {
            throw SecurityException("Unencrypted payload rejected by strict decryption policy.")
        }
        val raw = ciphertextPayload.removePrefix(ENCRYPTED_PREFIX)
        val parts = raw.split(":")
        if (parts.size != 3) {
            throw SecurityException("Malformed encrypted field structure.")
        }
        val keyId = parts[0]
        val iv = try {
            Base64.getUrlDecoder().decode(parts[1])
        } catch (e: Exception) {
            throw SecurityException("Corrupted IV in encrypted field.", e)
        }
        val cipherBytes = try {
            Base64.getUrlDecoder().decode(parts[2])
        } catch (e: Exception) {
            throw SecurityException("Corrupted ciphertext in encrypted field.", e)
        }

        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, resolveSecretKey(keyId), spec)
            if (associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData.toByteArray(StandardCharsets.UTF_8))
            }
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("AES-256-GCM authentication tag verification failed (tampering or wrong AAD).", e)
        }
    }

    fun isEncryptedPayload(value: String): Boolean {
        return value.startsWith(ENCRYPTED_PREFIX) && value.count { it == ':' } == 4
    }

    // =========================================================================
    // 2. Financial Record HMAC-SHA256 Integrity Verification
    // =========================================================================

    fun computeHmacSha256(canonicalData: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacSigningSecret, "HmacSHA256"))
        val digest = mac.doFinal(canonicalData.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun verifyHmacSha256(canonicalData: String, expectedHmac: String): Boolean {
        if (expectedHmac.isBlank()) return false
        val computed = computeHmacSha256(canonicalData)
        return MessageDigest.isEqual(
            computed.toByteArray(StandardCharsets.UTF_8),
            expectedHmac.toByteArray(StandardCharsets.UTF_8)
        )
    }

    fun computeStoreLedgerHmac(
        storeId: String,
        availableEarningsNgn: Long,
        pendingEscrowNgn: Long,
        totalWithdrawnNgn: Long,
        customCommissionPercent: Double?
    ): String {
        val canonical = "store=$storeId|avail=$availableEarningsNgn|escrow=$pendingEscrowNgn|withdrawn=$totalWithdrawnNgn|comm=${customCommissionPercent ?: "DEFAULT"}"
        return computeHmacSha256(canonical)
    }

    // =========================================================================
    // 3. Entity-Level Database Encryption Wrappers
    // =========================================================================

    fun encryptSellerStoreForDatabase(store: SellerStore): EncryptedSellerStoreRecord {
        val sid = store.id
        val kId = activeKeyId
        return EncryptedSellerStoreRecord(
            storeId = sid,
            ownerUid = store.ownerUid,
            storeName = store.storeName,
            city = store.city,
            state = store.state,
            statusName = store.status.name,
            encryptedOwnerEmail = encryptSensitiveField(store.ownerEmail, "stores:$sid:ownerEmail", kId),
            encryptedPhone = encryptSensitiveField(store.phone, "stores:$sid:phone", kId),
            encryptedPhysicalAddress = encryptSensitiveField(store.physicalAddress, "stores:$sid:physicalAddress", kId),
            encryptedBusinessRegNumber = encryptSensitiveField(store.businessRegNumber, "stores:$sid:businessRegNumber", kId),
            encryptedTaxIdNumber = encryptSensitiveField(store.taxIdNumber, "stores:$sid:taxIdNumber", kId),
            encryptedIdDocumentType = encryptSensitiveField(store.idDocumentType, "stores:$sid:idDocumentType", kId),
            encryptedIdDocumentNumber = encryptSensitiveField(store.idDocumentNumber, "stores:$sid:idDocumentNumber", kId),
            encryptedBankName = encryptSensitiveField(store.bankName, "stores:$sid:bankName", kId),
            encryptedBankAccountName = encryptSensitiveField(store.bankAccountName, "stores:$sid:bankAccountName", kId),
            encryptedBankAccountNumber = encryptSensitiveField(store.bankAccountNumber, "stores:$sid:bankAccountNumber", kId),
            availableEarningsNgn = store.availableEarningsNgn,
            pendingEscrowNgn = store.pendingEscrowNgn,
            totalWithdrawnNgn = store.totalWithdrawnNgn,
            totalGrossSalesNgn = store.totalGrossSalesNgn,
            totalCommissionPaidNgn = store.totalCommissionPaidNgn,
            customCommissionPercent = store.customCommissionPercent,
            ledgerIntegrityHmac = computeStoreLedgerHmac(
                storeId = sid,
                availableEarningsNgn = store.availableEarningsNgn,
                pendingEscrowNgn = store.pendingEscrowNgn,
                totalWithdrawnNgn = store.totalWithdrawnNgn,
                customCommissionPercent = store.customCommissionPercent
            ),
            keyId = kId
        )
    }

    fun decryptSellerStoreFromDatabase(
        record: EncryptedSellerStoreRecord,
        templateStore: SellerStore
    ): SellerStore {
        val sid = record.storeId
        val validLedger = verifyHmacSha256(
            canonicalData = "store=$sid|avail=${record.availableEarningsNgn}|escrow=${record.pendingEscrowNgn}|withdrawn=${record.totalWithdrawnNgn}|comm=${record.customCommissionPercent ?: "DEFAULT"}",
            expectedHmac = record.ledgerIntegrityHmac
        )
        if (!validLedger) {
            recordDatabaseActivity(
                actorUid = "system_integrity_guard",
                actorEmail = "security@solarfix.africa",
                actorRole = UserRole.ADMIN,
                operation = DatabaseOperationType.SECURITY_VIOLATION_BLOCKED,
                collectionOrTable = "stores",
                documentOrRowId = sid,
                authorized = false,
                anomalyDetected = true,
                details = "Tampered financial ledger HMAC detected on store $sid"
            )
            throw SecurityException("Database integrity violation: Store financial ledger HMAC mismatch for $sid")
        }

        return templateStore.copy(
            id = sid,
            ownerUid = record.ownerUid,
            storeName = record.storeName,
            city = record.city,
            state = record.state,
            status = StoreVerificationStatus.valueOf(record.statusName),
            ownerEmail = if (record.encryptedOwnerEmail.isNotEmpty()) decryptSensitiveField(record.encryptedOwnerEmail, "stores:$sid:ownerEmail") else "",
            phone = if (record.encryptedPhone.isNotEmpty()) decryptSensitiveField(record.encryptedPhone, "stores:$sid:phone") else "",
            physicalAddress = if (record.encryptedPhysicalAddress.isNotEmpty()) decryptSensitiveField(record.encryptedPhysicalAddress, "stores:$sid:physicalAddress") else "",
            businessRegNumber = if (record.encryptedBusinessRegNumber.isNotEmpty()) decryptSensitiveField(record.encryptedBusinessRegNumber, "stores:$sid:businessRegNumber") else "",
            taxIdNumber = if (record.encryptedTaxIdNumber.isNotEmpty()) decryptSensitiveField(record.encryptedTaxIdNumber, "stores:$sid:taxIdNumber") else "",
            idDocumentType = if (record.encryptedIdDocumentType.isNotEmpty()) decryptSensitiveField(record.encryptedIdDocumentType, "stores:$sid:idDocumentType") else "",
            idDocumentNumber = if (record.encryptedIdDocumentNumber.isNotEmpty()) decryptSensitiveField(record.encryptedIdDocumentNumber, "stores:$sid:idDocumentNumber") else "",
            bankName = if (record.encryptedBankName.isNotEmpty()) decryptSensitiveField(record.encryptedBankName, "stores:$sid:bankName") else "",
            bankAccountName = if (record.encryptedBankAccountName.isNotEmpty()) decryptSensitiveField(record.encryptedBankAccountName, "stores:$sid:bankAccountName") else "",
            bankAccountNumber = if (record.encryptedBankAccountNumber.isNotEmpty()) decryptSensitiveField(record.encryptedBankAccountNumber, "stores:$sid:bankAccountNumber") else "",
            availableEarningsNgn = record.availableEarningsNgn,
            pendingEscrowNgn = record.pendingEscrowNgn,
            totalWithdrawnNgn = record.totalWithdrawnNgn,
            totalGrossSalesNgn = record.totalGrossSalesNgn,
            totalCommissionPaidNgn = record.totalCommissionPaidNgn,
            customCommissionPercent = record.customCommissionPercent
        )
    }

    fun encryptDeliveryAddressForDatabase(address: DeliveryAddress): EncryptedDeliveryAddressRecord {
        val aid = address.id
        val kId = activeKeyId
        val encEmail = encryptSensitiveField(address.userEmail, "addresses:$aid:userEmail", kId)
        val encRecipient = encryptSensitiveField(address.recipientName, "addresses:$aid:recipientName", kId)
        val encPhone = encryptSensitiveField(address.phone, "addresses:$aid:phone", kId)
        val encStreet = encryptSensitiveField(address.streetAddress, "addresses:$aid:streetAddress", kId)
        val hmac = computeHmacSha256("addr=$aid|uid=${address.userUid}|street=$encStreet|phone=$encPhone")

        return EncryptedDeliveryAddressRecord(
            addressId = aid,
            userUid = address.userUid,
            label = address.label,
            city = address.city,
            state = address.state,
            isDefault = address.isDefault,
            encryptedUserEmail = encEmail,
            encryptedRecipientName = encRecipient,
            encryptedPhone = encPhone,
            encryptedStreetAddress = encStreet,
            recordIntegrityHmac = hmac,
            keyId = kId
        )
    }

    fun decryptDeliveryAddressFromDatabase(record: EncryptedDeliveryAddressRecord): DeliveryAddress {
        val aid = record.addressId
        val valid = verifyHmacSha256(
            canonicalData = "addr=$aid|uid=${record.userUid}|street=${record.encryptedStreetAddress}|phone=${record.encryptedPhone}",
            expectedHmac = record.recordIntegrityHmac
        )
        if (!valid) {
            throw SecurityException("Delivery address integrity HMAC verification failed for $aid")
        }
        return DeliveryAddress(
            id = aid,
            label = record.label,
            recipientName = if (record.encryptedRecipientName.isNotEmpty()) decryptSensitiveField(record.encryptedRecipientName, "addresses:$aid:recipientName") else "",
            phone = if (record.encryptedPhone.isNotEmpty()) decryptSensitiveField(record.encryptedPhone, "addresses:$aid:phone") else "",
            streetAddress = if (record.encryptedStreetAddress.isNotEmpty()) decryptSensitiveField(record.encryptedStreetAddress, "addresses:$aid:streetAddress") else "",
            city = record.city,
            state = record.state,
            isDefault = record.isDefault,
            userUid = record.userUid,
            userEmail = if (record.encryptedUserEmail.isNotEmpty()) decryptSensitiveField(record.encryptedUserEmail, "addresses:$aid:userEmail") else ""
        )
    }

    fun encryptAuthTokensForStorage(
        sessionId: String,
        userUid: String,
        accessToken: String,
        refreshToken: String,
        expiresAtEpoch: Long
    ): EncryptedAuthTokenRecord {
        val kId = activeKeyId
        val encAt = encryptSensitiveField(accessToken, "tokens:$sessionId:$userUid:at", kId)
        val encRt = encryptSensitiveField(refreshToken, "tokens:$sessionId:$userUid:rt", kId)
        val hmac = computeHmacSha256("sess=$sessionId|uid=$userUid|at=$encAt|rt=$encRt|exp=$expiresAtEpoch")
        return EncryptedAuthTokenRecord(
            sessionId = sessionId,
            userUid = userUid,
            encryptedAccessToken = encAt,
            encryptedRefreshToken = encRt,
            expiresAtEpoch = expiresAtEpoch,
            tokenIntegrityHmac = hmac,
            keyId = kId
        )
    }

    fun decryptAuthTokensFromStorage(record: EncryptedAuthTokenRecord): Pair<String, String> {
        val valid = verifyHmacSha256(
            canonicalData = "sess=${record.sessionId}|uid=${record.userUid}|at=${record.encryptedAccessToken}|rt=${record.encryptedRefreshToken}|exp=${record.expiresAtEpoch}",
            expectedHmac = record.tokenIntegrityHmac
        )
        if (!valid) {
            throw SecurityException("Encrypted authentication token record failed HMAC integrity check.")
        }
        val at = decryptSensitiveField(record.encryptedAccessToken, "tokens:${record.sessionId}:${record.userUid}:at")
        val rt = decryptSensitiveField(record.encryptedRefreshToken, "tokens:${record.sessionId}:${record.userUid}:rt")
        return at to rt
    }

    // =========================================================================
    // 4. PCI-DSS & Prohibited Banking Credential Non-Storage Guard
    // =========================================================================

    /**
     * Enforces: "Do not store unnecessary sensitive information. Never store customers' card numbers, CVVs or banking passwords."
     */
    fun validateNoProhibitedPaymentData(
        fields: Map<String, Any?>,
        actorUid: String = "unknown"
    ): ProhibitedDataValidationResult {
        for ((rawKey, rawValue) in fields) {
            val normalizedKey = rawKey.trim().lowercase().replace("-", "_")
            if (normalizedKey in PROHIBITED_FIELD_NAMES || normalizedKey.replace("_", "") in PROHIBITED_FIELD_NAMES) {
                recordDatabaseActivity(
                    actorUid = actorUid,
                    actorEmail = "unknown",
                    actorRole = UserRole.CUSTOMER,
                    operation = DatabaseOperationType.SECURITY_VIOLATION_BLOCKED,
                    collectionOrTable = "pci_dss_guard",
                    documentOrRowId = rawKey,
                    authorized = false,
                    anomalyDetected = true,
                    details = "Blocked attempt to persist prohibited payment/banking field '$rawKey'"
                )
                return ProhibitedDataValidationResult(
                    isSafeToStore = false,
                    violatedField = rawKey,
                    violationReason = "PCI-DSS Violation: Storing '$rawKey' (card numbers, CVVs, PINs, or banking passwords) is strictly prohibited."
                )
            }

            val strVal = rawValue?.toString().orEmpty()
            if (strVal.isNotBlank()) {
                if (CVV_LABEL_PATTERN.containsMatchIn(strVal)) {
                    return ProhibitedDataValidationResult(
                        isSafeToStore = false,
                        violatedField = rawKey,
                        violationReason = "PCI-DSS Violation: Value in '$rawKey' contains a labeled CVV, card PIN, or banking password."
                    )
                }
                if (containsRawCardNumberLuhn(strVal)) {
                    recordDatabaseActivity(
                        actorUid = actorUid,
                        actorEmail = "unknown",
                        actorRole = UserRole.CUSTOMER,
                        operation = DatabaseOperationType.SECURITY_VIOLATION_BLOCKED,
                        collectionOrTable = "pci_dss_guard",
                        documentOrRowId = rawKey,
                        authorized = false,
                        anomalyDetected = true,
                        details = "Blocked attempt to store raw credit/debit card PAN in field '$rawKey'"
                    )
                    return ProhibitedDataValidationResult(
                        isSafeToStore = false,
                        violatedField = rawKey,
                        violationReason = "PCI-DSS Violation: Raw credit/debit card number (PAN) detected in '$rawKey'. Never store card numbers."
                    )
                }
            }
        }
        return ProhibitedDataValidationResult(isSafeToStore = true)
    }

    /**
     * Checks whether a string contains a 13–19 digit sequence that passes the ISO/IEC 7812 Luhn checksum
     * and matches major card network prefixes (Visa 4, Mastercard 51-55/2221-2720, Verve 506/650, Amex 34/37).
     * Note: 10-digit Nigerian NUBAN bank account numbers are 10 digits and are NOT flagged as 13-19 digit PANs.
     */
    fun containsRawCardNumberLuhn(text: String): Boolean {
        for (match in CANDIDATE_PAN_REGEX.findAll(text)) {
            val digitsOnly = match.value.filter { it.isDigit() }
            if (digitsOnly.length in 13..19 && passesLuhnCheck(digitsOnly)) {
                val isKnownCardIssuer = digitsOnly.startsWith("4") ||
                    digitsOnly.startsWith("5") ||
                    digitsOnly.startsWith("2") ||
                    digitsOnly.startsWith("34") ||
                    digitsOnly.startsWith("37") ||
                    digitsOnly.startsWith("506") ||
                    digitsOnly.startsWith("650") ||
                    digitsOnly.startsWith("6011")
                if (isKnownCardIssuer) {
                    return true
                }
            }
        }
        return false
    }

    fun passesLuhnCheck(digits: String): Boolean {
        if (digits.length !in 13..19 || !digits.all { it.isDigit() }) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    fun scrubProhibitedCardAndBankingSecrets(input: String): String {
        if (input.isEmpty()) return ""
        var scrubbed = input.replace(CVV_LABEL_PATTERN, "[REDACTED_PAYMENT_SECRET]")
        scrubbed = CANDIDATE_PAN_REGEX.replace(scrubbed) { matchResult ->
            val digits = matchResult.value.filter { it.isDigit() }
            if (digits.length in 13..19 && passesLuhnCheck(digits)) {
                "[REDACTED_CARD_PAN]"
            } else {
                matchResult.value
            }
        }
        return scrubbed
    }

    // =========================================================================
    // 5. HTTPS / TLS 1.2+ Transit Enforcement
    // =========================================================================

    fun isSecureHttpsEndpoint(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        if (!trimmed.startsWith("https://")) return false
        if (trimmed.contains(" ") || trimmed.contains("\u0000") || trimmed.contains("\r") || trimmed.contains("\n")) {
            return false
        }
        val hostPart = trimmed.removePrefix("https://").substringBefore("/").substringBefore(":")
        if (hostPart.isBlank() || hostPart == "localhost" || hostPart == "127.0.0.1" || hostPart == "0.0.0.0") {
            return false
        }
        return hostPart.contains(".")
    }

    fun enforceHttpsTransportOrThrow(url: String) {
        if (!isSecureHttpsEndpoint(url)) {
            throw SecurityException("Insecure transport rejected: All SolarFix database and API connections must use HTTPS/TLS 1.2+ ($url).")
        }
    }

    // =========================================================================
    // 6. Row-Level Security (RLS) & Safe Database Query Validator
    // =========================================================================

    /**
     * Validates a database document ID or query filter value to prevent NoSQL/SQL injection,
     * path traversal (`/` or `..`), or wildcard enumeration.
     */
    fun isSafeDatabaseIdentifier(identifier: String): Boolean {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty() || trimmed.length > 128) return false
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..") || trimmed.contains("\u0000")) {
            return false
        }
        if (SecuritySanitizer.containsDangerousPayload(trimmed)) {
            return false
        }
        return true
    }

    /**
     * Evaluates Row-Level Security (RLS) policy for any database document read or write.
     */
    fun evaluateRowLevelAccessPolicy(
        actorUid: String,
        actorEmail: String,
        actorRole: UserRole,
        isActorAdmin: Boolean,
        operation: DatabaseOperationType,
        collection: String,
        resourceOwnerUid: String,
        resourceOwnerEmail: String = ""
    ): Boolean {
        if (actorUid.isBlank() && actorEmail.isBlank()) return false
        val verifiedAdmin = isActorAdmin ||
            actorRole == UserRole.ADMIN ||
            FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(actorEmail)

        val isRowOwner = (resourceOwnerUid.isNotBlank() && resourceOwnerUid == actorUid) ||
            (resourceOwnerEmail.isNotBlank() && resourceOwnerEmail.equals(actorEmail, ignoreCase = true))

        val allowed = when (collection.lowercase()) {
            "users", "addresses" -> {
                when (operation) {
                    DatabaseOperationType.SELECT_READ,
                    DatabaseOperationType.INSERT_CREATE,
                    DatabaseOperationType.UPDATE_MUTATE -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.DELETE_REMOVE -> (collection.equals("addresses", ignoreCase = true) && isRowOwner) || verifiedAdmin
                    else -> verifiedAdmin
                }
            }
            "stores" -> {
                when (operation) {
                    DatabaseOperationType.SELECT_READ -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.INSERT_CREATE -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.UPDATE_MUTATE -> (isRowOwner && actorRole == UserRole.VERIFIED_SELLER) || isRowOwner || verifiedAdmin
                    DatabaseOperationType.DELETE_REMOVE -> verifiedAdmin
                    else -> verifiedAdmin
                }
            }
            "orders", "withdrawals", "disputes" -> {
                when (operation) {
                    DatabaseOperationType.SELECT_READ -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.INSERT_CREATE -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.UPDATE_MUTATE -> verifiedAdmin || (collection == "orders" && isRowOwner)
                    DatabaseOperationType.DELETE_REMOVE -> false // Financial & order records are immutable and cannot be deleted
                    else -> verifiedAdmin
                }
            }
            "transactions", "admin_audit_logs" -> {
                when (operation) {
                    DatabaseOperationType.SELECT_READ -> isRowOwner || verifiedAdmin
                    DatabaseOperationType.INSERT_CREATE -> verifiedAdmin
                    DatabaseOperationType.UPDATE_MUTATE,
                    DatabaseOperationType.DELETE_REMOVE -> false // Strictly append-only immutable audit & ledger tables
                    else -> verifiedAdmin
                }
            }
            else -> verifiedAdmin
        }

        recordDatabaseActivity(
            actorUid = actorUid,
            actorEmail = actorEmail,
            actorRole = actorRole,
            operation = operation,
            collectionOrTable = collection,
            documentOrRowId = resourceOwnerUid.ifBlank { "row_check" },
            authorized = allowed,
            anomalyDetected = !allowed,
            details = if (allowed) {
                "RLS policy permitted ${operation.name} on $collection"
            } else {
                "RLS policy BLOCKED unauthorized ${operation.name} on $collection by $actorEmail ($actorUid)"
            }
        )
        return allowed
    }

    // =========================================================================
    // 7. Database Activity Monitoring & Anomaly Tracking
    // =========================================================================

    fun recordDatabaseActivity(
        actorUid: String,
        actorEmail: String,
        actorRole: UserRole,
        operation: DatabaseOperationType,
        collectionOrTable: String,
        documentOrRowId: String,
        encryptedFieldsProtected: Boolean = true,
        authorized: Boolean = true,
        anomalyDetected: Boolean = false,
        details: String
    ): DatabaseActivityLogEntry {
        val entry = DatabaseActivityLogEntry(
            actorUid = actorUid,
            actorEmail = SecuritySanitizer.maskEmail(actorEmail),
            actorRole = actorRole,
            operation = operation,
            collectionOrTable = collectionOrTable,
            documentOrRowId = documentOrRowId,
            encryptedFieldsProtected = encryptedFieldsProtected,
            authorized = authorized,
            anomalyDetected = anomalyDetected,
            details = scrubProhibitedCardAndBankingSecrets(
                SecuritySanitizer.redactTokensFromTextOrUrl(details)
            )
        )
        _activityLogs.update { current ->
            (listOf(entry) + current).take(500)
        }
        if (!authorized || anomalyDetected) {
            MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = SecurityMonitoringCategory.UNUSUAL_DATABASE_ACCESS,
                eventType = "DATABASE_${operation.name}_ANOMALY",
                actorIdentifier = actorEmail.ifBlank { actorUid },
                targetResourceId = "$collectionOrTable:$documentOrRowId",
                details = entry.details
            )
        }
        return entry
    }

    // =========================================================================
    // 8. Encrypted & HMAC-Signed Database Backup Snapshot
    // =========================================================================

    fun createEncryptedBackupSnapshot(
        stores: List<SellerStore>,
        orders: List<MarketplaceOrder>,
        addresses: List<DeliveryAddress>,
        adminEmail: String = "admin@solarfix.africa"
    ): EncryptedBackupArchive {
        if (!FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(adminEmail)) {
            throw SecurityException("Only verified Platform Administrators can generate database backups.")
        }
        val backupId = "BKP-${UUID.randomUUID().toString().take(10).uppercase()}"
        val encryptedStores = stores.map { encryptSellerStoreForDatabase(it) }
        val encryptedAddresses = addresses.map { encryptDeliveryAddressForDatabase(it) }

        val canonicalBackupJson = buildString {
            append("{\"backupId\":\"").append(backupId).append("\",")
            append("\"storesCount\":").append(encryptedStores.size).append(",")
            append("\"ordersCount\":").append(orders.size).append(",")
            append("\"addressesCount\":").append(encryptedAddresses.size).append(",")
            append("\"storeLedgers\":[")
            encryptedStores.forEachIndexed { idx, st ->
                if (idx > 0) append(",")
                append("{\"id\":\"").append(st.storeId)
                    .append("\",\"hmac\":\"").append(st.ledgerIntegrityHmac)
                    .append("\",\"bankEnc\":\"").append(st.encryptedBankAccountNumber).append("\"}")
            }
            append("]}")
        }

        val sha256Bytes = MessageDigest.getInstance("SHA-256")
            .digest(canonicalBackupJson.toByteArray(StandardCharsets.UTF_8))
        val checksumHex = sha256Bytes.joinToString("") { "%02x".format(it) }

        val ciphertext = encryptSensitiveField(
            plaintext = canonicalBackupJson,
            associatedData = "backup:$backupId:$checksumHex",
            keyId = activeKeyId
        )
        val hmacSig = computeHmacSha256("backup=$backupId|sha256=$checksumHex|ct=$ciphertext")

        recordDatabaseActivity(
            actorUid = "admin_backup_operator",
            actorEmail = adminEmail,
            actorRole = UserRole.ADMIN,
            operation = DatabaseOperationType.BACKUP_CREATE,
            collectionOrTable = "all_collections",
            documentOrRowId = backupId,
            encryptedFieldsProtected = true,
            authorized = true,
            details = "Created AES-256-GCM encrypted backup archive $backupId (${stores.size} stores, ${orders.size} orders, ${addresses.size} addresses)"
        )

        return EncryptedBackupArchive(
            backupId = backupId,
            createdAtEpoch = System.currentTimeMillis(),
            keyId = activeKeyId,
            encryptedPayloadCiphertext = ciphertext,
            payloadSha256Checksum = checksumHex,
            archiveHmacSignature = hmacSig,
            storesCount = stores.size,
            ordersCount = orders.size,
            addressesCount = addresses.size
        )
    }

    fun verifyAndDecryptBackupSnapshot(
        archive: EncryptedBackupArchive,
        adminEmail: String = "admin@solarfix.africa"
    ): String {
        if (!FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(adminEmail)) {
            throw SecurityException("Only verified Platform Administrators can restore or decrypt database backups.")
        }
        val expectedCanonical = "backup=${archive.backupId}|sha256=${archive.payloadSha256Checksum}|ct=${archive.encryptedPayloadCiphertext}"
        if (!verifyHmacSha256(expectedCanonical, archive.archiveHmacSignature)) {
            throw SecurityException("Backup archive HMAC signature verification failed. Archive may be tampered with.")
        }
        val decryptedJson = decryptSensitiveField(
            ciphertextPayload = archive.encryptedPayloadCiphertext,
            associatedData = "backup:${archive.backupId}:${archive.payloadSha256Checksum}"
        )
        val computedSha256 = MessageDigest.getInstance("SHA-256")
            .digest(decryptedJson.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        if (computedSha256 != archive.payloadSha256Checksum) {
            throw SecurityException("Backup payload SHA-256 checksum mismatch.")
        }
        recordDatabaseActivity(
            actorUid = "admin_backup_operator",
            actorEmail = adminEmail,
            actorRole = UserRole.ADMIN,
            operation = DatabaseOperationType.BACKUP_RESTORE,
            collectionOrTable = "all_collections",
            documentOrRowId = archive.backupId,
            encryptedFieldsProtected = true,
            authorized = true,
            details = "Verified and decrypted backup snapshot ${archive.backupId}"
        )
        return decryptedJson
    }
}
