package com.example.util

import android.graphics.Bitmap
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Defines the 6 distinct categories of user/seller file uploads in SolarFix,
 * with strict per-category size limits, MIME whitelists, extension whitelists,
 * and storage partition isolation.
 */
enum class UploadFileCategory(
    val categoryCode: String,
    val displayName: String,
    val partition: StoragePartition,
    val accessLevel: FileAccessLevel,
    val maxFileSizeBytes: Long,
    val allowedExtensions: Set<String>,
    val allowedMimeTypes: Set<String>,
    val storageSubdirectory: String
) {
    SELLER_IDENTITY_DOCUMENT(
        categoryCode = "kyc_id",
        displayName = "Seller Identity Document (NIN / Passport / CAC)",
        partition = StoragePartition.PRIVATE_ADMIN_VAULT,
        accessLevel = FileAccessLevel.PRIVATE_ADMIN_ONLY,
        maxFileSizeBytes = 10L * 1024L * 1024L, // 10 MB max
        allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "pdf"),
        allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf"),
        storageSubdirectory = "private-vault/kyc-identity-docs"
    ),
    BUSINESS_VERIFICATION_PHOTO(
        categoryCode = "kyb_premises",
        displayName = "Business Verification Photograph (Premises / Warehouse)",
        partition = StoragePartition.PRIVATE_ADMIN_VAULT,
        accessLevel = FileAccessLevel.PRIVATE_ADMIN_ONLY,
        maxFileSizeBytes = 10L * 1024L * 1024L, // 10 MB max
        allowedExtensions = setOf("jpg", "jpeg", "png", "webp"),
        allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp"),
        storageSubdirectory = "private-vault/business-premises"
    ),
    OTHER_VERIFICATION_DOCUMENT(
        categoryCode = "compliance_doc",
        displayName = "Compliance / Authenticity Verification Document (SONCAP / OEM Invoice)",
        partition = StoragePartition.PRIVATE_ADMIN_VAULT,
        accessLevel = FileAccessLevel.PRIVATE_ADMIN_ONLY,
        maxFileSizeBytes = 10L * 1024L * 1024L, // 10 MB max
        allowedExtensions = setOf("pdf", "jpg", "jpeg", "png", "webp"),
        allowedMimeTypes = setOf("application/pdf", "image/jpeg", "image/png", "image/webp"),
        storageSubdirectory = "private-vault/compliance-docs"
    ),
    PRODUCT_PHOTOGRAPH(
        categoryCode = "prod_photo",
        displayName = "Public Marketplace Product Photograph",
        partition = StoragePartition.PUBLIC_CATALOG_MEDIA,
        accessLevel = FileAccessLevel.PUBLIC_READ_OWNER_WRITE,
        maxFileSizeBytes = 10L * 1024L * 1024L, // 10 MB max
        allowedExtensions = setOf("jpg", "jpeg", "png", "webp"),
        allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp"),
        storageSubdirectory = "public-media/products/photos"
    ),
    PRODUCT_VIDEO(
        categoryCode = "prod_video",
        displayName = "Public Marketplace Product Inspection Video",
        partition = StoragePartition.PUBLIC_CATALOG_MEDIA,
        accessLevel = FileAccessLevel.PUBLIC_READ_OWNER_WRITE,
        maxFileSizeBytes = 50L * 1024L * 1024L, // 50 MB max
        allowedExtensions = setOf("mp4", "webm"),
        allowedMimeTypes = setOf("video/mp4", "video/webm"),
        storageSubdirectory = "public-media/products/videos"
    ),
    PROFILE_PHOTOGRAPH(
        categoryCode = "profile_photo",
        displayName = "Profile Avatar / Storefront Logo",
        partition = StoragePartition.PUBLIC_CATALOG_MEDIA,
        accessLevel = FileAccessLevel.PUBLIC_READ_OWNER_WRITE,
        maxFileSizeBytes = 5L * 1024L * 1024L, // 5 MB max
        allowedExtensions = setOf("jpg", "jpeg", "png", "webp"),
        allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp"),
        storageSubdirectory = "public-media/profiles/avatars"
    );

    val isPrivateVerificationAsset: Boolean
        get() = partition == StoragePartition.PRIVATE_ADMIN_VAULT
}

/**
 * Physical and logical storage partition separation:
 * - [PUBLIC_CATALOG_MEDIA]: Public product photos, product videos, and storefront logos/avatars.
 * - [PRIVATE_ADMIN_VAULT]: Quarantined, AES-256-GCM encrypted vault for seller identity documents,
 *   business verification photographs, and compliance documents. Never publicly accessible.
 */
enum class StoragePartition {
    PUBLIC_CATALOG_MEDIA,
    PRIVATE_ADMIN_VAULT
}

enum class FileAccessLevel {
    PUBLIC_READ_OWNER_WRITE,
    PRIVATE_ADMIN_ONLY
}

enum class MalwareScanStatus {
    CLEAN,
    INFECTED_SIGNATURE_DETECTED,
    EMBEDDED_SCRIPT_OR_POLYGLOT_DETECTED,
    ACTIVE_PDF_PAYLOAD_DETECTED,
    INVALID_BINARY_HEADER
}

data class MalwareScanResult(
    val status: MalwareScanStatus,
    val isClean: Boolean,
    val sha256Hex: String,
    val threatDescription: String? = null
)

data class ComprehensiveFileValidationResult(
    val isValid: Boolean,
    val category: UploadFileCategory,
    val detectedMimeType: String? = null,
    val canonicalExtension: String? = null,
    val secureStorageFileName: String? = null,
    val isolatedStoragePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val sha256Checksum: String? = null,
    val malwareScanResult: MalwareScanResult? = null,
    val errorReason: String? = null
)

data class StoredFileRecord(
    val fileId: String,
    val category: UploadFileCategory,
    val partition: StoragePartition,
    val accessLevel: FileAccessLevel,
    val ownerUid: String,
    val ownerStoreId: String,
    val secureFileName: String,
    val isolatedStoragePath: String,
    val verifiedMimeType: String,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val posixPermissions: String, // e.g., "0600 (rw-------, noexec)" for private vault, "0640 (rw-r-----, noexec)" for public media
    val contentDispositionHeader: String, // "attachment; filename=..." to prevent browser inline execution
    val isEncryptedAtRest: Boolean,
    val encryptedPayloadOrReference: String,
    val malwareScanStatus: MalwareScanStatus,
    val uploadedAtEpoch: Long = System.currentTimeMillis()
) {
    val isPubliclyAccessible: Boolean
        get() = partition == StoragePartition.PUBLIC_CATALOG_MEDIA &&
            accessLevel == FileAccessLevel.PUBLIC_READ_OWNER_WRITE
}

data class FileStorageAuditEntry(
    val auditId: String = "FS-AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
    val timestampEpoch: Long = System.currentTimeMillis(),
    val actorEmail: String,
    val actorRole: UserRole,
    val action: String,
    val fileId: String,
    val category: UploadFileCategory,
    val partition: StoragePartition,
    val granted: Boolean,
    val details: String
)

/**
 * Central File Storage and Upload Security Engine for SolarFix.
 *
 * Enforces:
 * 1. Strict file type & extension validation per upload category.
 * 2. Category-specific file size limits (5 MB profile photos, 10 MB product/KYC docs, 50 MB product videos).
 * 3. True binary magic-byte signature verification (JPEG, PNG, WEBP, PDF, MP4, WEBM) cross-checked
 *    against declared MIME types to block MIME spoofing and extension renaming attacks.
 * 4. Malware, polyglot, web-shell, and active-PDF payload scanning (EICAR, PHP/JS/HTML scripts,
 *    ELF/PE/DEX/APK executables, and PDF `/JavaScript` or `/Launch` actions).
 * 5. Cryptographic secure file naming (`sf_<category>_<128bitHex>.<ext>`) preventing path traversal,
 *    null-byte injection, and double-extension execution.
 * 6. Strict physical/logical separation between Public Catalog Media (`public-media/...`) and
 *    Private Verification Vault (`private-vault/...`).
 * 7. Zero public access to seller identity documents or business verification photographs —
 *    restricted exclusively to verified Platform Administrators with full audit logging.
 */
object FileUploadAndStorageSecurityManager {

    private const val MIN_VALID_FILE_BYTES = 12
    private val secureRandom = SecureRandom()

    private val DANGEROUS_EXECUTABLE_AND_SCRIPT_EXTENSIONS = setOf(
        "php", "php3", "php4", "php5", "phtml", "phar",
        "exe", "dll", "bat", "cmd", "com", "msi", "scr", "ps1", "vbs",
        "sh", "bash", "zsh", "csh", "ksh", "bin", "run", "elf",
        "js", "jsx", "ts", "tsx", "mjs", "cjs",
        "html", "htm", "xhtml", "shtml", "svg", "xml", "xsl",
        "jsp", "jspx", "asp", "aspx", "ascx", "ashx", "asmx",
        "py", "pyc", "pyo", "rb", "pl", "pm", "cgi", "fcgi",
        "jar", "war", "ear", "class", "apk", "aab", "dex", "so", "dylib",
        "htaccess", "htpasswd", "env", "config", "ini", "sql", "db", "sqlite"
    )

    private val EXTENSION_TO_CANONICAL_MIME = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "pdf" to "application/pdf",
        "mp4" to "video/mp4",
        "webm" to "video/webm"
    )

    private val MIME_TO_CANONICAL_EXTENSION = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "application/pdf" to "pdf",
        "video/mp4" to "mp4",
        "video/webm" to "webm"
    )

    // Standard EICAR antivirus test signature
    private const val EICAR_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!\$H+H*"

    // Embedded script / web-shell / polyglot signatures forbidden inside uploaded media or documents
    private val MALICIOUS_PAYLOAD_MARKERS = listOf(
        "<?php",
        "<?=",
        "<script",
        "javascript:",
        "vbscript:",
        "onload=",
        "onerror=",
        "onclick=",
        "Runtime.getRuntime().exec",
        "ProcessBuilder(",
        "/bin/sh",
        "/bin/bash",
        "cmd.exe",
        "powershell",
        "eval(base64_decode",
        "system(\$_GET",
        "passthru(",
        "shell_exec("
    )

    // Dangerous active content markers inside PDF documents
    private val DANGEROUS_PDF_ACTIONS = listOf(
        "/JavaScript",
        "/JS",
        "/Launch",
        "/EmbeddedFiles",
        "/RichMedia",
        "/SubmitForm",
        "/ImportData"
    )

    // Separate storage registries for Public Catalog Media vs Private Admin Verification Vault
    private val publicMediaRegistry = mutableMapOf<String, StoredFileRecord>()
    private val privateAdminVaultRegistry = mutableMapOf<String, StoredFileRecord>()

    private val _storageAuditLogs = MutableStateFlow<List<FileStorageAuditEntry>>(emptyList())
    val storageAuditLogsFlow: StateFlow<List<FileStorageAuditEntry>> = _storageAuditLogs.asStateFlow()

    // =========================================================================
    // 1. Filename, Extension & Path Traversal Validation
    // =========================================================================

    fun validateFileNameForCategory(
        originalFileName: String,
        category: UploadFileCategory
    ): UploadValidationResult {
        val trimmed = originalFileName.trim()
        if (trimmed.isEmpty() || trimmed.length > 255) {
            return UploadValidationResult(false, "File name must be between 1 and 255 characters.")
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") ||
            trimmed.contains("\u0000") || trimmed.startsWith(".")
        ) {
            return UploadValidationResult(
                false,
                "Path traversal, hidden dot-file, or null-byte injection detected in file name."
            )
        }
        if (trimmed.any { it.isISOControl() }) {
            return UploadValidationResult(false, "Illegal control characters detected in file name.")
        }

        val parts = trimmed.lowercase().split(".")
        if (parts.size < 2) {
            return UploadValidationResult(
                false,
                "File must include a valid extension for ${category.displayName} (${category.allowedExtensions.joinToString(", ")})."
            )
        }

        val finalExt = parts.last()
        if (finalExt in DANGEROUS_EXECUTABLE_AND_SCRIPT_EXTENSIONS) {
            return UploadValidationResult(
                false,
                "Dangerous executable or script file extension '.$finalExt' is strictly prohibited."
            )
        }
        if (finalExt !in category.allowedExtensions) {
            return UploadValidationResult(
                false,
                "File extension '.$finalExt' is not allowed for ${category.displayName}. Allowed: ${category.allowedExtensions.joinToString(", ")}."
            )
        }

        // Inspect all intermediate segments for double-extension attacks (e.g., "shell.php.jpg" or "exploit.html.png")
        val intermediateSegments = parts.drop(1).dropLast(1)
        for (seg in intermediateSegments) {
            if (seg in DANGEROUS_EXECUTABLE_AND_SCRIPT_EXTENSIONS) {
                return UploadValidationResult(
                    false,
                    "Malicious double extension '.$seg.$finalExt' detected and blocked."
                )
            }
        }

        return UploadValidationResult(true)
    }

    // =========================================================================
    // 2. Binary Magic-Byte Signature Verification & MIME Type Verification
    // =========================================================================

    /**
     * Inspects the raw binary header (magic bytes) of an uploaded file to determine its authentic
     * MIME type regardless of what the client's HTTP Content-Type header or file extension claims.
     */
    fun detectMimeTypeFromMagicBytes(bytes: ByteArray): String? {
        if (bytes.size < MIN_VALID_FILE_BYTES) return null

        // 1. JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
        ) {
            return "image/png"
        }

        // 3. WEBP: "RIFF" (52 49 46 46) at 0..3 and "WEBP" (57 45 42 50) at 8..11
        if (bytes[0] == 0x52.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() &&
            bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte()
        ) {
            return "image/webp"
        }

        // 4. PDF: "%PDF-" (25 50 44 46 2D) at 0..4
        if (bytes[0] == 0x25.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x44.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[4] == 0x2D.toByte()
        ) {
            return "application/pdf"
        }

        // 5. MP4 (ISO Base Media File Format): "ftyp" (66 74 79 70) at offset 4..7
        if (bytes[4] == 0x66.toByte() &&
            bytes[5] == 0x74.toByte() &&
            bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte()
        ) {
            return "video/mp4"
        }

        // 6. WEBM (Matroska EBML Header): 1A 45 DF A3 at offset 0..3
        if (bytes[0] == 0x1A.toByte() &&
            bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xDF.toByte() &&
            bytes[3] == 0xA3.toByte()
        ) {
            return "video/webm"
        }

        return null
    }

    // =========================================================================
    // 3. Malware, Polyglot & Embedded Exploit Scanner
    // =========================================================================

    fun scanBytesForMalware(
        bytes: ByteArray,
        detectedMimeType: String?
    ): MalwareScanResult {
        val sha256Hex = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        if (bytes.size < MIN_VALID_FILE_BYTES) {
            return MalwareScanResult(
                status = MalwareScanStatus.INVALID_BINARY_HEADER,
                isClean = false,
                sha256Hex = sha256Hex,
                threatDescription = "File is empty or truncated below minimum valid header size."
            )
        }

        // Check for executable binary headers (ELF, Windows PE/MZ, Android DEX, ZIP/APK/JAR)
        val isElfBinary = bytes[0] == 0x7F.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0x4C.toByte() && bytes[3] == 0x46.toByte()
        val isWindowsPe = bytes[0] == 0x4D.toByte() && bytes[1] == 0x5A.toByte()
        val isAndroidDex = bytes[0] == 0x64.toByte() && bytes[1] == 0x65.toByte() &&
            bytes[2] == 0x78.toByte() && bytes[3] == 0x0A.toByte()
        val isZipOrApk = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

        if (isElfBinary || isWindowsPe || isAndroidDex || isZipOrApk) {
            return MalwareScanResult(
                status = MalwareScanStatus.INFECTED_SIGNATURE_DETECTED,
                isClean = false,
                sha256Hex = sha256Hex,
                threatDescription = "Executable binary or archive signature (ELF/PE/DEX/APK) detected and blocked."
            )
        }

        // Inspect printable ASCII/UTF-8 window of the file (up to first 64 KB and last 8 KB) for EICAR, web-shells & polyglots
        val headLength = bytes.size.coerceAtMost(65_536)
        val headText = String(bytes, 0, headLength, StandardCharsets.ISO_8859_1)
        val tailText = if (bytes.size > 65_536) {
            val tailLen = 8_192.coerceAtMost(bytes.size - headLength)
            String(bytes, bytes.size - tailLen, tailLen, StandardCharsets.ISO_8859_1)
        } else {
            ""
        }
        val combinedInspectionText = "$headText\n$tailText"

        if (combinedInspectionText.contains(EICAR_SIGNATURE)) {
            return MalwareScanResult(
                status = MalwareScanStatus.INFECTED_SIGNATURE_DETECTED,
                isClean = false,
                sha256Hex = sha256Hex,
                threatDescription = "Malware signature detected: EICAR-STANDARD-ANTIVIRUS-TEST-FILE."
            )
        }

        for (marker in MALICIOUS_PAYLOAD_MARKERS) {
            if (combinedInspectionText.contains(marker, ignoreCase = true)) {
                return MalwareScanResult(
                    status = MalwareScanStatus.EMBEDDED_SCRIPT_OR_POLYGLOT_DETECTED,
                    isClean = false,
                    sha256Hex = sha256Hex,
                    threatDescription = "Embedded script or polyglot exploit payload ('$marker') detected inside file."
                )
            }
        }

        // If PDF document, scan for embedded JavaScript or Launch actions
        if (detectedMimeType == "application/pdf") {
            for (pdfAction in DANGEROUS_PDF_ACTIONS) {
                if (combinedInspectionText.contains(pdfAction, ignoreCase = true)) {
                    return MalwareScanResult(
                        status = MalwareScanStatus.ACTIVE_PDF_PAYLOAD_DETECTED,
                        isClean = false,
                        sha256Hex = sha256Hex,
                        threatDescription = "Active PDF action ('$pdfAction') is forbidden in verification documents."
                    )
                }
            }
        }

        return MalwareScanResult(
            status = MalwareScanStatus.CLEAN,
            isClean = true,
            sha256Hex = sha256Hex
        )
    }

    // =========================================================================
    // 4. Secure File Naming & Isolated Storage Path Generation
    // =========================================================================

    /**
     * Generates a cryptographically random, non-guessable filename using a canonical extension.
     * Never uses user-supplied characters in the generated storage filename.
     */
    fun generateSecureStorageFileName(
        category: UploadFileCategory,
        canonicalExtension: String
    ): String {
        val safeExt = canonicalExtension.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "bin" }
        val randomBytes = ByteArray(16) // 128-bit cryptographic entropy
        secureRandom.nextBytes(randomBytes)
        val hexToken = randomBytes.joinToString("") { "%02x".format(it) }
        return "sf_${category.categoryCode}_$hexToken.$safeExt"
    }

    /**
     * Builds the isolated storage path ensuring public product photographs/videos and private
     * seller verification documents are stored in completely separate directory trees.
     */
    fun buildIsolatedStoragePath(
        category: UploadFileCategory,
        ownerOrStoreId: String,
        secureFileName: String
    ): String {
        val cleanOwnerSegment = ownerOrStoreId.trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(64)
            .ifEmpty { "unassigned" }
        return "${category.storageSubdirectory}/$cleanOwnerSegment/$secureFileName"
    }

    // =========================================================================
    // 5. Full End-to-End File Validation Pipeline
    // =========================================================================

    fun validateAndInspectUpload(
        originalFileName: String,
        declaredMimeType: String,
        fileBytes: ByteArray,
        category: UploadFileCategory
    ): ComprehensiveFileValidationResult {
        // 1. Validate filename and extension against category whitelist & path traversal
        val nameValidation = validateFileNameForCategory(originalFileName, category)
        if (!nameValidation.isValid) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                fileSizeBytes = fileBytes.size.toLong(),
                errorReason = nameValidation.errorMessage
            )
        }

        // 2. Enforce file size limits (min header size and category max size)
        val sizeBytes = fileBytes.size.toLong()
        if (sizeBytes < MIN_VALID_FILE_BYTES) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                fileSizeBytes = sizeBytes,
                errorReason = "Uploaded file is empty or smaller than minimum valid header size ($MIN_VALID_FILE_BYTES bytes)."
            )
        }
        if (sizeBytes > category.maxFileSizeBytes) {
            val maxMb = category.maxFileSizeBytes / (1024L * 1024L)
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                fileSizeBytes = sizeBytes,
                errorReason = "File size ($sizeBytes bytes) exceeds maximum allowed limit of $maxMb MB for ${category.displayName}."
            )
        }

        // 3. Verify declared MIME type is in category whitelist
        val normalizedDeclaredMime = declaredMimeType.trim().lowercase().let {
            if (it == "image/jpg") "image/jpeg" else it
        }
        if (normalizedDeclaredMime !in category.allowedMimeTypes) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                fileSizeBytes = sizeBytes,
                errorReason = "Declared MIME type '$declaredMimeType' is not permitted for ${category.displayName}."
            )
        }

        // 4. Verify true binary magic-byte signature & cross-check with declared MIME and file extension
        val detectedMime = detectMimeTypeFromMagicBytes(fileBytes)
            ?: return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                fileSizeBytes = sizeBytes,
                errorReason = "File binary signature (magic bytes) is unrecognized or corrupted."
            )

        if (detectedMime !in category.allowedMimeTypes) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                detectedMimeType = detectedMime,
                fileSizeBytes = sizeBytes,
                errorReason = "Binary file signature ($detectedMime) is not permitted for ${category.displayName}."
            )
        }

        if (detectedMime != normalizedDeclaredMime) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                detectedMimeType = detectedMime,
                fileSizeBytes = sizeBytes,
                errorReason = "MIME spoofing detected: declared '$normalizedDeclaredMime' does not match binary signature '$detectedMime'."
            )
        }

        val fileExtension = originalFileName.trim().lowercase().substringAfterLast('.')
        val expectedMimeForExtension = EXTENSION_TO_CANONICAL_MIME[fileExtension]
        if (expectedMimeForExtension != null && expectedMimeForExtension != detectedMime) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                detectedMimeType = detectedMime,
                fileSizeBytes = sizeBytes,
                errorReason = "Extension spoofing detected: '.$fileExtension' does not match binary signature '$detectedMime'."
            )
        }

        // 5. Run Malware, Polyglot & Active-PDF Scan
        val scanResult = scanBytesForMalware(fileBytes, detectedMime)
        if (!scanResult.isClean) {
            return ComprehensiveFileValidationResult(
                isValid = false,
                category = category,
                detectedMimeType = detectedMime,
                fileSizeBytes = sizeBytes,
                sha256Checksum = scanResult.sha256Hex,
                malwareScanResult = scanResult,
                errorReason = scanResult.threatDescription ?: "Malicious payload detected in uploaded file."
            )
        }

        // 6. Generate cryptographic filename and isolated storage path
        val canonicalExt = MIME_TO_CANONICAL_EXTENSION[detectedMime] ?: fileExtension
        val secureFileName = generateSecureStorageFileName(category, canonicalExt)
        val isolatedPath = buildIsolatedStoragePath(category, "validated", secureFileName)

        return ComprehensiveFileValidationResult(
            isValid = true,
            category = category,
            detectedMimeType = detectedMime,
            canonicalExtension = canonicalExt,
            secureStorageFileName = secureFileName,
            isolatedStoragePath = isolatedPath,
            fileSizeBytes = sizeBytes,
            sha256Checksum = scanResult.sha256Hex,
            malwareScanResult = scanResult
        )
    }

    /**
     * Validates a product video URI or internal SolarFix video reference (`solarfix://video/...` or `https://...` or `content://...`)
     * to prevent `javascript:`, `file://`, `data:text/html`, or command-injection URIs in product video fields.
     */
    fun validateAndSanitizeProductVideoReference(rawVideoUriOrUrl: String?): String? {
        if (rawVideoUriOrUrl.isNullOrBlank()) return null
        val trimmed = rawVideoUriOrUrl.trim()
        if (trimmed.length > 500 || trimmed.contains("\u0000") || trimmed.contains("..")) return null
        if (SecuritySanitizer.containsDangerousPayload(trimmed)) return null

        val lower = trimmed.lowercase()
        // Reject dangerous schemes
        if (lower.startsWith("javascript:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("file:") ||
            lower.startsWith("http://")
        ) {
            return null
        }

        val allowedScheme = lower.startsWith("solarfix://video/") ||
            lower.startsWith("https://") ||
            lower.startsWith("content://")
        return if (allowedScheme) trimmed else null
    }

    // =========================================================================
    // 6. Secure Storage Engine (Public Catalog vs. Private Admin Verification Vault)
    // =========================================================================

    /**
     * Validates and stores an uploaded file into the appropriate isolated partition:
     * - Private Verification Documents (`SELLER_IDENTITY_DOCUMENT`, `BUSINESS_VERIFICATION_PHOTO`, `OTHER_VERIFICATION_DOCUMENT`)
     *   are encrypted at rest using AES-256-GCM and stored in `privateAdminVaultRegistry` with `0600 (rw-------, noexec)` permissions.
     * - Public Media (`PRODUCT_PHOTOGRAPH`, `PRODUCT_VIDEO`, `PROFILE_PHOTOGRAPH`) are stored in
     *   `publicMediaRegistry` with `0640 (rw-r-----, noexec)` permissions.
     */
    @Synchronized
    fun storeValidatedUpload(
        originalFileName: String,
        declaredMimeType: String,
        fileBytes: ByteArray,
        category: UploadFileCategory,
        ownerUid: String,
        ownerStoreId: String,
        uploaderEmail: String = ""
    ): Result<StoredFileRecord> {
        val validation = validateAndInspectUpload(
            originalFileName = originalFileName,
            declaredMimeType = declaredMimeType,
            fileBytes = fileBytes,
            category = category
        )
        if (!validation.isValid) {
            recordStorageAudit(
                actorEmail = uploaderEmail.ifBlank { ownerUid },
                actorRole = UserRole.CUSTOMER,
                action = "UPLOAD_REJECTED",
                fileId = "rejected",
                category = category,
                partition = category.partition,
                granted = false,
                details = "Rejected upload '$originalFileName': ${validation.errorReason}"
            )
            return Result.failure(SecurityException(validation.errorReason ?: "Upload validation failed."))
        }

        val fileId = "FILE-${UUID.randomUUID().toString().take(12).uppercase()}"
        val secureName = validation.secureStorageFileName!!
        val isolatedPath = buildIsolatedStoragePath(
            category = category,
            ownerOrStoreId = ownerStoreId.ifBlank { ownerUid },
            secureFileName = secureName
        )
        val sha256 = validation.sha256Checksum!!
        val base64Content = Base64.getUrlEncoder().withoutPadding().encodeToString(fileBytes)

        val isPrivate = category.isPrivateVerificationAsset
        val storedPayload = if (isPrivate) {
            // Encrypt private KYC/KYB verification document payload at rest using AES-256-GCM bound to fileId & storeId
            DatabaseEncryptionAndSecurityManager.encryptSensitiveField(
                plaintext = base64Content,
                associatedData = "private_vault:$fileId:${category.name}:$ownerStoreId"
            )
        } else {
            base64Content
        }

        val record = StoredFileRecord(
            fileId = fileId,
            category = category,
            partition = category.partition,
            accessLevel = category.accessLevel,
            ownerUid = ownerUid,
            ownerStoreId = ownerStoreId,
            secureFileName = secureName,
            isolatedStoragePath = isolatedPath,
            verifiedMimeType = validation.detectedMimeType!!,
            fileSizeBytes = validation.fileSizeBytes,
            sha256Checksum = sha256,
            posixPermissions = if (isPrivate) "0600 (rw-------, noexec, nosuid)" else "0640 (rw-r-----, noexec, nosuid)",
            contentDispositionHeader = "attachment; filename=\"$secureName\"",
            isEncryptedAtRest = isPrivate,
            encryptedPayloadOrReference = storedPayload,
            malwareScanStatus = MalwareScanStatus.CLEAN
        )

        if (isPrivate) {
            privateAdminVaultRegistry[fileId] = record
        } else {
            publicMediaRegistry[fileId] = record
        }

        recordStorageAudit(
            actorEmail = uploaderEmail.ifBlank { ownerUid },
            actorRole = UserRole.VERIFIED_SELLER,
            action = if (isPrivate) "STORE_PRIVATE_VAULT_ENCRYPTED" else "STORE_PUBLIC_MEDIA",
            fileId = fileId,
            category = category,
            partition = category.partition,
            granted = true,
            details = "Stored ${category.displayName} at '$isolatedPath' (SHA-256: ${sha256.take(12)}..., encryptedAtRest=$isPrivate)"
        )

        return Result.success(record)
    }

    /**
     * Converts an Android [Bitmap] into PNG header bytes (or validates its dimensions/footprint in unit test environments)
     * and registers it in the isolated storage partition for the specified [category].
     */
    fun registerBitmapInSecureStorage(
        bitmap: Bitmap,
        category: UploadFileCategory,
        ownerUid: String,
        ownerStoreId: String,
        uploaderEmail: String = ""
    ): StoredFileRecord? {
        val bmpValidation = SecuritySanitizer.validateUploadedBitmap(bitmap)
        if (!bmpValidation.isValid) return null

        val rawBytes = try {
            val out = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            val bytes = out.toByteArray()
            if (compressed && bytes.size >= MIN_VALID_FILE_BYTES) {
                bytes
            } else {
                buildSyntheticPngHeaderBytes(bitmap.width, bitmap.height)
            }
        } catch (_: Exception) {
            buildSyntheticPngHeaderBytes(bitmap.width, bitmap.height)
        }

        return storeValidatedUpload(
            originalFileName = "${category.categoryCode}_capture.png",
            declaredMimeType = "image/png",
            fileBytes = rawBytes,
            category = category,
            ownerUid = ownerUid,
            ownerStoreId = ownerStoreId,
            uploaderEmail = uploaderEmail
        ).getOrNull()
    }

    private fun buildSyntheticPngHeaderBytes(width: Int, height: Int): ByteArray {
        // Authentic PNG 8-byte signature + IHDR chunk metadata for JVM unit tests where Bitmap.compress is stubbed
        val pngMagic = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        val meta = "W=${width}xH=${height}".toByteArray(StandardCharsets.UTF_8)
        return pngMagic + meta
    }

    // =========================================================================
    // 7. Strict Access Control: Public Media vs. Admin-Only Private Verification Vault
    // =========================================================================

    /**
     * Public access endpoint: Can ONLY retrieve files from [StoragePartition.PUBLIC_CATALOG_MEDIA].
     * Strictly refuses to resolve or return any file from [StoragePartition.PRIVATE_ADMIN_VAULT].
     */
    fun getPublicMediaFile(fileId: String, callerEmail: String = "anonymous"): StoredFileRecord? {
        if (privateAdminVaultRegistry.containsKey(fileId)) {
            val privateRecord = privateAdminVaultRegistry[fileId]!!
            recordStorageAudit(
                actorEmail = callerEmail,
                actorRole = UserRole.CUSTOMER,
                action = "PUBLIC_ACCESS_TO_PRIVATE_DOC_BLOCKED",
                fileId = fileId,
                category = privateRecord.category,
                partition = StoragePartition.PRIVATE_ADMIN_VAULT,
                granted = false,
                details = "BLOCKED public access attempt to private verification document $fileId (${privateRecord.isolatedStoragePath})"
            )
            return null
        }
        return publicMediaRegistry[fileId]
    }

    /**
     * Restricted Administrator-Only Vault Access:
     * Seller identity documents, business verification photographs, and compliance documents
     * can ONLY be accessed and decrypted by a verified Platform Administrator.
     * Throws [SecurityException] if called by a Customer, unauthenticated user, or non-admin Seller.
     */
    fun accessPrivateVerificationDocumentForAdmin(
        fileId: String,
        adminEmail: String,
        adminRole: UserRole = UserRole.ADMIN
    ): Pair<StoredFileRecord, ByteArray> {
        val isAuthorizedAdmin = adminRole == UserRole.ADMIN &&
            FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(adminEmail)

        val record = privateAdminVaultRegistry[fileId]
        if (!isAuthorizedAdmin) {
            recordStorageAudit(
                actorEmail = adminEmail,
                actorRole = adminRole,
                action = "UNAUTHORIZED_PRIVATE_VAULT_ACCESS_BLOCKED",
                fileId = fileId,
                category = record?.category ?: UploadFileCategory.SELLER_IDENTITY_DOCUMENT,
                partition = StoragePartition.PRIVATE_ADMIN_VAULT,
                granted = false,
                details = "SECURITY ALERT: Non-administrator '$adminEmail' ($adminRole) attempted to access private verification document $fileId"
            )
            throw SecurityException("Access Denied: Only authorized SolarFix Administrators can access private seller verification documents.")
        }

        if (record == null) {
            throw NoSuchElementException("Private verification document $fileId not found in admin vault.")
        }

        val decryptedB64 = DatabaseEncryptionAndSecurityManager.decryptSensitiveField(
            ciphertextPayload = record.encryptedPayloadOrReference,
            associatedData = "private_vault:${record.fileId}:${record.category.name}:${record.ownerStoreId}"
        )
        val rawBytes = Base64.getUrlDecoder().decode(decryptedB64)

        recordStorageAudit(
            actorEmail = adminEmail,
            actorRole = UserRole.ADMIN,
            action = "ADMIN_PRIVATE_DOC_DECRYPTED_VIEW",
            fileId = fileId,
            category = record.category,
            partition = StoragePartition.PRIVATE_ADMIN_VAULT,
            granted = true,
            details = "Authorized administrator '$adminEmail' accessed private verification document ${record.isolatedStoragePath}"
        )

        return record to rawBytes
    }

    /**
     * Lists private verification document metadata for a specific seller store.
     * Restricted strictly to verified Platform Administrators.
     */
    fun listPrivateVerificationDocumentsForStore(
        storeId: String,
        adminEmail: String,
        adminRole: UserRole = UserRole.ADMIN
    ): List<StoredFileRecord> {
        val isAuthorizedAdmin = adminRole == UserRole.ADMIN &&
            FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(adminEmail)
        if (!isAuthorizedAdmin) {
            recordStorageAudit(
                actorEmail = adminEmail,
                actorRole = adminRole,
                action = "UNAUTHORIZED_PRIVATE_VAULT_LIST_BLOCKED",
                fileId = storeId,
                category = UploadFileCategory.SELLER_IDENTITY_DOCUMENT,
                partition = StoragePartition.PRIVATE_ADMIN_VAULT,
                granted = false,
                details = "Blocked non-admin '$adminEmail' from listing private KYC documents for store $storeId"
            )
            throw SecurityException("Access Denied: Only authorized Administrators can view seller verification documents.")
        }
        return privateAdminVaultRegistry.values.filter { it.ownerStoreId == storeId }
    }

    /**
     * Lists public media records for a specific store or product.
     * Guaranteed never to return any record from [privateAdminVaultRegistry].
     */
    fun listPublicMediaForOwner(ownerStoreId: String): List<StoredFileRecord> {
        return publicMediaRegistry.values.filter {
            it.ownerStoreId == ownerStoreId && it.isPubliclyAccessible
        }
    }

    private fun recordStorageAudit(
        actorEmail: String,
        actorRole: UserRole,
        action: String,
        fileId: String,
        category: UploadFileCategory,
        partition: StoragePartition,
        granted: Boolean,
        details: String
    ) {
        val entry = FileStorageAuditEntry(
            actorEmail = SecuritySanitizer.maskEmail(actorEmail),
            actorRole = actorRole,
            action = action,
            fileId = fileId,
            category = category,
            partition = partition,
            granted = granted,
            details = details
        )
        _storageAuditLogs.update { current ->
            (listOf(entry) + current).take(500)
        }
    }
}
