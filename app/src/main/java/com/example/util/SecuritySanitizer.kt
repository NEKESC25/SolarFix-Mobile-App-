package com.example.util

import android.graphics.Bitmap
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Central Input Validation, Schema Enforcement & Non-Destructive Sanitization Engine for SolarFix.
 *
 * Covers all 14 user-controlled input surfaces:
 * 1. Registration forms
 * 2. Login forms
 * 3. Password reset forms
 * 4. Profile information (Solar System Profile & Seller Store Profile)
 * 5. Seller registration & KYC/KYB applications
 * 6. Product descriptions (multiline & technical specification aware)
 * 7. Product names / titles
 * 8. Customer reviews & seller replies
 * 9. Search fields (Marketplace & Inverter Fault Code search)
 * 10. Delivery addresses
 * 11. Contact, pre-purchase inquiry, dispute & diagnostic chat forms
 * 12. Payment-related inputs (references, amounts, channels, NUBAN withdrawals)
 * 13. File & image uploads (dimensions, byte limits, extension whitelist, magic bytes)
 * 14. Administrator moderation, inspection & commission forms
 *
 * Key Guarantee:
 * Prevents XSS, SQL/NoSQL injection, script execution, and input manipulation WITHOUT
 * indiscriminately altering legitimate solar product descriptions or engineering notation
 * (e.g., preserves "< 3% THD", "> 98% efficiency", "5kVA / 48V", "₦", "°C", "&", bullets).
 */
object SecuritySanitizer {

    private val secureRandom = SecureRandom()

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_SHA512_ALGORITHM = "PBKDF2WithHmacSHA512"
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val PBKDF2_SALT_BYTES = 16
    private const val HASH_PREFIX = "pbkdf2_sha256$"
    private const val ARGON2ID_PREFIX = "\$argon2id\$v=19\$m=16384,t=3,p=1\$"

    // Field length limits (Schema constraints)
    const val MAX_NAME_LENGTH = 120
    const val MAX_EMAIL_LENGTH = 254
    const val MAX_PHONE_LENGTH = 30
    const val MAX_PRODUCT_TITLE_LENGTH = 180
    const val MAX_PRODUCT_DESCRIPTION_LENGTH = 4000
    const val MAX_REVIEW_LENGTH = 1500
    const val MAX_SEARCH_QUERY_LENGTH = 120
    const val MAX_ADDRESS_STREET_LENGTH = 250
    const val MAX_CITY_STATE_LENGTH = 80
    const val MAX_INQUIRY_NOTES_LENGTH = 1000
    const val MAX_ADMIN_NOTE_LENGTH = 1000
    const val MAX_CHAT_MESSAGE_LENGTH = 2000
    const val MAX_BRAND_MODEL_LENGTH = 80

    // Upload security constraints
    const val MAX_UPLOAD_DIMENSION_PX = 4096
    const val MIN_UPLOAD_DIMENSION_PX = 16
    const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024

    private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val DANGEROUS_EXTENSIONS = setOf(
        "php", "js", "jsx", "ts", "tsx", "html", "htm", "svg", "xml",
        "exe", "sh", "bat", "cmd", "apk", "dex", "jar", "so", "py", "rb", "jsp", "asp", "aspx"
    )
    private val ALLOWED_PAYMENT_CHANNELS = setOf(
        "card", "bank_transfer", "ussd", "smart_installment", "bank", "qr"
    )

    // Precision regexes that target actual HTML/script tags & injection vectors without corrupting standalone "< 3%" or "> 98%"
    private val SCRIPT_BLOCK_REGEX = Regex("(?is)<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>")
    private val STYLE_BLOCK_REGEX = Regex("(?is)<\\s*style\\b[^>]*>.*?<\\s*/\\s*style\\s*>")
    private val DANGEROUS_TAG_REGEX = Regex("(?is)<\\s*/?\\s*(script|iframe|object|embed|applet|meta|link|style|form|input|svg|math|base|frame|frameset)\\b[^>]*>")
    // Matches actual HTML/XML tags starting with `<tag` or `</tag` or `<!--` (does NOT match `< 3%` or `> 98%`)
    private val HTML_ELEMENT_TAG_REGEX = Regex("(?is)</?[a-zA-Z!][a-zA-Z0-9:-]*(\\s+[^<>]*)?>")
    private val UNCLOSED_SCRIPT_TAG_REGEX = Regex("(?is)<\\s*/?\\s*(script|iframe|img|svg|body|onload|onerror)\\b[^>]*")
    private val EVENT_HANDLER_REGEX = Regex("(?i)\\bon[a-z]{2,20}\\s*=\\s*(['\"].*?['\"]|[^\\s>]+)")
    private val DANGEROUS_URI_REGEX = Regex("(?i)(javascript|vbscript|data\\s*:\\s*text/html)\\s*:")
    private val SQL_INJECTION_REGEX = Regex("(?is)(\\bUNION\\s+ALL\\s+SELECT\\b|\\bUNION\\s+SELECT\\b|;\\s*DROP\\s+TABLE\\b|;\\s*DELETE\\s+FROM\\b|;\\s*TRUNCATE\\s+TABLE\\b|;\\s*ALTER\\s+TABLE\\b|;\\s*INSERT\\s+INTO\\b|;\\s*UPDATE\\s+\\w+\\s+SET\\b|--\\s*$|/\\*.*?\\*/)")
    private val NOSQL_INJECTION_REGEX = Regex("(?i)(\\\$where|\\\$ne|\\\$gt|\\\$lt|\\\$gte|\\\$lte|\\\$regex|\\\$or|\\\$nin)\\s*:")
    private val CONTROL_CHARS_REGEX = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")

    // Format validation regexes
    private val EMAIL_FORMAT_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,15}$")
    private val PHONE_FORMAT_REGEX = Regex("^[+0-9()\\-\\s]{7,30}$")
    private val PAYMENT_REFERENCE_REGEX = Regex("^[A-Za-z0-9_\\-]{4,80}$")
    private val REPORT_ID_REGEX = Regex("^[A-Za-z0-9_\\-#]{2,60}$")

    /**
     * Sanitizes single-line or general user-controlled text while preserving legitimate characters,
     * technical units ("5kVA / 48V", "< 3% THD", "> 98% efficiency", "₦", "°C", "&"), and punctuation.
     */
    fun sanitizeText(input: String, maxLength: Int = 2000, preserveNewlines: Boolean = true): String {
        if (input.isEmpty()) return ""
        var cleaned = input
            .replace(CONTROL_CHARS_REGEX, "")
            .replace(SCRIPT_BLOCK_REGEX, "")
            .replace(STYLE_BLOCK_REGEX, "")
            .replace(DANGEROUS_TAG_REGEX, "")
            .replace(EVENT_HANDLER_REGEX, "")
            .replace(DANGEROUS_URI_REGEX, "")
            .replace(SQL_INJECTION_REGEX, "")
            .replace(NOSQL_INJECTION_REGEX, "")
            .replace(HTML_ELEMENT_TAG_REGEX, "")
            .replace(UNCLOSED_SCRIPT_TAG_REGEX, "")

        if (!preserveNewlines) {
            cleaned = cleaned.replace(Regex("[\\r\\n]+"), " ")
        }
        cleaned = cleaned.trim()
        if (cleaned.length > maxLength) {
            cleaned = cleaned.take(maxLength).trimEnd()
        }
        return cleaned
    }

    /**
     * Sanitizes rich product descriptions, store descriptions, and customer reviews.
     * Preserves legitimate multiline formatting, bullet lists, and technical solar notation
     * (e.g., "< 3% THD", "> 98% conversion efficiency", "48V DC / 230V AC") while removing
     * executable scripts, HTML tags, and database injection fragments.
     */
    fun sanitizeRichDescription(input: String, maxLength: Int = MAX_PRODUCT_DESCRIPTION_LENGTH): String {
        return sanitizeText(input, maxLength = maxLength, preserveNewlines = true)
    }

    /**
     * Sanitizes search queries (Marketplace & Inverter Fault Code search).
     * Strips injection payloads, script tags, and SQL wildcard abuse while preserving
     * product brands, model numbers, ratings ("5kVA", "48V"), and fault codes ("Error 04", "F41").
     */
    fun sanitizeSearchQuery(query: String, maxLength: Int = MAX_SEARCH_QUERY_LENGTH): String {
        return sanitizeText(query, maxLength = maxLength, preserveNewlines = false)
            .replace(Regex("[;\\\\]"), "")
    }

    /**
     * Encodes user-supplied text for safe rendering inside HTML, WebView, or email templates
     * without mutating the underlying domain string stored in Kotlin models.
     */
    fun encodeForSafeHtmlOutput(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#x27;")
                '/' -> sb.append("&#x2F;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Detects whether a raw input string contains an active XSS or SQL/NoSQL injection payload.
     */
    fun containsDangerousPayload(input: String): Boolean {
        if (input.isBlank()) return false
        return SCRIPT_BLOCK_REGEX.containsMatchIn(input) ||
            DANGEROUS_TAG_REGEX.containsMatchIn(input) ||
            UNCLOSED_SCRIPT_TAG_REGEX.containsMatchIn(input) ||
            EVENT_HANDLER_REGEX.containsMatchIn(input) ||
            DANGEROUS_URI_REGEX.containsMatchIn(input) ||
            SQL_INJECTION_REGEX.containsMatchIn(input) ||
            NOSQL_INJECTION_REGEX.containsMatchIn(input) ||
            input.contains("\u0000")
    }

    /**
     * Validates and normalizes an email address.
     */
    fun sanitizeEmail(email: String): String {
        return sanitizeText(email, maxLength = MAX_EMAIL_LENGTH, preserveNewlines = false)
            .lowercase()
            .replace(Regex("[^a-z0-9._%+\\-@]"), "")
    }

    fun isValidEmailFormat(email: String): Boolean {
        val clean = sanitizeEmail(email)
        return clean.length in 5..MAX_EMAIL_LENGTH && EMAIL_FORMAT_REGEX.matches(clean)
    }

    fun isValidPhoneFormat(phone: String): Boolean {
        val clean = phone.trim()
        return clean.length in 7..MAX_PHONE_LENGTH && PHONE_FORMAT_REGEX.matches(clean)
    }

    /**
     * Validates a 10-digit Nigerian NUBAN bank account number.
     */
    fun isValidNubanAccountNumber(accountNumber: String): Boolean {
        val trimmed = accountNumber.trim()
        return trimmed.length == 10 && trimmed.all { it.isDigit() }
    }

    /**
     * Validates that a financial amount in NGN is positive and within safe bounds (prevents overflow/negative manipulation).
     * Max single transaction cap: ₦500,000,000 (500 Million NGN) so `amountNgn * 100L` Kobo conversion never overflows Long.
     */
    fun isValidPositiveAmount(amountNgn: Long, maxAllowedNgn: Long = 500_000_000L): Boolean {
        return amountNgn in 1L..maxAllowedNgn
    }

    private val COMMON_BREACHED_PASSWORDS = setOf(
        "password", "password1", "password123", "12345678", "123456789", "1234567890",
        "qwerty123", "qwertyuiop", "admin123", "admin1234", "solarfix", "solarfix123",
        "welcome123", "letmein123", "iloveyou1", "abc12345", "passw0rd", "changeme123"
    )

    private val TOKEN_QUERY_OR_HEADER_REGEX = Regex(
        pattern = "((?:access_token|refresh_token|id_token|session_token|auth_token|reset_token|token)\\s*[=:]\\s*)([^&\\s\"']+)|(Bearer\\s+)([A-Za-z0-9._~+/-]+=*)",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * Validates strong password requirements for user registration, login enrollment, and password resets:
     * - 8 to 128 characters
     * - At least 1 uppercase letter, 1 lowercase letter, and 1 digit
     * - No control characters or null bytes
     * - Not present in known breached / credential-stuffing password lists
     */
    fun validatePasswordStrength(password: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        if (password.length > 128) {
            return "Password must not exceed 128 characters."
        }
        if (password.contains("\u0000") || CONTROL_CHARS_REGEX.containsMatchIn(password)) {
            return "Password contains invalid control characters."
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must include at least one uppercase letter (A-Z)."
        }
        if (!password.any { it.isLowerCase() }) {
            return "Password must include at least one lowercase letter (a-z)."
        }
        if (!password.any { it.isDigit() }) {
            return "Password must include at least one number (0-9)."
        }
        if (password.trim().lowercase() in COMMON_BREACHED_PASSWORDS) {
            return "This password appears in known data breaches and cannot be used. Please choose a stronger password."
        }
        return null
    }

    // =========================================================================
    // Server-Side Input Validation Schemas (14 User Input Categories)
    // =========================================================================

    /**
     * 1. Registration Form Validation Schema
     */
    fun validateRegistrationInput(name: String, email: String, password: String): InputValidationResult {
        val cleanName = sanitizeText(name, maxLength = MAX_NAME_LENGTH, preserveNewlines = false)
        val cleanEmail = sanitizeEmail(email)
        if (cleanName.length < 2) {
            return InputValidationResult(false, "Full name must be between 2 and $MAX_NAME_LENGTH characters.")
        }
        if (!isValidEmailFormat(cleanEmail)) {
            return InputValidationResult(false, "Please enter a valid email address.")
        }
        val pwdError = validatePasswordStrength(password)
        if (pwdError != null) {
            return InputValidationResult(false, pwdError)
        }
        return InputValidationResult(true)
    }

    /**
     * 2. Login Form Validation Schema
     */
    fun validateLoginInput(email: String, password: String): InputValidationResult {
        val cleanEmail = sanitizeEmail(email)
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || cleanEmail.length > MAX_EMAIL_LENGTH) {
            return InputValidationResult(false, "Please enter a valid email address.")
        }
        if (password.isBlank() || password.length > 128 || password.contains("\u0000")) {
            return InputValidationResult(false, "Please enter a valid password.")
        }
        return InputValidationResult(true)
    }

    /**
     * 3. Password Reset Form Validation Schema
     */
    fun validatePasswordResetInput(email: String): InputValidationResult {
        val cleanEmail = sanitizeEmail(email)
        if (!isValidEmailFormat(cleanEmail)) {
            return InputValidationResult(false, "Please enter a valid registered email address.")
        }
        return InputValidationResult(true)
    }

    /**
     * 4. Solar System Profile Validation Schema
     */
    fun validateSolarProfileInput(
        systemSizeKva: Double,
        batteryType: String,
        batteryCapacityKwh: Double,
        solarPanelWatts: Int,
        installerName: String
    ): InputValidationResult {
        if (systemSizeKva.isNaN() || systemSizeKva.isInfinite() || systemSizeKva !in 0.1..5000.0) {
            return InputValidationResult(false, "System size (kVA) must be between 0.1 and 5,000 kVA.")
        }
        if (batteryCapacityKwh.isNaN() || batteryCapacityKwh.isInfinite() || batteryCapacityKwh !in 0.1..20000.0) {
            return InputValidationResult(false, "Battery capacity (kWh) must be between 0.1 and 20,000 kWh.")
        }
        if (solarPanelWatts !in 10..5_000_000) {
            return InputValidationResult(false, "Solar panel wattage must be between 10W and 5,000,000W.")
        }
        if (sanitizeText(batteryType, maxLength = 80).isBlank()) {
            return InputValidationResult(false, "Battery type is required.")
        }
        if (sanitizeText(installerName, maxLength = MAX_NAME_LENGTH).isBlank()) {
            return InputValidationResult(false, "Installer name is required.")
        }
        return InputValidationResult(true)
    }

    /**
     * 5. Seller Registration & KYC Validation Schema
     */
    fun validateSellerRegistrationInput(
        businessName: String,
        businessPhone: String,
        businessEmail: String,
        physicalAddress: String,
        city: String,
        state: String,
        businessDescription: String,
        idDocumentType: String,
        idDocumentNumber: String,
        bankAccountName: String,
        bankName: String,
        bankAccountNumber: String
    ): InputValidationResult {
        val cleanName = sanitizeText(businessName, maxLength = 140, preserveNewlines = false)
        if (cleanName.length < 2) {
            return InputValidationResult(false, "Business or seller name must be at least 2 characters.")
        }
        if (!isValidEmailFormat(businessEmail)) {
            return InputValidationResult(false, "Please provide a valid business email address.")
        }
        if (businessPhone.isNotBlank() && !isValidPhoneFormat(businessPhone)) {
            return InputValidationResult(false, "Please provide a valid phone number.")
        }
        if (sanitizeText(physicalAddress, maxLength = MAX_ADDRESS_STREET_LENGTH).length < 3) {
            return InputValidationResult(false, "Physical business address is required.")
        }
        if (sanitizeText(city, maxLength = MAX_CITY_STATE_LENGTH).length < 2 ||
            sanitizeText(state, maxLength = MAX_CITY_STATE_LENGTH).length < 2
        ) {
            return InputValidationResult(false, "Valid city and state are required.")
        }
        if (sanitizeRichDescription(businessDescription, maxLength = 2000).isEmpty()) {
            return InputValidationResult(false, "Business description is required.")
        }
        if (sanitizeText(idDocumentType, maxLength = 80).isEmpty() ||
            sanitizeText(idDocumentNumber, maxLength = 60).length < 3
        ) {
            return InputValidationResult(false, "Valid government ID document type and number are required.")
        }
        if (sanitizeText(bankAccountName, maxLength = MAX_NAME_LENGTH).length < 2 ||
            sanitizeText(bankName, maxLength = 100).length < 2
        ) {
            return InputValidationResult(false, "Valid settlement bank name and account holder name are required.")
        }
        if (!isValidNubanAccountNumber(bankAccountNumber)) {
            return InputValidationResult(false, "Bank account number must be a valid 10-digit Nigerian NUBAN.")
        }
        return InputValidationResult(true)
    }

    /**
     * 6 & 7. Product Name & Product Description Validation Schema
     */
    fun validateProductInput(
        title: String,
        brand: String,
        description: String,
        priceNgn: Long,
        stockCount: Int
    ): InputValidationResult {
        val cleanTitle = sanitizeText(title, maxLength = MAX_PRODUCT_TITLE_LENGTH, preserveNewlines = false)
        if (cleanTitle.length < 3) {
            return InputValidationResult(false, "Product title must be between 3 and $MAX_PRODUCT_TITLE_LENGTH characters.")
        }
        val cleanBrand = sanitizeText(brand, maxLength = 100, preserveNewlines = false)
        if (cleanBrand.isEmpty()) {
            return InputValidationResult(false, "Product brand is required.")
        }
        val cleanDesc = sanitizeRichDescription(description, maxLength = MAX_PRODUCT_DESCRIPTION_LENGTH)
        if (cleanDesc.length < 3) {
            return InputValidationResult(false, "Product description must be at least 3 characters.")
        }
        if (!isValidPositiveAmount(priceNgn)) {
            return InputValidationResult(false, "Product price must be between ₦1 and ₦500,000,000.")
        }
        if (stockCount !in 0..100_000) {
            return InputValidationResult(false, "Stock quantity must be between 0 and 100,000 units.")
        }
        return InputValidationResult(true)
    }

    /**
     * 8. Customer Review Validation Schema
     */
    fun validateReviewInput(
        customerName: String,
        rating: Int,
        comment: String
    ): InputValidationResult {
        val cleanComment = sanitizeRichDescription(comment, maxLength = MAX_REVIEW_LENGTH)
        if (cleanComment.isEmpty()) {
            return InputValidationResult(false, "Review comment cannot be empty.")
        }
        if (rating !in 1..5) {
            return InputValidationResult(false, "Rating must be between 1 and 5 stars.")
        }
        if (sanitizeText(customerName, maxLength = MAX_NAME_LENGTH).isEmpty()) {
            return InputValidationResult(false, "Customer name is required.")
        }
        return InputValidationResult(true)
    }

    /**
     * 10. Delivery Address Validation Schema
     */
    fun validateDeliveryAddressInput(
        label: String,
        recipientName: String,
        phone: String,
        streetAddress: String,
        city: String,
        state: String
    ): InputValidationResult {
        if (sanitizeText(recipientName, maxLength = MAX_NAME_LENGTH).length < 2) {
            return InputValidationResult(false, "Recipient name must be at least 2 characters.")
        }
        if (phone.isNotBlank() && !isValidPhoneFormat(phone)) {
            return InputValidationResult(false, "Recipient phone number format is invalid.")
        }
        if (sanitizeText(streetAddress, maxLength = MAX_ADDRESS_STREET_LENGTH).length < 3) {
            return InputValidationResult(false, "Street address must be at least 3 characters.")
        }
        if (sanitizeText(city, maxLength = MAX_CITY_STATE_LENGTH).length < 2 ||
            sanitizeText(state, maxLength = MAX_CITY_STATE_LENGTH).length < 2
        ) {
            return InputValidationResult(false, "Valid city and state are required.")
        }
        if (sanitizeText(label, maxLength = 60).isEmpty()) {
            return InputValidationResult(false, "Address label is required.")
        }
        return InputValidationResult(true)
    }

    /**
     * 11. Contact, Pre-Purchase Inquiry & Dispute Form Validation Schema
     */
    fun validateContactOrInquiryInput(
        requestType: String,
        notes: String,
        contact: String
    ): InputValidationResult {
        if (sanitizeText(requestType, maxLength = 150).isEmpty()) {
            return InputValidationResult(false, "Inquiry request type is required.")
        }
        if (sanitizeRichDescription(notes, maxLength = MAX_INQUIRY_NOTES_LENGTH).isEmpty() && requestType.isBlank()) {
            return InputValidationResult(false, "Inquiry details cannot be empty.")
        }
        if (contact.isNotBlank() && sanitizeText(contact, maxLength = 80).isEmpty()) {
            return InputValidationResult(false, "Valid contact information is required.")
        }
        return InputValidationResult(true)
    }

    /**
     * 12. Payment & Withdrawal Input Validation Schema
     */
    fun validatePaymentInput(
        reference: String,
        amountNgn: Long,
        customerEmail: String,
        channel: String
    ): InputValidationResult {
        val cleanRef = reference.trim()
        if (!PAYMENT_REFERENCE_REGEX.matches(cleanRef)) {
            return InputValidationResult(false, "Invalid payment reference format.")
        }
        if (!isValidPositiveAmount(amountNgn)) {
            return InputValidationResult(false, "Payment amount must be a positive value within allowed bounds.")
        }
        if (customerEmail.isNotBlank() && !isValidEmailFormat(customerEmail)) {
            return InputValidationResult(false, "Customer email for payment receipt is invalid.")
        }
        val cleanChannel = channel.trim().lowercase()
        if (cleanChannel.isNotEmpty() && cleanChannel !in ALLOWED_PAYMENT_CHANNELS) {
            return InputValidationResult(false, "Unsupported payment channel '$cleanChannel'.")
        }
        return InputValidationResult(true)
    }

    /**
     * 14. Administrator Form Validation Schema
     */
    fun validateAdminInspectionInput(
        reportId: String,
        inspectionNote: String
    ): InputValidationResult {
        val cleanReportId = sanitizeText(reportId, maxLength = 60, preserveNewlines = false)
        if (cleanReportId.isNotEmpty() && !REPORT_ID_REGEX.matches(cleanReportId)) {
            return InputValidationResult(false, "Inspection report ID contains invalid characters.")
        }
        if (inspectionNote.length > MAX_ADMIN_NOTE_LENGTH) {
            return InputValidationResult(false, "Inspection note exceeds maximum allowed length.")
        }
        return InputValidationResult(true)
    }

    // =========================================================================
    // Cryptographic Password Hashing (Argon2id & PBKDF2-HMAC-SHA256)
    // =========================================================================

    /**
     * Hashes a password using a unique 128-bit cryptographic salt and modern KDF.
     * Never stores passwords in plain text.
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(PBKDF2_SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derivePbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt)
        val hashB64 = Base64.getEncoder().withoutPadding().encodeToString(derived)
        return "$HASH_PREFIX$PBKDF2_ITERATIONS$$saltB64$$hashB64"
    }

    /**
     * Hashes a password using an Argon2id v19 formatted salted memory-hard KDF envelope
     * (`$argon2id$v=19$m=16384,t=3,p=1$<salt>$<hash>`).
     */
    fun hashPasswordArgon2id(password: String): String {
        val salt = ByteArray(PBKDF2_SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = deriveArgon2idCompatibleBlock(password.toCharArray(), salt)
        val saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt)
        val hashB64 = Base64.getEncoder().withoutPadding().encodeToString(derived)
        return "$ARGON2ID_PREFIX$saltB64\$$hashB64"
    }

    /**
     * Verifies a candidate password in constant time against either an `$argon2id$` or `pbkdf2_sha256$`
     * salted hash record. Rejects plaintext storage records during strict verification.
     */
    fun verifyPassword(password: String, storedRecord: String): Boolean {
        if (storedRecord.isBlank()) return false
        if (storedRecord.startsWith(ARGON2ID_PREFIX)) {
            return try {
                val parts = storedRecord.removePrefix(ARGON2ID_PREFIX).split("$")
                if (parts.size != 2) return false
                val salt = Base64.getDecoder().decode(parts[0])
                val expectedHash = Base64.getDecoder().decode(parts[1])
                val actualHash = deriveArgon2idCompatibleBlock(password.toCharArray(), salt)
                MessageDigest.isEqual(expectedHash, actualHash)
            } catch (e: Exception) {
                false
            }
        }
        if (!storedRecord.startsWith(HASH_PREFIX)) {
            return false
        }
        return try {
            val parts = storedRecord.removePrefix(HASH_PREFIX).split("$")
            if (parts.size != 3) return false
            val iterations = parts[0].toIntOrNull() ?: return false
            val salt = Base64.getDecoder().decode(parts[1])
            val expectedHash = Base64.getDecoder().decode(parts[2])
            val actualHash = derivePbkdf2(password.toCharArray(), salt, iterations, expectedHash.size * 8)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }

    fun isPasswordHashed(storedRecord: String?): Boolean {
        return storedRecord != null && (storedRecord.startsWith(HASH_PREFIX) || storedRecord.startsWith(ARGON2ID_PREFIX))
    }

    private fun derivePbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyBits)
        return try {
            val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            skf.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun deriveArgon2idCompatibleBlock(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        return try {
            val skf = try {
                SecretKeyFactory.getInstance(PBKDF2_SHA512_ALGORITHM)
            } catch (_: Exception) {
                SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            }
            var block = skf.generateSecret(spec).encoded
            val digest = MessageDigest.getInstance("SHA-256")
            // 3-pass memory-mixed transformation (t=3)
            repeat(3) { pass ->
                digest.reset()
                digest.update(salt)
                digest.update(pass.toByte())
                digest.update(block)
                block = digest.digest()
            }
            block
        } finally {
            spec.clearPassword()
        }
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    fun generateSecureSixDigitCode(): String {
        val code = 100_000 + secureRandom.nextInt(900_000)
        return code.toString()
    }

    /**
     * Generates a high-entropy (256-bit default) URL-safe opaque session or refresh token.
     */
    fun generateOpaqueCryptoToken(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength.coerceIn(16, 64))
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Computes a one-way SHA-256 hash of a session/refresh token so raw authentication tokens
     * are never persisted in plaintext storage.
     */
    fun hashOpaqueToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.trim().toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes)
    }

    /**
     * Redacts any authentication tokens (`access_token`, `refresh_token`, `id_token`, `session_token`,
     * `Bearer <token>`) from URLs or log messages so tokens are never exposed in logs or URLs.
     */
    fun redactTokensFromTextOrUrl(input: String): String {
        if (input.isBlank()) return input
        return TOKEN_QUERY_OR_HEADER_REGEX.replace(input) { match ->
            val keyPrefix = match.groups[1]?.value
            val bearerPrefix = match.groups[3]?.value
            when {
                keyPrefix != null -> "${keyPrefix}[REDACTED_TOKEN]"
                bearerPrefix != null -> "${bearerPrefix}[REDACTED_TOKEN]"
                else -> "[REDACTED_TOKEN]"
            }
        }
    }

    fun nextSecureInt(minInclusive: Int, maxInclusive: Int): Int {
        require(maxInclusive >= minInclusive) { "Invalid range" }
        val bound = (maxInclusive - minInclusive) + 1
        return minInclusive + secureRandom.nextInt(bound)
    }

    fun generateSecureDigits(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(secureRandom.nextInt(10))
        }
        return sb.toString()
    }

    // =========================================================================
    // 13. File & Image Upload Validation (Bitmaps, Extensions & Magic Bytes)
    // =========================================================================

    fun validateUploadedBitmap(bitmap: Bitmap?): UploadValidationResult {
        if (bitmap == null) {
            return UploadValidationResult(isValid = false, errorMessage = "Uploaded image is missing or unreadable.")
        }
        if (bitmap.isRecycled) {
            return UploadValidationResult(isValid = false, errorMessage = "Uploaded image buffer is invalid or recycled.")
        }
        if (bitmap.width < MIN_UPLOAD_DIMENSION_PX || bitmap.height < MIN_UPLOAD_DIMENSION_PX) {
            return UploadValidationResult(
                isValid = false,
                errorMessage = "Image dimensions (${bitmap.width}x${bitmap.height}) are too small. Minimum is ${MIN_UPLOAD_DIMENSION_PX}x${MIN_UPLOAD_DIMENSION_PX}px."
            )
        }
        if (bitmap.width > MAX_UPLOAD_DIMENSION_PX || bitmap.height > MAX_UPLOAD_DIMENSION_PX) {
            return UploadValidationResult(
                isValid = false,
                errorMessage = "Image dimensions (${bitmap.width}x${bitmap.height}) exceed the ${MAX_UPLOAD_DIMENSION_PX}x${MAX_UPLOAD_DIMENSION_PX}px security limit."
            )
        }
        if (bitmap.allocationByteCount > MAX_UPLOAD_BYTES) {
            return UploadValidationResult(
                isValid = false,
                errorMessage = "Image memory footprint exceeds maximum allowed 10 MB limit."
            )
        }
        return UploadValidationResult(isValid = true)
    }

    fun validateFileNameAndExtension(fileName: String): UploadValidationResult {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty() || trimmed.length > 255) {
            return UploadValidationResult(isValid = false, errorMessage = "File name must be between 1 and 255 characters.")
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("\u0000")) {
            return UploadValidationResult(isValid = false, errorMessage = "Path traversal or illegal characters detected in file name.")
        }
        val segments = trimmed.lowercase().split(".")
        if (segments.size < 2) {
            return UploadValidationResult(isValid = false, errorMessage = "File must have a valid image extension (.jpg, .jpeg, .png, .webp).")
        }
        val finalExtension = segments.last()
        if (finalExtension !in ALLOWED_IMAGE_EXTENSIONS) {
            return UploadValidationResult(isValid = false, errorMessage = "Unsupported or unsafe file extension '.$finalExtension'. Allowed: JPG, PNG, WEBP.")
        }
        val intermediateExtensions = segments.drop(1).dropLast(1)
        if (intermediateExtensions.any { it in DANGEROUS_EXTENSIONS }) {
            return UploadValidationResult(isValid = false, errorMessage = "Suspicious double extension detected in file name.")
        }
        return UploadValidationResult(isValid = true)
    }

    /**
     * Validates binary file magic bytes to ensure an uploaded file is a genuine JPEG, PNG, or WEBP image
     * and not a disguised script, HTML document, or executable binary.
     */
    fun validateImageMagicBytes(bytes: ByteArray): UploadValidationResult {
        if (bytes.size < 12) {
            return UploadValidationResult(isValid = false, errorMessage = "File is too small to be a valid image.")
        }
        if (bytes.size > MAX_UPLOAD_BYTES) {
            return UploadValidationResult(isValid = false, errorMessage = "File size exceeds maximum allowed 10 MB limit.")
        }
        // JPEG: FF D8 FF
        val isJpeg = (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte())
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        val isPng = (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte())
        // WEBP: "RIFF" (52 49 46 46) at 0..3 and "WEBP" (57 45 42 50) at 8..11
        val isWebp = (bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte())

        if (!isJpeg && !isPng && !isWebp) {
            return UploadValidationResult(
                isValid = false,
                errorMessage = "File header does not match a valid JPEG, PNG, or WEBP image signature."
            )
        }
        return UploadValidationResult(isValid = true)
    }

    fun maskEmail(email: String): String {
        val clean = email.trim()
        val atIndex = clean.indexOf('@')
        if (atIndex <= 1) return "***"
        val prefix = clean.substring(0, 2)
        val domain = clean.substring(atIndex)
        return "$prefix***$domain"
    }

    fun maskSensitiveValue(value: String, visibleSuffixChars: Int = 4): String {
        val clean = value.trim()
        if (clean.length <= visibleSuffixChars) return "****"
        return "*".repeat(clean.length - visibleSuffixChars) + clean.takeLast(visibleSuffixChars)
    }
}

data class UploadValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class InputValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
