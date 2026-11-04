package com.example.data.repository

import com.example.data.model.*
import com.example.data.payment.*
import com.example.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.roundToLong

class SolarRepository {

    // --- Inverter Error Code Database ---
    val errorCodes: List<InverterErrorCode> = listOf(
        InverterErrorCode(
            brand = InverterBrand.GROWATT,
            code = "Error 04",
            title = "Low Battery Voltage",
            probableCause = "Battery pack voltage has dropped below the inverter cutoff threshold (e.g. <44V for 48V system or <21V for 24V system).",
            stepByStepFix = listOf(
                "Shed non-essential heavy appliances (freezers, ACs, water heaters) immediately.",
                "Check solar charging display to ensure solar PV breaker has not tripped.",
                "Inspect battery terminals for loose bolts or white sulfate/copper corrosion.",
                "If grid/generator is available, turn on AC input to allow grid bulk charging."
            ),
            severity = ErrorSeverity.WARNING,
            safetyWarning = "Do not short battery terminals with metallic tools while checking voltage."
        ),
        InverterErrorCode(
            brand = InverterBrand.GROWATT,
            code = "Error 02",
            title = "Over Temperature / Heat Sink Too Hot",
            probableCause = "Internal heatsink exceeded 90°C due to blocked cooling fans, heavy continuous load, or poorly ventilated inverter room.",
            stepByStepFix = listOf(
                "Turn off heavy loads immediately to lower operating temperature.",
                "Inspect cooling fans at the bottom/side for dust clogs, hair, or spiderwebs.",
                "Ensure minimum 30cm clearance around the inverter casing for proper cross-ventilation.",
                "Allow 15-20 minutes cooldown before restarting inverter."
            ),
            severity = ErrorSeverity.WARNING
        ),
        InverterErrorCode(
            brand = InverterBrand.GROWATT,
            code = "Error 08",
            title = "DC Bus Voltage Too High",
            probableCause = "PV string open circuit voltage (Voc) exceeded inverter maximum DC input, or high voltage surge from grid / lightning.",
            stepByStepFix = listOf(
                "Turn OFF the DC Solar Array isolator breaker immediately.",
                "Verify panel series connection — count panels in series to ensure total Voc does not exceed inverter rating (usually 450V or 500V max).",
                "During cold morning hours, panel Voc rises by up to 15%. Allow installer to reconfigure panel strings in parallel if necessary."
            ),
            severity = ErrorSeverity.CRITICAL,
            safetyWarning = "High DC voltage risk (>300V DC). Do not touch open PV cable terminals!"
        ),
        InverterErrorCode(
            brand = InverterBrand.GROWATT,
            code = "Error 51",
            title = "Over Current / Output Surge Inverter Shutdown",
            probableCause = "Inrush motor current (e.g., borehole submersible pump, induction motor, or large compressor starting) exceeded maximum peak surge rating.",
            stepByStepFix = listOf(
                "Disconnect the appliance that was turned on right before the trip.",
                "Power down the inverter switch, wait 30 seconds, and turn back on.",
                "For borehole pumps, install a Variable Frequency Drive (VFD) or motor soft-starter to tame initial starting surge."
            ),
            severity = ErrorSeverity.CRITICAL
        ),
        InverterErrorCode(
            brand = InverterBrand.FELICITY,
            code = "Fault 04",
            title = "AC Inverter Overload Shutdown",
            probableCause = "Connected household/office appliances exceed 110% of rated continuous inverter capacity for over 1 minute, or 150% for 10 seconds.",
            stepByStepFix = listOf(
                "Unplug high-draw appliances: electric iron, boiling kettle, water heater, microwave.",
                "Wait 60 seconds for inverter automatic restart or toggle the reset switch.",
                "Distribute appliances between day time (when solar PV is actively producing) and night time."
            ),
            severity = ErrorSeverity.WARNING
        ),
        InverterErrorCode(
            brand = InverterBrand.FELICITY,
            code = "Fault 06",
            title = "Output Short-Circuit Protection",
            probableCause = "Direct short circuit in household AC wiring, burnt socket, or damaged appliance power cord.",
            stepByStepFix = listOf(
                "Switch OFF all AC output distribution sub-breakers in your distribution board.",
                "Reset inverter. If inverter powers on cleanly, turn on sub-breakers one by one.",
                "When a specific breaker trips the inverter again, inspect that specific room circuit for burnt plugs or rodent-chewed cables."
            ),
            severity = ErrorSeverity.CRITICAL,
            safetyWarning = "Risk of electrical fire. Do not bypass the inverter breaker."
        ),
        InverterErrorCode(
            brand = InverterBrand.MUST,
            code = "Error 04",
            title = "Battery Discharged / Low Voltage Shutdown",
            probableCause = "Batteries depleted below cutoff protection. Often caused by prolonged rainy/overcast weather or aged battery cells with high internal resistance.",
            stepByStepFix = listOf(
                "Shed all loads and allow the solar charge controller to recharge batteries in Bulk and Absorption modes.",
                "If using tubular batteries, inspect electrolyte water levels in each cell cap and top up with distilled water if plates are exposed.",
                "If using lithium batteries, check if BMS entered low-voltage lock state."
            ),
            severity = ErrorSeverity.WARNING
        ),
        InverterErrorCode(
            brand = InverterBrand.MUST,
            code = "Error 07",
            title = "Overload Exceeded Protection",
            probableCause = "Connected load is greater than inverter rated wattage.",
            stepByStepFix = listOf(
                "Reduce total wattage by turning off air conditioning units and laundry machines.",
                "Press the power button off for 5 seconds and restart.",
                "Review load calculation to determine if system capacity upgrade is needed."
            ),
            severity = ErrorSeverity.WARNING
        ),
        InverterErrorCode(
            brand = InverterBrand.LUMINOUS,
            code = "Low Batt Alarm",
            title = "Continuous Low Battery Buzzer Beeping",
            probableCause = "Battery terminal voltage below 10.5V per 12V battery. System will shut down within 2-3 minutes.",
            stepByStepFix = listOf(
                "Immediately turn off TV, ceiling fans, and desktop computers to preserve battery life.",
                "Ensure charger selector switch at the rear matches battery type (Tubular / Flat Plate / SMF).",
                "Ensure AC mains wall socket is switched on and supplying minimum 180V for charging."
            ),
            severity = ErrorSeverity.WARNING
        ),
        InverterErrorCode(
            brand = InverterBrand.DEYE,
            code = "F18",
            title = "AC Output Overcurrent",
            probableCause = "High instantaneous current on load side during heavy load transition or phase imbalance.",
            stepByStepFix = listOf(
                "Turn off large inductive appliances.",
                "Verify neutral and ground bonding wiring in distribution box.",
                "Check Deye screen load percentage graph to confirm load peaks."
            ),
            severity = ErrorSeverity.CRITICAL
        ),
        InverterErrorCode(
            brand = InverterBrand.DEYE,
            code = "F41",
            title = "PV DC Arc Fault Detected",
            probableCause = "Loose MC4 connector on roof, cracked solar panel junction box, or chewed DC cable causing dangerous electrical sparking.",
            stepByStepFix = listOf(
                "Turn off DC isolator immediately to stop arcing.",
                "Do not attempt to touch roof wiring without insulated technician gloves.",
                "Call certified installer to inspect MC4 connectors with a thermal camera or multimeter."
            ),
            severity = ErrorSeverity.CRITICAL,
            safetyWarning = "DC arcs can reach 3000°C and ignite roof timbers or dry leaves. Immediate inspection required!"
        )
    )

    // --- Common Diagnostic Symptoms ---
    val symptoms: List<SymptomDiagnostic> = listOf(
        SymptomDiagnostic(
            id = "sym_batt_drain",
            title = "Battery drops rapidly at night",
            iconName = "battery_alert",
            description = "Battery shows 100% at sunset but drops down to 20% or shuts down within 1-2 hours under moderate load.",
            quickDiagnosis = "Likely battery sulfation (tubular/gel), unbalanced cell voltage, or unexpected phantom heavy load.",
            steps = listOf(
                "Check for hidden heavy loads: pumping machines, old deep freezers with degraded compressors, or electric water heaters running unnoticed.",
                "Test individual battery voltages with a digital multimeter 30 minutes after sunset. If one 12V battery reads 10.8V while others read 12.6V, that weak battery is dragging down the whole bank.",
                "For lithium batteries, check BMS Bluetooth app for cell voltage imbalance (>0.15V delta between cells).",
                "Ensure inverter float and absorption charging voltages match the battery manufacturer's datasheet."
            ),
            professionalRequired = false
        ),
        SymptomDiagnostic(
            id = "sym_low_solar_yield",
            title = "Low solar generation on sunny days",
            iconName = "wb_sunny",
            description = "Midday sunshine is intense, but the inverter or MPPT charge controller shows low amps or watts (e.g. only 300W out of a 3000W array).",
            quickDiagnosis = "Harsh dust/Harmattan soot buildup on panel glass, partial shade from trees, or blown DC string fuse.",
            steps = listOf(
                "Safely inspect panel surfaces: Harmattan dust and diesel generator exhaust soot can reduce output by up to 40-60%. Wash panels with soft water and microfiber mop early in the morning.",
                "Check for shading: Even a single tree branch or satellite dish casting shade on one cell can choke the entire string in series.",
                "Check DC breaker and surge protection devices (SPD) in the combiner box to ensure no blown fuses.",
                "Verify open-circuit voltage (Voc) at the inverter DC terminal using a DC multimeter."
            ),
            professionalRequired = false
        ),
        SymptomDiagnostic(
            id = "sym_inverter_shock",
            title = "Electric shock on touching inverter body",
            iconName = "bolt",
            description = "Feeling a tingling sensation or electric shock when touching the metallic body of the inverter, battery rack, or solar panel aluminum frames.",
            quickDiagnosis = "Dangerous missing or broken Earth/Grounding connection. Floating AC neutral or leakage current.",
            steps = listOf(
                "Wear rubber-soled footwear immediately. Do not touch bare metal frames with bare hands or wet feet.",
                "Inspect the earth grounding wire (green/yellow) connecting the inverter chassis to the earthing copper rod.",
                "Check earth pit moisture: During dry seasons, pour saline water around the earth rod pit to reduce soil earth resistance below 5 Ohms.",
                "Hire a certified electrician to test earth loop impedance with an Earth Megger tester."
            ),
            professionalRequired = true
        ),
        SymptomDiagnostic(
            id = "sym_pump_trip",
            title = "Inverter trips whenever borehole pump starts",
            iconName = "water_drop",
            description = "System powers lights and TV fine, but when the water pumping machine turns on, the inverter beeps and shuts off with Overload/Error 51.",
            quickDiagnosis = "Pumping motors draw 3x to 6x their rated power in starting inrush current. Inverter peak surge is exceeded.",
            steps = listOf(
                "Check pump horsepower: A 1HP pump is ~750W running, but can draw 3,500W surge on startup. A 2HP pump draws up to 6,000W surge.",
                "Temporarily shed other heavy household loads before starting the pump.",
                "Consider installing a Soft-Starter or Solar Pump Inverter (VFD) to eliminate sudden current spikes.",
                "Verify battery bank capacity: Undersized battery cables (e.g. 10mm² instead of 25mm² or 35mm²) will cause battery voltage to collapse under surge."
            ),
            professionalRequired = false
        ),
        SymptomDiagnostic(
            id = "sym_inverter_fan_noise",
            title = "Inverter fan running non-stop and very loud",
            iconName = "air",
            description = "Cooling fans operate at maximum speed continuously even when the room is cool and load is low.",
            quickDiagnosis = "Clogged air intake filters, failing fan bearing, or internal dust buildup insulating the temperature sensor.",
            steps = listOf(
                "Power down inverter safely, wait for lights to turn off, and vacuum or brush away dust from air grilles.",
                "Check if inverter is enclosed in a tight, unventilated cabinet.",
                "If fan makes a grinding/screeching sound, the bearing has worn out and needs replacement to prevent overheating."
            ),
            professionalRequired = false
        )
    )

    // --- Verified Certified Technicians Directory ---
    val certifiedTechnicians: List<TechnicianContact> = listOf(
        TechnicianContact(
            id = "tech_1",
            name = "Engr. Emmanuel Okafor",
            businessName = "Apex Solar & Inverter Engineering",
            location = "Ikeja & Victoria Island, Lagos, Nigeria",
            phone = "+234 803 456 7890",
            rating = 4.9f,
            completedJobs = 340,
            badges = listOf("Deye Certified", "Felicity Master Installer", "High Voltage Safety")
        ),
        TechnicianContact(
            id = "tech_2",
            name = "Babatunde Alabi",
            businessName = "SunPower Solutions",
            location = "Garki & Maitama, Abuja, Nigeria",
            phone = "+234 802 987 6543",
            rating = 4.8f,
            completedJobs = 215,
            badges = listOf("Growatt Specialist", "Lithium Battery Retrofit")
        ),
        TechnicianContact(
            id = "tech_3",
            name = "Chukwuma Davis",
            businessName = "Niger Delta Clean Energy Tech",
            location = "Port Harcourt, Rivers State, Nigeria",
            phone = "+234 805 112 3344",
            rating = 4.9f,
            completedJobs = 180,
            badges = listOf("Borehole Solar Pumping", "Industrial Solar")
        ),
        TechnicianContact(
            id = "tech_4",
            name = "Kiprono Mwangi",
            businessName = "Rift Valley Solar Engineers",
            location = "Nairobi & Nakuru, Kenya",
            phone = "+254 712 345 678",
            rating = 4.9f,
            completedJobs = 290,
            badges = listOf("EPRA Class T3 Licensed", "Victron Energy Pro")
        ),
        TechnicianContact(
            id = "tech_5",
            name = "Kwame Mensah",
            businessName = "GoldCoast Solar Mechanics",
            location = "Accra & Tema, Ghana",
            phone = "+233 24 555 8899",
            rating = 4.8f,
            completedJobs = 150,
            badges = listOf("Certified Energy Auditor", "Solar Farm Diagnostics")
        )
    )

    // Backward-compatible accessors backed by shared state
    val products: List<MarketplaceProduct>
        get() = _sharedProducts.value

    val storesFlow: StateFlow<List<SellerStore>> = _sharedStores.asStateFlow()
    val productsFlow: StateFlow<List<MarketplaceProduct>> = _sharedProducts.asStateFlow()
    val reviewsFlow: StateFlow<List<ProductReview>> = _sharedReviews.asStateFlow()
    val addressesFlow: StateFlow<List<DeliveryAddress>> = _sharedAddresses.asStateFlow()
    val ordersFlow: StateFlow<List<MarketplaceOrder>> = _sharedOrders.asStateFlow()
    val disputesFlow: StateFlow<List<ProductDispute>> = _sharedDisputes.asStateFlow()
    val withdrawalsFlow: StateFlow<List<WithdrawalRequest>> = _sharedWithdrawals.asStateFlow()
    val transactionsFlow: StateFlow<List<MarketplaceTransaction>> = _sharedTransactions.asStateFlow()
    val settingsFlow: StateFlow<MarketplaceSettings> = _sharedSettings.asStateFlow()
    val adminAuditLogFlow: StateFlow<List<AdminActionAuditRecord>> = _sharedAdminAuditLogs.asStateFlow()
    val databaseActivityLogsFlow: StateFlow<List<DatabaseActivityLogEntry>> = DatabaseEncryptionAndSecurityManager.activityLogsFlow
    val fileStorageAuditLogsFlow: StateFlow<List<FileStorageAuditEntry>> = FileUploadAndStorageSecurityManager.storageAuditLogsFlow
    val financialAuditLedgerFlow: StateFlow<List<FinancialAuditLedgerEntry>> = RateLimitingAndFinancialSecurityManager.financialAuditLedgerFlow
    val abuseEventsFlow: StateFlow<List<AbuseActivityEvent>> = RateLimitingAndFinancialSecurityManager.abuseEventsFlow
    val adminSecurityAlertsFlow: StateFlow<List<com.example.util.AdminSecurityAlert>> = com.example.util.MobileSecurityAndMonitoringManager.adminSecurityAlertsFlow
    val securityAndFinancialAuditLogFlow: StateFlow<List<com.example.util.UnifiedSecurityAuditEntry>> = com.example.util.MobileSecurityAndMonitoringManager.securityAndFinancialAuditLogFlow

    fun acknowledgeAdminSecurityAlert(
        alertId: String,
        adminEmail: String,
        adminRole: UserRole = UserRole.ADMIN
    ): Boolean {
        return com.example.util.MobileSecurityAndMonitoringManager.acknowledgeSecurityAlert(
            alertId = alertId,
            adminEmail = adminEmail,
            adminRole = adminRole
        )
    }

    fun recordAdminAction(record: AdminActionAuditRecord) {
        _sharedAdminAuditLogs.update { listOf(record) + it }
        if (record.actionType == AdminActionType.PRIVILEGE_MODIFICATION_ATTEMPT_BLOCKED) {
            com.example.util.MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = com.example.util.SecurityMonitoringCategory.UNEXPECTED_ADMIN_ACTION,
                eventType = record.actionType.name,
                actorIdentifier = record.adminEmail,
                targetResourceId = record.targetEntityId,
                details = record.description
            )
        } else {
            com.example.util.MobileSecurityAndMonitoringManager.recordUnifiedAuditEntry(
                category = com.example.util.SecurityMonitoringCategory.UNEXPECTED_ADMIN_ACTION,
                severity = com.example.util.SecurityAlertSeverity.INFO,
                eventType = record.actionType.name,
                actorIdentifier = record.adminEmail,
                targetResourceId = record.targetEntityId,
                details = record.description
            )
        }
    }

    /**
     * Recalculates order totals on the backend from authoritative catalog prices and store commission settings.
     * Never trusts frontend-supplied unit prices or totals.
     */
    fun recalculateOrderTotalOnBackend(
        requestedItems: List<ClientOrderItemClaim>,
        frontendClaimedTotalNgn: Long? = null
    ): BackendRecalculatedOrderSummary {
        return RateLimitingAndFinancialSecurityManager.recalculateAndVerifyOrderFinancialsOnBackend(
            requestedItems = requestedItems,
            catalogProducts = _sharedProducts.value,
            stores = _sharedStores.value,
            defaultCommissionPercent = _sharedSettings.value.defaultCommissionPercent,
            frontendClaimedTotalNgn = frontendClaimedTotalNgn
        )
    }

    /**
     * Validates, scans for malware/polyglots, renames with cryptographic token, and stores an uploaded file
     * into either the Public Catalog Media partition or the AES-256-GCM encrypted Private Admin Verification Vault.
     */
    fun uploadFileToSecureStorage(
        originalFileName: String,
        declaredMimeType: String,
        fileBytes: ByteArray,
        category: UploadFileCategory,
        ownerUid: String,
        ownerStoreId: String,
        uploaderEmail: String = ""
    ): Result<StoredFileRecord> {
        return FileUploadAndStorageSecurityManager.storeValidatedUpload(
            originalFileName = originalFileName,
            declaredMimeType = declaredMimeType,
            fileBytes = fileBytes,
            category = category,
            ownerUid = ownerUid,
            ownerStoreId = ownerStoreId,
            uploaderEmail = uploaderEmail
        )
    }

    /**
     * Restricted Administrator-Only Access:
     * Retrieves and decrypts a private verification document (seller identity document, business premises photo,
     * or compliance certificate) ONLY for verified Platform Administrators.
     */
    fun accessPrivateVerificationDocumentForAdmin(
        fileId: String,
        adminEmail: String,
        adminRole: UserRole = UserRole.ADMIN
    ): Pair<StoredFileRecord, ByteArray> {
        return FileUploadAndStorageSecurityManager.accessPrivateVerificationDocumentForAdmin(
            fileId = fileId,
            adminEmail = adminEmail,
            adminRole = adminRole
        )
    }

    /**
     * Returns the AES-256-GCM encrypted at-rest database record for a SellerStore,
     * including HMAC-SHA256 ledger integrity signature over balances and commission rate.
     */
    fun getEncryptedStoreRecordAtRest(storeId: String): EncryptedSellerStoreRecord? {
        val store = _sharedStores.value.find { it.id == storeId } ?: return null
        return DatabaseEncryptionAndSecurityManager.encryptSellerStoreForDatabase(store)
    }

    /**
     * Returns the AES-256-GCM encrypted at-rest database record for a customer DeliveryAddress.
     */
    fun getEncryptedAddressRecordAtRest(addressId: String): EncryptedDeliveryAddressRecord? {
        val addr = _sharedAddresses.value.find { it.id == addressId } ?: return null
        return DatabaseEncryptionAndSecurityManager.encryptDeliveryAddressForDatabase(addr)
    }

    /**
     * Creates an AES-256-GCM encrypted and HMAC-SHA256 signed backup snapshot of database state.
     * Restricted strictly to verified Platform Administrators.
     */
    fun createEncryptedDatabaseBackup(adminEmail: String = "admin@solarfix.africa"): EncryptedBackupArchive {
        return DatabaseEncryptionAndSecurityManager.createEncryptedBackupSnapshot(
            stores = _sharedStores.value,
            orders = _sharedOrders.value,
            addresses = _sharedAddresses.value,
            adminEmail = adminEmail
        )
    }

    /**
     * Verifies HMAC-SHA256 integrity and decrypts an encrypted database backup snapshot.
     */
    fun verifyAndRestoreDatabaseBackup(
        archive: EncryptedBackupArchive,
        adminEmail: String = "admin@solarfix.africa"
    ): String {
        return DatabaseEncryptionAndSecurityManager.verifyAndDecryptBackupSnapshot(
            archive = archive,
            adminEmail = adminEmail
        )
    }

    /**
     * RBAC Enforcement: Verifies platform administrative clearance.
     * Logs unauthorized attempts to the immutable audit trail and throws SecurityException.
     */
    fun verifyAdminClearanceOrThrow(
        adminEmail: String,
        actionType: AdminActionType,
        targetEntityId: String,
        description: String
    ) {
        if (!FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(adminEmail)) {
            com.example.util.MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = com.example.util.SecurityMonitoringCategory.UNAUTHORIZED_ACCESS_ATTEMPT,
                eventType = "UNAUTHORIZED_ADMIN_OPERATION_${actionType.name}",
                actorIdentifier = adminEmail,
                targetResourceId = targetEntityId,
                details = "Blocked non-admin $adminEmail from executing ${actionType.name}: $description"
            )
            recordAdminAction(
                AdminActionAuditRecord(
                    adminEmail = adminEmail,
                    actionType = AdminActionType.PRIVILEGE_MODIFICATION_ATTEMPT_BLOCKED,
                    targetEntityId = targetEntityId,
                    description = "UNAUTHORIZED ADMINISTRATIVE ATTEMPT: $description by non-admin $adminEmail",
                    stepUpMfaVerified = false
                )
            )
            throw SecurityException("Administrative privilege required. Action blocked for $adminEmail.")
        }
    }

    // --- Role-Based Access Control: Customer Data Isolation ---

    fun getOrdersForCustomer(customerEmail: String, customerUid: String? = null): List<MarketplaceOrder> {
        val cleanEmail = customerEmail.trim().lowercase()
        return _sharedOrders.value.filter { ord ->
            ord.customerEmail.trim().lowercase() == cleanEmail ||
                (customerUid != null && customerUid.isNotBlank() && ord.customerUid == customerUid)
        }
    }

    fun getAddressesForCustomer(userUid: String, userEmail: String? = null): List<DeliveryAddress> {
        val cleanEmail = userEmail?.trim()?.lowercase()
        return _sharedAddresses.value.filter { addr ->
            (addr.userUid.isNotBlank() && addr.userUid == userUid) ||
                (cleanEmail != null && cleanEmail.isNotBlank() && addr.userEmail.trim().lowercase() == cleanEmail) ||
                (addr.userUid.isBlank() && addr.userEmail.isBlank())
        }
    }

    // --- Role-Based Access Control: Seller Data Isolation ---

    fun getProductsForSeller(
        storeId: String,
        callerUid: String? = null,
        callerEmail: String? = null,
        isAdmin: Boolean = false
    ): List<MarketplaceProduct> {
        if (callerUid != null && !isAdmin) {
            val store = _sharedStores.value.find { it.id == storeId }
            if (store != null && store.ownerUid.isNotBlank() && store.ownerUid != callerUid &&
                (callerEmail == null || !store.ownerEmail.equals(callerEmail, ignoreCase = true))) {
                return emptyList()
            }
        }
        return _sharedProducts.value.filter { it.storeId == storeId }
    }

    fun getOrdersForSeller(
        storeId: String,
        callerUid: String? = null,
        callerEmail: String? = null,
        isAdmin: Boolean = false
    ): List<MarketplaceOrder> {
        if (callerUid != null && !isAdmin) {
            val store = _sharedStores.value.find { it.id == storeId }
            if (store != null && store.ownerUid.isNotBlank() && store.ownerUid != callerUid &&
                (callerEmail == null || !store.ownerEmail.equals(callerEmail, ignoreCase = true))) {
                return emptyList()
            }
        }
        return _sharedOrders.value.filter { ord ->
            ord.primaryStoreId == storeId || ord.sellerFulfilments.any { it.storeId == storeId }
        }
    }

    /**
     * Retrieves full SellerStore for the authorized store owner or verified Administrator.
     * Raw private verification document bitmaps (idDocumentBitmap, premisesPhotoBitmap) are
     * restricted exclusively to verified Administrators so private KYC files cannot be exfiltrated.
     */
    fun getPrivateStoreForSeller(
        storeId: String,
        callerUid: String,
        callerEmail: String,
        isAdmin: Boolean = false
    ): SellerStore? {
        val store = _sharedStores.value.find { it.id == storeId } ?: return null
        val isVerifiedAdmin = isAdmin || FirebaseAuthAdminHandler.isOwnerOrSystemAdminEmail(callerEmail)
        val isOwner = (store.ownerUid.isNotBlank() && store.ownerUid == callerUid) ||
            (store.ownerEmail.isNotBlank() && store.ownerEmail.equals(callerEmail, ignoreCase = true))
        if (!isVerifiedAdmin && !isOwner) return null
        return if (isVerifiedAdmin) {
            store
        } else {
            // Non-admin store owners see their store & banking details, while raw KYC document scans remain Admin-Only
            store.copy(
                idDocumentBitmap = null,
                premisesPhotoBitmap = null
            )
        }
    }

    /**
     * Public storefront representation for customers and general browsing.
     * Guaranteed to strip all sensitive bank account numbers, tax IDs, and KYC documents.
     */
    fun getPublicStoreProfile(storeId: String): PublicStoreProfile? {
        return _sharedStores.value.find { it.id == storeId }?.toPublicProfile()
    }

    fun getPublicStoreProfiles(): List<PublicStoreProfile> {
        return _sharedStores.value
            .filter { it.status == StoreVerificationStatus.APPROVED }
            .map { it.toPublicProfile() }
    }

    val cartItems: StateFlow<List<CartItem>> = _sharedCartItems.asStateFlow()
    val prePurchaseInquiriesFlow: StateFlow<List<PrePurchaseVerificationInquiry>> = _sharedPrePurchaseInquiries.asStateFlow()
    val paymentGateway: PaystackPaymentGatewayManager = _sharedPaymentGateway
    val gatewayTransactionsFlow: StateFlow<List<PaymentVerificationResult>> = _sharedPaymentGateway.gatewayTransactions
    val reconciliationRecordsFlow: StateFlow<List<PaymentReconciliationRecord>> = _sharedPaymentGateway.reconciliationRecords
    val refundRecordsFlow: StateFlow<List<GatewayRefundRecord>> = _sharedPaymentGateway.refundRecords
    val sellerSubaccountsFlow: StateFlow<Map<String, PaystackSubaccount>> = _sharedPaymentGateway.sellerSubaccounts
    val selectedCurrency: StateFlow<Currency> = _sharedSelectedCurrency.asStateFlow()
    val maintenanceTasks: StateFlow<List<MaintenanceTask>> = _sharedMaintenanceTasks.asStateFlow()
    val userProfile: StateFlow<SolarSystemProfile> = _sharedUserProfile.asStateFlow()

    fun toggleMaintenanceTask(taskId: String) {
        _sharedMaintenanceTasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    val newCompleted = !task.isCompleted
                    task.copy(
                        isCompleted = newCompleted,
                        lastDoneDate = if (newCompleted) "Today" else null
                    )
                } else task
            }
        }
    }

    fun addToCart(product: MarketplaceProduct, paymentPlan: PaymentPlanType = PaymentPlanType.OUTRIGHT) {
        _sharedCartItems.update { current ->
            val existing = current.find { it.product.id == product.id }
            if (existing != null) {
                current.map {
                    if (it.product.id == product.id) {
                        it.copy(
                            quantity = (it.quantity + 1).coerceAtMost(product.stockCount.coerceAtLeast(1)),
                            selectedPaymentPlan = paymentPlan
                        )
                    } else it
                }
            } else {
                current + CartItem(product = product, quantity = 1, selectedPaymentPlan = paymentPlan)
            }
        }
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        _sharedCartItems.update { current ->
            current.mapNotNull { item ->
                if (item.product.id == productId) {
                    val maxStock = item.product.stockCount.coerceAtLeast(1)
                    val newQty = (item.quantity + delta).coerceAtMost(maxStock)
                    if (newQty > 0) item.copy(quantity = newQty) else null
                } else item
            }
        }
    }

    fun setCartItemQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        _sharedCartItems.update { current ->
            current.map { item ->
                if (item.product.id == productId) {
                    val maxStock = item.product.stockCount.coerceAtLeast(1)
                    item.copy(quantity = quantity.coerceIn(1, maxStock))
                } else item
            }
        }
    }

    fun removeFromCart(productId: String) {
        _sharedCartItems.update { current ->
            current.filterNot { it.product.id == productId }
        }
    }

    fun updateCartPaymentPlan(productId: String, plan: PaymentPlanType) {
        _sharedCartItems.update { current ->
            current.map { item ->
                if (item.product.id == productId) item.copy(selectedPaymentPlan = plan) else item
            }
        }
    }

    fun clearCart() {
        _sharedCartItems.value = emptyList()
    }

    /**
     * Validates that all items in the cart are currently available (in stock, quantity <= stockCount)
     * and that their unit prices have not changed in the catalog since they were added.
     * Returns a list of detailed validation error messages, or an empty list if valid.
     */
    fun validateCartBeforeCheckout(): List<String> {
        val currentCart = _sharedCartItems.value
        if (currentCart.isEmpty()) return listOf("Your shopping cart is empty.")
        val liveProducts = _sharedProducts.value
        val issues = mutableListOf<String>()
        currentCart.forEach { item ->
            val live = liveProducts.find { it.id == item.product.id }
            if (live == null || !live.isActive) {
                issues.add("Item \"${item.product.title}\" is no longer available in the marketplace.")
            } else if (!live.inStock || live.stockCount <= 0) {
                issues.add("Item \"${live.title}\" is currently OUT OF STOCK.")
            } else if (live.stockCount < item.quantity) {
                issues.add("Only ${live.stockCount} unit(s) remaining for \"${live.title}\" (requested: ${item.quantity}).")
            } else if (live.basePriceNgn != item.product.basePriceNgn) {
                issues.add("Price update for \"${live.title}\": listed as ₦${item.product.basePriceNgn}, latest catalog price is ₦${live.basePriceNgn}.")
            }
        }
        return issues
    }

    /**
     * Synchronizes the cart with the latest catalog stock and pricing.
     * Updates prices, caps quantities to available stock, and removes discontinued/out-of-stock items.
     * Returns a list of user notices describing any changes made.
     */
    fun refreshCartWithCurrentCatalog(): List<String> {
        val liveProducts = _sharedProducts.value
        val notices = mutableListOf<String>()
        _sharedCartItems.update { current ->
            current.mapNotNull { item ->
                val live = liveProducts.find { it.id == item.product.id }
                if (live == null || !live.isActive || !live.inStock || live.stockCount <= 0) {
                    notices.add("Removed unavailable item: \"${item.product.title}\"")
                    null
                } else {
                    var updated = item
                    if (item.quantity > live.stockCount) {
                        notices.add("Adjusted quantity of \"${live.title}\" to available stock (${live.stockCount})")
                        updated = updated.copy(quantity = live.stockCount)
                    }
                    if (item.product.basePriceNgn != live.basePriceNgn) {
                        notices.add("Updated price of \"${live.title}\" to ₦${live.basePriceNgn}")
                        updated = updated.copy(product = live)
                    } else if (item.product != live) {
                        updated = updated.copy(product = live)
                    }
                    updated
                }
            }
        }
        return notices
    }

    /**
     * Submits a pre-payment verification request for expensive solar hardware
     * (e.g. live serial plate photo, OEM flash test curve, direct seller phone confirmation).
     */
    fun submitPrePurchaseInquiry(
        productId: String,
        requestType: String,
        notes: String,
        contact: String
    ): PrePurchaseVerificationInquiry {
        val product = _sharedProducts.value.find { it.id == productId }
        val cleanContact = SecuritySanitizer.sanitizeText(contact, maxLength = 60).ifEmpty { "+234 803 700 1234" }
        val inquiry = PrePurchaseVerificationInquiry(
            id = "INQ-${SecuritySanitizer.nextSecureInt(10000, 99999)}",
            productId = productId,
            productTitle = product?.title ?: "Solar Equipment",
            storeId = product?.storeId ?: "store_general",
            storeName = product?.sellerName ?: "Verified SolarFix Seller",
            requestType = SecuritySanitizer.sanitizeText(requestType, maxLength = 150),
            customerNotes = SecuritySanitizer.sanitizeText(notes, maxLength = 1000),
            customerContact = cleanContact,
            timestamp = "Just now",
            status = "PENDING_SELLER_CONFIRMATION"
        )
        _sharedPrePurchaseInquiries.update { listOf(inquiry) + it }
        return inquiry
    }

    /**
     * Initializes payment with Paystack Nigerian Gateway.
     * Generates dedicated virtual account details and unique transaction reference.
     */
    fun initializeGatewayPayment(
        customerEmail: String,
        amountNgn: Long,
        storesInCart: List<SellerStore> = emptyList(),
        customRef: String? = null
    ): PaymentInitializationResult {
        return _sharedPaymentGateway.initializePayment(customerEmail, amountNgn, storesInCart, customRef)
    }

    /**
     * Verifies payment on backend server using Paystack Gateway API.
     * Prevents duplicate payments and validates exact amount matching.
     */
    fun verifyServerPayment(
        reference: String,
        expectedOrderTotalNgn: Long,
        customerEmail: String,
        customerName: String,
        channel: String = "card"
    ): PaymentVerificationResult {
        return _sharedPaymentGateway.verifyPaymentOnServer(
            reference = reference,
            expectedOrderTotalNgn = expectedOrderTotalNgn,
            customerEmail = customerEmail,
            customerName = customerName,
            channel = channel
        )
    }

    /**
     * Processes incoming payment webhooks with cryptographic HMAC-SHA512 verification.
     */
    fun processPaymentWebhook(
        rawPayload: String,
        signatureHeader: String,
        event: PaymentWebhookEvent
    ): Boolean {
        return _sharedPaymentGateway.processWebhookEvent(rawPayload, signatureHeader, event)
    }

    /**
     * Processes customer refunds via Paystack gateway.
     */
    fun processOrderRefund(
        orderId: String,
        reason: String
    ): GatewayRefundRecord? {
        val order = _sharedOrders.value.find { it.id == orderId } ?: return null
        if (order.status == OrderStatus.REFUNDED) return null
        val uniqueRefundRegistered = RateLimitingAndFinancialSecurityManager.tryRegisterUniqueOrderRefund(
            orderId = order.id,
            storeId = order.primaryStoreId,
            refundAmountNgn = order.totalAmountNgn
        )
        if (!uniqueRefundRegistered) return null

        val refundRecord = _sharedPaymentGateway.processRefund(
            originalReference = order.paymentReference ?: order.id,
            orderId = order.id,
            amountNgn = order.totalAmountNgn,
            reason = reason,
            storeId = order.primaryStoreId,
            storeName = order.primaryStoreName,
            customerEmail = order.customerEmail
        )
        // Mark order refunded
        _sharedOrders.update { list ->
            list.map { ord ->
                if (ord.id == orderId) {
                    ord.copy(
                        status = OrderStatus.REFUNDED,
                        statusTimeline = ord.statusTimeline + "Refund of ₦${order.totalAmountNgn} authorized via Paystack API: $reason"
                    )
                } else ord
            }
        }
        return refundRecord
    }

    /**
     * Generates payment reconciliation report for marketplace audits.
     */
    fun generatePaymentReconciliation(dateLabel: String = "Daily Marketplace Reconciliation"): PaymentReconciliationRecord {
        return _sharedPaymentGateway.generateReconciliationReport(dateLabel)
    }

    /**
     * Computes a complete multi-seller cart breakdown with per-seller subtotals,
     * delivery cost calculation per seller fulfilment leg, SolarFix commission visibility,
     * net seller payouts, and the final order total.
     */
    fun calculateCartBreakdown(): CartSummaryBreakdown {
        val currentCart = _sharedCartItems.value
        if (currentCart.isEmpty()) {
            return CartSummaryBreakdown(
                sellerBreakdowns = emptyList(),
                totalItemsCount = 0,
                totalSellersCount = 0,
                subtotalNgn = 0L,
                totalDeliveryCostNgn = 0L,
                totalSolarfixCommissionNgn = 0L,
                totalNetSellerPayoutNgn = 0L,
                finalOrderTotalNgn = 0L
            )
        }

        val settings = _sharedSettings.value
        val groupedByStore = currentCart.groupBy { it.product.storeId }

        val sellerBreakdowns = groupedByStore.map { (storeId, sellerItems) ->
            val store = _sharedStores.value.find { it.id == storeId }
            val firstProd = sellerItems.first().product
            val storeName = store?.storeName ?: firstProd.sellerName
            val storeLocation = store?.location ?: firstProd.sellerLocation
            val isVerified = if (store != null) store.status == StoreVerificationStatus.APPROVED else firstProd.isCertifiedSeller

            val liveCatalog = _sharedProducts.value
            val sellerSubtotal = sellerItems.sumOf { item ->
                val authoritativePrice = liveCatalog.find { p -> p.id == item.product.id }?.basePriceNgn ?: item.product.basePriceNgn
                authoritativePrice * item.quantity
            }
            val sellerDeliveryFee = 15000L // Insured SolarFix logistics per seller store shipment leg
            val gatewayFee = (sellerSubtotal * 0.015).roundToLong().coerceAtMost(2000L) // Paystack 1.5% gateway charge capped at ₦2000
            val commissionPct = store?.customCommissionPercent ?: settings.defaultCommissionPercent
            val commissionNgn = (sellerSubtotal * (commissionPct / 100.0)).roundToLong()
            val netSellerNgn = (sellerSubtotal - commissionNgn).coerceAtLeast(0L)
            val sellerTotal = sellerSubtotal + sellerDeliveryFee

            SellerCartBreakdown(
                storeId = storeId,
                storeName = storeName,
                storeLocation = storeLocation,
                isVerifiedSeller = isVerified,
                items = sellerItems,
                sellerSubtotalNgn = sellerSubtotal,
                sellerDeliveryFeeNgn = sellerDeliveryFee,
                paymentGatewayFeeNgn = gatewayFee,
                commissionRatePercent = commissionPct,
                solarfixCommissionNgn = commissionNgn,
                netSellerPayoutNgn = netSellerNgn,
                sellerOrderTotalNgn = sellerTotal
            )
        }

        val totalItems = currentCart.sumOf { it.quantity }
        val subtotal = sellerBreakdowns.sumOf { it.sellerSubtotalNgn }
        val totalDelivery = sellerBreakdowns.sumOf { it.sellerDeliveryFeeNgn }
        val totalGatewayFee = sellerBreakdowns.sumOf { it.paymentGatewayFeeNgn }
        val totalCommission = sellerBreakdowns.sumOf { it.solarfixCommissionNgn }
        val totalNetSeller = sellerBreakdowns.sumOf { it.netSellerPayoutNgn }
        val finalOrderTotal = subtotal + totalDelivery

        return CartSummaryBreakdown(
            sellerBreakdowns = sellerBreakdowns,
            totalItemsCount = totalItems,
            totalSellersCount = sellerBreakdowns.size,
            subtotalNgn = subtotal,
            totalDeliveryCostNgn = totalDelivery,
            totalPaymentGatewayFeeNgn = totalGatewayFee,
            totalSolarfixCommissionNgn = totalCommission,
            totalNetSellerPayoutNgn = totalNetSeller,
            finalOrderTotalNgn = finalOrderTotal
        )
    }

    fun selectCurrency(currency: Currency) {
        _sharedSelectedCurrency.value = currency
    }

    fun updateProfile(newProfile: SolarSystemProfile) {
        val safeKva = if (newProfile.systemSizeKva.isNaN() || newProfile.systemSizeKva.isInfinite()) 5.0 else newProfile.systemSizeKva.coerceIn(0.1, 5000.0)
        val safeKwh = if (newProfile.batteryCapacityKwh.isNaN() || newProfile.batteryCapacityKwh.isInfinite()) 10.0 else newProfile.batteryCapacityKwh.coerceIn(0.1, 20000.0)
        _sharedUserProfile.value = newProfile.copy(
            systemSizeKva = safeKva,
            batteryType = SecuritySanitizer.sanitizeText(newProfile.batteryType, maxLength = 80, preserveNewlines = false),
            batteryCapacityKwh = safeKwh,
            solarPanelWatts = newProfile.solarPanelWatts.coerceIn(10, 5_000_000),
            installerName = SecuritySanitizer.sanitizeText(newProfile.installerName, maxLength = SecuritySanitizer.MAX_NAME_LENGTH, preserveNewlines = false)
        )
    }

    fun getCurrentEnergyMetrics(): EnergyMetrics {
        return EnergyMetrics(
            solarWattsNow = 2850,
            batterySocPercent = 84,
            batteryVoltage = 52.8,
            loadWattsNow = 1120,
            isGridAvailable = false,
            dailyPvKwh = 14.6,
            generatorFuelSavedLitres = 4.2,
            estimatedHoursLeftAtCurrentLoad = 7.5,
            hourlyHistory = listOf(
                HourlyYield("7 AM", 200f, 650f),
                HourlyYield("9 AM", 1100f, 850f),
                HourlyYield("11 AM", 2400f, 980f),
                HourlyYield("1 PM", 3100f, 1350f),
                HourlyYield("3 PM", 2650f, 1120f),
                HourlyYield("5 PM", 950f, 920f)
            )
        )
    }

    // --- Multi-Vendor Marketplace Operations ---

    fun addOrUpdateProduct(
        product: MarketplaceProduct,
        callerStoreId: String? = null,
        isAdmin: Boolean = false
    ) {
        if (callerStoreId != null && !isAdmin) {
            if (product.storeId != callerStoreId) {
                throw SecurityException("Unauthorized: Seller can only add or update products for their own store ($callerStoreId)")
            }
            val existing = _sharedProducts.value.find { it.id == product.id }
            if (existing != null && existing.storeId != callerStoreId) {
                throw SecurityException("Unauthorized: Cannot modify a product belonging to another store")
            }
        }
        val safeBitmap = product.photoBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid }
        val safeBitmapList = product.photoBitmaps.filter { SecuritySanitizer.validateUploadedBitmap(it).isValid }
        val safeVideoUri = FileUploadAndStorageSecurityManager.validateAndSanitizeProductVideoReference(product.videoUrlOrUri)

        // Register public product photographs in the isolated PUBLIC_CATALOG_MEDIA storage partition
        safeBitmap?.let {
            FileUploadAndStorageSecurityManager.registerBitmapInSecureStorage(
                bitmap = it,
                category = UploadFileCategory.PRODUCT_PHOTOGRAPH,
                ownerUid = product.storeId,
                ownerStoreId = product.storeId
            )
        }
        safeBitmapList.forEach { bmp ->
            FileUploadAndStorageSecurityManager.registerBitmapInSecureStorage(
                bitmap = bmp,
                category = UploadFileCategory.PRODUCT_PHOTOGRAPH,
                ownerUid = product.storeId,
                ownerStoreId = product.storeId
            )
        }

        val sanitizedProduct = product.copy(
            title = SecuritySanitizer.sanitizeText(product.title, maxLength = SecuritySanitizer.MAX_PRODUCT_TITLE_LENGTH, preserveNewlines = false),
            brand = SecuritySanitizer.sanitizeText(product.brand, maxLength = 100, preserveNewlines = false),
            modelNumber = SecuritySanitizer.sanitizeText(product.modelNumber, maxLength = 100, preserveNewlines = false),
            powerRating = SecuritySanitizer.sanitizeText(product.powerRating, maxLength = 80, preserveNewlines = false),
            voltage = SecuritySanitizer.sanitizeText(product.voltage, maxLength = 80, preserveNewlines = false),
            description = SecuritySanitizer.sanitizeRichDescription(product.description, maxLength = SecuritySanitizer.MAX_PRODUCT_DESCRIPTION_LENGTH),
            sellerName = SecuritySanitizer.sanitizeText(product.sellerName, maxLength = SecuritySanitizer.MAX_NAME_LENGTH, preserveNewlines = false),
            sellerLocation = SecuritySanitizer.sanitizeText(product.sellerLocation, maxLength = 120, preserveNewlines = false),
            condition = SecuritySanitizer.sanitizeText(product.condition, maxLength = 80, preserveNewlines = false),
            warrantyPeriod = SecuritySanitizer.sanitizeText(product.warrantyPeriod, maxLength = 120, preserveNewlines = false),
            returnPolicy = SecuritySanitizer.sanitizeText(product.returnPolicy, maxLength = 300, preserveNewlines = false),
            deliveryAvailability = SecuritySanitizer.sanitizeText(product.deliveryAvailability, maxLength = 200, preserveNewlines = false),
            authenticityEvidenceNote = SecuritySanitizer.sanitizeText(product.authenticityEvidenceNote, maxLength = 500),
            adminModerationNote = SecuritySanitizer.sanitizeText(product.adminModerationNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH),
            basePriceNgn = product.basePriceNgn.coerceIn(1L, 500_000_000L),
            stockCount = product.stockCount.coerceIn(0, 100_000),
            specs = product.specs.entries.associate { (k, v) ->
                SecuritySanitizer.sanitizeText(k, maxLength = 80, preserveNewlines = false) to
                    SecuritySanitizer.sanitizeText(v, maxLength = 200, preserveNewlines = false)
            },
            photoBitmap = safeBitmap,
            photoBitmaps = safeBitmapList,
            videoUrlOrUri = safeVideoUri,
            videoDescription = SecuritySanitizer.sanitizeText(product.videoDescription, maxLength = 200, preserveNewlines = false)
        )
        _sharedProducts.update { list ->
            val exists = list.any { it.id == sanitizedProduct.id }
            if (exists) {
                list.map { if (it.id == sanitizedProduct.id) sanitizedProduct else it }
            } else {
                listOf(sanitizedProduct) + list
            }
        }
    }

    fun updateProductPriceAndStock(
        productId: String,
        newPriceNgn: Long,
        newStockCount: Int,
        callerStoreId: String? = null,
        isAdmin: Boolean = false
    ): Boolean {
        val target = _sharedProducts.value.find { it.id == productId } ?: return false
        if (callerStoreId != null && !isAdmin && target.storeId != callerStoreId) {
            com.example.util.MobileSecurityAndMonitoringManager.recordSuspiciousActivity(
                category = com.example.util.SecurityMonitoringCategory.UNAUTHORIZED_ACCESS_ATTEMPT,
                eventType = "CROSS_STORE_PRODUCT_MODIFICATION_BLOCKED",
                actorIdentifier = callerStoreId,
                targetResourceId = "${target.storeId}:$productId",
                details = "Store $callerStoreId attempted to modify price/stock of product $productId belonging to ${target.storeId}"
            )
            return false
        }
        val safePrice = newPriceNgn.coerceIn(1L, 500_000_000L)
        val safeStock = newStockCount.coerceIn(0, 100_000)

        // Monitor for unusual seller price swings or abnormal inventory spikes
        val storeOwnerEmail = _sharedStores.value.find { it.id == target.storeId }?.ownerEmail ?: "seller@solarfix.africa"
        com.example.util.MobileSecurityAndMonitoringManager.monitorSellerProductUpdateForAnomalies(
            storeId = target.storeId,
            productId = productId,
            previousPriceNgn = target.basePriceNgn,
            newPriceNgn = safePrice,
            previousStock = target.stockCount,
            newStock = safeStock,
            sellerEmail = storeOwnerEmail
        )

        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        basePriceNgn = safePrice,
                        stockCount = safeStock,
                        inStock = safeStock > 0
                    )
                } else prod
            }
        }
        return true
    }

    fun deleteProductListing(
        productId: String,
        callerStoreId: String? = null,
        isAdmin: Boolean = false
    ): Boolean {
        val target = _sharedProducts.value.find { it.id == productId } ?: return false
        if (callerStoreId != null && !isAdmin && target.storeId != callerStoreId) {
            return false
        }
        _sharedProducts.update { list -> list.filterNot { it.id == productId } }
        return true
    }

    fun toggleProductActiveStatus(productId: String): Boolean {
        var newActiveState = true
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    newActiveState = !prod.isActive
                    prod.copy(isActive = newActiveState)
                } else prod
            }
        }
        return newActiveState
    }

    fun setProductModerationStatus(
        productId: String,
        status: ProductModerationStatus,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.PRODUCT_MODERATION_STATUS_CHANGE,
            targetEntityId = productId,
            description = "Update product moderation status to ${status.name}"
        )
        val cleanNote = adminNote?.let { SecuritySanitizer.sanitizeText(it, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH) }
        val prevStatus = _sharedProducts.value.find { it.id == productId }?.moderationStatus?.name ?: "UNKNOWN"
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        moderationStatus = status,
                        adminModerationNote = cleanNote?.takeIf { it.isNotBlank() } ?: prod.adminModerationNote
                    )
                } else prod
            }
        }
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.PRODUCT_MODERATION_STATUS_CHANGE,
                targetEntityId = productId,
                description = "Set product moderation status to ${status.name}. Note: ${cleanNote ?: "None"}",
                previousValue = prevStatus,
                newValue = status.name,
                stepUpMfaVerified = true
            )
        )
    }

    /**
     * Marks a product listing as Suspicious / High-Risk and hides it from customer catalog pending investigation.
     */
    fun flagSuspiciousProduct(productId: String, adminNote: String) {
        val cleanNote = SecuritySanitizer.sanitizeText(adminNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH)
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        moderationStatus = ProductModerationStatus.SUSPICIOUS_HIGH_RISK,
                        adminModerationNote = cleanNote.ifEmpty {
                            "Flagged as suspicious/high-risk listing for anti-counterfeit review."
                        }
                    )
                } else prod
            }
        }
    }

    /**
     * Requests additional authenticity evidence (e.g., OEM invoice, SONCAP certificate, or serial plate photo) from the seller.
     */
    fun requestProductAuthenticityEvidence(productId: String, adminNote: String) {
        val cleanNote = SecuritySanitizer.sanitizeText(adminNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH)
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        moderationStatus = ProductModerationStatus.EVIDENCE_REQUESTED,
                        adminModerationNote = cleanNote.ifEmpty {
                            "Please provide OEM factory serial plate photo and authorized distributor invoice."
                        }
                    )
                } else prod
            }
        }
    }

    /**
     * Allows a seller to submit requested authenticity evidence for a flagged/evidence-requested listing.
     */
    fun submitSellerProductAuthenticityEvidence(productId: String, evidenceNote: String) {
        val cleanEvidence = SecuritySanitizer.sanitizeText(evidenceNote, maxLength = 500)
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        moderationStatus = ProductModerationStatus.PENDING_REVIEW,
                        sellerAuthenticityConfirmed = true,
                        authenticityEvidenceNote = cleanEvidence.ifEmpty {
                            "Uploaded OEM serial batch certificate & distributor invoice."
                        },
                        adminModerationNote = "Seller submitted additional authenticity evidence for Admin review."
                    )
                } else prod
            }
        }
    }

    /**
     * Removes a counterfeit or misleading product listing from the marketplace.
     */
    fun removeCounterfeitProduct(
        productId: String,
        adminReason: String,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.COUNTERFEIT_PRODUCT_REMOVED,
            targetEntityId = productId,
            description = "Remove counterfeit or misleading product listing"
        )
        val cleanReason = SecuritySanitizer.sanitizeText(adminReason, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH)
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(
                        moderationStatus = ProductModerationStatus.REMOVED_COUNTERFEIT,
                        isActive = false,
                        inStock = false,
                        stockCount = 0,
                        solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_FAILED,
                        solarfixInspectionReportId = null,
                        adminModerationNote = cleanReason.ifEmpty {
                            "Removed by Administrator: Counterfeit or misleading technical specifications."
                        }
                    )
                } else prod
            }
        }
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.COUNTERFEIT_PRODUCT_REMOVED,
                targetEntityId = productId,
                description = "Removed counterfeit product $productId: $cleanReason",
                previousValue = "ACTIVE",
                newValue = ProductModerationStatus.REMOVED_COUNTERFEIT.name,
                stepUpMfaVerified = true
            )
        )
    }

    /**
     * Records an actual independent physical/lab inspection by SolarFix engineers.
     * A SolarFix inspection badge is ONLY displayed when passed == true and a non-blank reportId is recorded.
     */
    fun recordSolarFixProductInspection(
        productId: String,
        passed: Boolean,
        reportId: String,
        inspectionNote: String = "Physical load-bank and serial verification completed by SolarFix Lab.",
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.PHYSICAL_INSPECTION_UPDATE,
            targetEntityId = productId,
            description = "Record official lab inspection: passed=$passed, reportId=$reportId"
        )
        val cleanReportId = SecuritySanitizer.sanitizeText(reportId, maxLength = 60, preserveNewlines = false)
        val cleanNote = SecuritySanitizer.sanitizeText(inspectionNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH)
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    if (passed && cleanReportId.isNotBlank()) {
                        prod.copy(
                            moderationStatus = ProductModerationStatus.APPROVED,
                            solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                            solarfixInspectionReportId = cleanReportId,
                            solarfixInspectionDate = "Inspected Today",
                            adminModerationNote = cleanNote
                        )
                    } else {
                        prod.copy(
                            moderationStatus = ProductModerationStatus.REMOVED_COUNTERFEIT,
                            isActive = false,
                            solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_FAILED,
                            solarfixInspectionReportId = null,
                            solarfixInspectionDate = null,
                            adminModerationNote = cleanNote.ifEmpty {
                                "Failed physical SolarFix lab inspection."
                            }
                        )
                    }
                } else prod
            }
        }
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.PHYSICAL_INSPECTION_UPDATE,
                targetEntityId = productId,
                description = "Recorded SolarFix lab inspection: passed=$passed, reportId=$cleanReportId",
                previousValue = "NOT_INSPECTED",
                newValue = if (passed) "INSPECTED_PASSED" else "INSPECTED_FAILED",
                stepUpMfaVerified = true
            )
        )
    }

    fun submitSellerRegistrationApplication(
        businessOrFullName: String,
        cacRegNumber: String,
        businessPhone: String,
        businessEmail: String,
        physicalAddress: String,
        city: String,
        state: String,
        businessDescription: String,
        logoBitmap: android.graphics.Bitmap?,
        idDocumentType: String,
        idDocumentNumber: String,
        idDocumentBitmap: android.graphics.Bitmap?,
        premisesPhotoBitmap: android.graphics.Bitmap?,
        bankAccountName: String,
        bankName: String,
        bankAccountNumber: String
    ): SellerStore {
        val cleanBusinessName = SecuritySanitizer.sanitizeText(businessOrFullName, maxLength = 140)
        val cleanCac = SecuritySanitizer.sanitizeText(cacRegNumber, maxLength = 60).ifEmpty { "Individual Seller (CAC N/A)" }
        val cleanPhone = SecuritySanitizer.sanitizeText(businessPhone, maxLength = 40)
        val cleanEmail = SecuritySanitizer.sanitizeEmail(businessEmail)
        val cleanAddr = SecuritySanitizer.sanitizeText(physicalAddress, maxLength = 250)
        val cleanCity = SecuritySanitizer.sanitizeText(city, maxLength = 80)
        val cleanState = SecuritySanitizer.sanitizeText(state, maxLength = 80)
        val cleanDesc = SecuritySanitizer.sanitizeText(businessDescription, maxLength = 1500)
        val cleanIdType = SecuritySanitizer.sanitizeText(idDocumentType, maxLength = 80)
        val cleanIdNumber = SecuritySanitizer.sanitizeText(idDocumentNumber, maxLength = 60)
        val cleanBankAccName = SecuritySanitizer.sanitizeText(bankAccountName, maxLength = 120)
        val cleanBankName = SecuritySanitizer.sanitizeText(bankName, maxLength = 100)
        val cleanBankAccNum = SecuritySanitizer.sanitizeText(bankAccountNumber, maxLength = 20)

        val safeLogo = logoBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid }
        val safeIdDoc = idDocumentBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid }
        val safePremises = premisesPhotoBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid }

        val newStoreId = "store_app_${UUID.randomUUID().toString().take(6)}"
        val newOwnerUid = "applicant_${UUID.randomUUID().toString().take(6)}"

        // Separate public store logo into PUBLIC_CATALOG_MEDIA and KYC/premises into PRIVATE_ADMIN_VAULT
        safeLogo?.let {
            FileUploadAndStorageSecurityManager.registerBitmapInSecureStorage(
                bitmap = it,
                category = UploadFileCategory.PROFILE_PHOTOGRAPH,
                ownerUid = newOwnerUid,
                ownerStoreId = newStoreId,
                uploaderEmail = cleanEmail
            )
        }
        safeIdDoc?.let {
            FileUploadAndStorageSecurityManager.registerBitmapInSecureStorage(
                bitmap = it,
                category = UploadFileCategory.SELLER_IDENTITY_DOCUMENT,
                ownerUid = newOwnerUid,
                ownerStoreId = newStoreId,
                uploaderEmail = cleanEmail
            )
        }
        safePremises?.let {
            FileUploadAndStorageSecurityManager.registerBitmapInSecureStorage(
                bitmap = it,
                category = UploadFileCategory.BUSINESS_VERIFICATION_PHOTO,
                ownerUid = newOwnerUid,
                ownerStoreId = newStoreId,
                uploaderEmail = cleanEmail
            )
        }

        val newStore = SellerStore(
            id = newStoreId,
            storeName = cleanBusinessName,
            ownerUid = newOwnerUid,
            ownerName = cleanBusinessName,
            ownerEmail = cleanEmail,
            businessRegNumber = cleanCac,
            physicalAddress = cleanAddr,
            city = cleanCity,
            state = cleanState,
            location = "$cleanCity, $cleanState",
            phone = cleanPhone,
            tagline = "Solar Equipment Supplier • $cleanCity, $cleanState",
            description = cleanDesc,
            warrantyPolicy = "Official Manufacturer Warranty + SolarFix 48-Hour Escrow Protection",
            rating = 5.0f,
            reviewsCount = 0,
            status = StoreVerificationStatus.PENDING,
            accreditationBadge = "Pending Verification",
            joinedDate = "Applied Today",
            logoBitmap = safeLogo,
            idDocumentType = cleanIdType,
            idDocumentNumber = cleanIdNumber,
            idDocumentBitmap = safeIdDoc,
            idDocumentProvided = true,
            premisesPhotoBitmap = safePremises,
            premisesPhotoProvided = true,
            bankAccountName = cleanBankAccName,
            bankName = cleanBankName,
            bankAccountNumber = cleanBankAccNum,
            adminVerificationNote = "Application submitted and awaiting SolarFix Administrator KYC/KYB verification."
        )
        _sharedStores.update { listOf(newStore) + it }
        return newStore
    }

    fun createOrUpdateStore(
        store: SellerStore,
        callerUid: String = "",
        callerEmail: String = "",
        isAdmin: Boolean = false
    ) {
        val exists = _sharedStores.value.any { it.id == store.id }
        if (exists && !isAdmin) {
            val existing = _sharedStores.value.first { it.id == store.id }
            if (callerUid.isNotBlank() && existing.ownerUid.isNotBlank() && existing.ownerUid != callerUid &&
                (callerEmail.isBlank() || !existing.ownerEmail.equals(callerEmail, ignoreCase = true))) {
                throw SecurityException("Access Denied: Sellers can only modify their own store.")
            }
        }
        val sanitizedStore = store.copy(
            storeName = SecuritySanitizer.sanitizeText(store.storeName, maxLength = 140, preserveNewlines = false),
            ownerName = SecuritySanitizer.sanitizeText(store.ownerName, maxLength = SecuritySanitizer.MAX_NAME_LENGTH, preserveNewlines = false),
            ownerEmail = SecuritySanitizer.sanitizeEmail(store.ownerEmail),
            businessRegNumber = SecuritySanitizer.sanitizeText(store.businessRegNumber, maxLength = 60, preserveNewlines = false),
            physicalAddress = SecuritySanitizer.sanitizeText(store.physicalAddress, maxLength = SecuritySanitizer.MAX_ADDRESS_STREET_LENGTH),
            city = SecuritySanitizer.sanitizeText(store.city, maxLength = SecuritySanitizer.MAX_CITY_STATE_LENGTH, preserveNewlines = false),
            state = SecuritySanitizer.sanitizeText(store.state, maxLength = SecuritySanitizer.MAX_CITY_STATE_LENGTH, preserveNewlines = false),
            location = SecuritySanitizer.sanitizeText(store.location, maxLength = 160, preserveNewlines = false),
            phone = SecuritySanitizer.sanitizeText(store.phone, maxLength = SecuritySanitizer.MAX_PHONE_LENGTH, preserveNewlines = false),
            tagline = SecuritySanitizer.sanitizeText(store.tagline, maxLength = 160, preserveNewlines = false),
            description = SecuritySanitizer.sanitizeRichDescription(store.description, maxLength = 2000),
            warrantyPolicy = SecuritySanitizer.sanitizeText(store.warrantyPolicy, maxLength = 300),
            idDocumentType = SecuritySanitizer.sanitizeText(store.idDocumentType, maxLength = 80, preserveNewlines = false),
            idDocumentNumber = SecuritySanitizer.sanitizeText(store.idDocumentNumber, maxLength = 60, preserveNewlines = false),
            bankAccountName = SecuritySanitizer.sanitizeText(store.bankAccountName, maxLength = SecuritySanitizer.MAX_NAME_LENGTH, preserveNewlines = false),
            bankName = SecuritySanitizer.sanitizeText(store.bankName, maxLength = 100, preserveNewlines = false),
            bankAccountNumber = SecuritySanitizer.sanitizeText(store.bankAccountNumber, maxLength = 20, preserveNewlines = false),
            adminVerificationNote = SecuritySanitizer.sanitizeText(store.adminVerificationNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH),
            logoBitmap = store.logoBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid },
            coverImageBitmap = store.coverImageBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid },
            idDocumentBitmap = store.idDocumentBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid },
            premisesPhotoBitmap = store.premisesPhotoBitmap?.takeIf { SecuritySanitizer.validateUploadedBitmap(it).isValid }
        )
        _sharedStores.update { list ->
            if (exists) {
                list.map { existing ->
                    if (existing.id == sanitizedStore.id) {
                        if (!isAdmin) {
                            // Strictly preserve existing financial balances and admin fields
                            val resolvedStatus = if (existing.status == StoreVerificationStatus.MORE_INFO_REQUIRED &&
                                sanitizedStore.status == StoreVerificationStatus.PENDING
                            ) {
                                StoreVerificationStatus.PENDING
                            } else {
                                existing.status
                            }
                            sanitizedStore.copy(
                                status = resolvedStatus,
                                customCommissionPercent = existing.customCommissionPercent,
                                availableEarningsNgn = existing.availableEarningsNgn,
                                pendingEscrowNgn = existing.pendingEscrowNgn,
                                totalGrossSalesNgn = existing.totalGrossSalesNgn,
                                totalWithdrawnNgn = existing.totalWithdrawnNgn,
                                totalCommissionPaidNgn = existing.totalCommissionPaidNgn,
                                accreditationBadge = if (resolvedStatus == StoreVerificationStatus.APPROVED) "VERIFIED SELLER" else resolvedStatus.label,
                                adminVerificationNote = sanitizedStore.adminVerificationNote.ifEmpty { existing.adminVerificationNote }
                            )
                        } else {
                            sanitizedStore
                        }
                    } else existing
                }
            } else {
                val newStore = if (!isAdmin) {
                    sanitizedStore.copy(
                        status = StoreVerificationStatus.PENDING,
                        customCommissionPercent = null,
                        availableEarningsNgn = 0L,
                        pendingEscrowNgn = 0L,
                        totalGrossSalesNgn = 0L,
                        totalWithdrawnNgn = 0L,
                        totalCommissionPaidNgn = 0L
                    )
                } else sanitizedStore
                list + newStore
            }
        }
        // Keep product sellerName/location synced with store updates
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.storeId == sanitizedStore.id) {
                    prod.copy(
                        sellerName = sanitizedStore.storeName,
                        sellerLocation = "${sanitizedStore.city}, ${sanitizedStore.state}",
                        isCertifiedSeller = (sanitizedStore.status == StoreVerificationStatus.APPROVED),
                        sellerCertificationBadge = if (sanitizedStore.status == StoreVerificationStatus.APPROVED) "VERIFIED SELLER" else sanitizedStore.status.label
                    )
                } else prod
            }
        }
        DatabaseEncryptionAndSecurityManager.recordDatabaseActivity(
            actorUid = callerUid.ifBlank { sanitizedStore.ownerUid },
            actorEmail = callerEmail.ifBlank { sanitizedStore.ownerEmail },
            actorRole = if (isAdmin) UserRole.ADMIN else UserRole.VERIFIED_SELLER,
            operation = if (exists) DatabaseOperationType.UPDATE_MUTATE else DatabaseOperationType.INSERT_CREATE,
            collectionOrTable = "stores",
            documentOrRowId = sanitizedStore.id,
            encryptedFieldsProtected = true,
            authorized = true,
            details = "Store record persisted with AES-256-GCM field encryption and ledger HMAC"
        )
    }

    fun updateStoreVerificationStatus(
        storeId: String,
        status: StoreVerificationStatus,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.STORE_VERIFICATION_STATUS_CHANGE,
            targetEntityId = storeId,
            description = "Change store verification status to ${status.name}"
        )
        val cleanAdminNote = adminNote?.let { SecuritySanitizer.sanitizeText(it, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH) }
        val prevStatus = _sharedStores.value.find { it.id == storeId }?.status?.name ?: "UNKNOWN"
        _sharedStores.update { list ->
            list.map { store ->
                if (store.id == storeId) {
                    val resolvedOpeningDate = if (status == StoreVerificationStatus.APPROVED &&
                        (store.storeOpeningDate.isBlank() || store.storeOpeningDate.startsWith("Applied"))
                    ) {
                        "Opened Today • Active Store"
                    } else {
                        store.storeOpeningDate
                    }
                    store.copy(
                        status = status,
                        accreditationBadge = if (status == StoreVerificationStatus.APPROVED) "VERIFIED SELLER" else status.label,
                        storeOpeningDate = resolvedOpeningDate,
                        adminVerificationNote = cleanAdminNote ?: store.adminVerificationNote
                    )
                } else store
            }
        }
        _sharedProducts.update { list ->
            list.map { prod ->
                if (prod.storeId == storeId) {
                    prod.copy(
                        isCertifiedSeller = (status == StoreVerificationStatus.APPROVED),
                        sellerCertificationBadge = if (status == StoreVerificationStatus.APPROVED) "VERIFIED SELLER" else status.label
                    )
                } else prod
            }
        }
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.STORE_VERIFICATION_STATUS_CHANGE,
                targetEntityId = storeId,
                description = "Updated store verification status to ${status.name}. Note: ${cleanAdminNote ?: "None"}",
                previousValue = prevStatus,
                newValue = status.name,
                stepUpMfaVerified = true
            )
        )
    }

    fun addDeliveryAddress(
        address: DeliveryAddress,
        callerUid: String = "",
        callerEmail: String = ""
    ) {
        val sanitizedAddress = address.copy(
            userUid = address.userUid.ifBlank { callerUid },
            userEmail = address.userEmail.ifBlank { callerEmail },
            label = SecuritySanitizer.sanitizeText(address.label, maxLength = 60),
            recipientName = SecuritySanitizer.sanitizeText(address.recipientName, maxLength = 120),
            phone = SecuritySanitizer.sanitizeText(address.phone, maxLength = 40),
            streetAddress = SecuritySanitizer.sanitizeText(address.streetAddress, maxLength = 250),
            city = SecuritySanitizer.sanitizeText(address.city, maxLength = 80),
            state = SecuritySanitizer.sanitizeText(address.state, maxLength = 80)
        )
        _sharedAddresses.update { list ->
            val updated = if (sanitizedAddress.isDefault) {
                list.map { it.copy(isDefault = false) }
            } else list
            updated + sanitizedAddress
        }
        DatabaseEncryptionAndSecurityManager.recordDatabaseActivity(
            actorUid = sanitizedAddress.userUid,
            actorEmail = sanitizedAddress.userEmail,
            actorRole = UserRole.CUSTOMER,
            operation = DatabaseOperationType.INSERT_CREATE,
            collectionOrTable = "addresses",
            documentOrRowId = sanitizedAddress.id,
            encryptedFieldsProtected = true,
            authorized = true,
            details = "Customer delivery address persisted with AES-256-GCM encryption"
        )
    }

    fun setDefaultAddress(addressId: String) {
        _sharedAddresses.update { list ->
            list.map { it.copy(isDefault = it.id == addressId) }
        }
    }

    fun deleteAddress(
        addressId: String,
        callerUid: String = "",
        callerEmail: String = "",
        isAdmin: Boolean = false
    ): Boolean {
        val target = _sharedAddresses.value.find { it.id == addressId } ?: return false
        if (callerUid.isNotBlank() && !isAdmin) {
            val ownerMatch = target.userUid == callerUid ||
                (callerEmail.isNotBlank() && target.userEmail.equals(callerEmail, ignoreCase = true)) ||
                (target.userUid.isBlank() && target.userEmail.isBlank())
            if (!ownerMatch) return false
        }
        _sharedAddresses.update { list ->
            val filtered = list.filterNot { it.id == addressId }
            if (filtered.isNotEmpty() && filtered.none { it.isDefault }) {
                filtered.mapIndexed { idx, addr -> addr.copy(isDefault = idx == 0) }
            } else filtered
        }
        return true
    }

    fun submitProductReview(
        productId: String,
        customerName: String,
        rating: Int,
        comment: String
    ) {
        val product = _sharedProducts.value.find { it.id == productId } ?: return
        val newReview = ProductReview(
            id = "rev_${UUID.randomUUID().toString().take(8)}",
            productId = productId,
            storeId = product.storeId,
            customerName = SecuritySanitizer.sanitizeText(customerName, maxLength = 120),
            rating = rating.coerceIn(1, 5),
            comment = SecuritySanitizer.sanitizeText(comment, maxLength = 1000),
            date = "Just now",
            verifiedPurchase = true
        )
        _sharedReviews.update { listOf(newReview) + it }

        // Recalculate product rating
        val prodReviews = _sharedReviews.value.filter { it.productId == productId }
        val avgRating = prodReviews.map { it.rating }.average().toFloat()
        _sharedProducts.update { list ->
            list.map {
                if (it.id == productId) {
                    it.copy(
                        rating = ((avgRating * 10).toInt() / 10f),
                        reviewsCount = it.reviewsCount + 1
                    )
                } else it
            }
        }
    }

    fun replyToProductReview(reviewId: String, sellerReply: String) {
        val cleanReply = SecuritySanitizer.sanitizeText(sellerReply, maxLength = 1000)
        _sharedReviews.update { list ->
            list.map { rev ->
                if (rev.id == reviewId) {
                    rev.copy(
                        sellerReply = cleanReply,
                        sellerReplyDate = "Replied Today"
                    )
                } else rev
            }
        }
    }

    fun placeOrder(
        customerName: String,
        customerEmail: String,
        address: DeliveryAddress,
        paymentPlan: PaymentPlanType,
        paymentMethod: PaymentMethodType,
        paymentReference: String? = null
    ): MarketplaceOrder? {
        return placeMultiSellerOrders(
            customerName = customerName,
            customerEmail = customerEmail,
            address = address,
            paymentPlan = paymentPlan,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference
        ).firstOrNull()
    }

    /**
     * Supports multiple sellers in one cart while maintaining separate seller fulfilment records
     * and order breakdowns for each seller store.
     * Enforces server-side payment verification, duplicate prevention, and marketplace split records.
     */
    fun placeMultiSellerOrders(
        customerName: String,
        customerEmail: String,
        address: DeliveryAddress,
        paymentPlan: PaymentPlanType,
        paymentMethod: PaymentMethodType,
        paymentReference: String? = null
    ): List<MarketplaceOrder> {
        val currentCart = _sharedCartItems.value
        if (currentCart.isEmpty()) return emptyList()

        val validationIssues = validateCartBeforeCheckout()
        if (validationIssues.isNotEmpty()) {
            return emptyList()
        }

        val breakdown = calculateCartBreakdown()

        // Requirement 4: Prevent duplicate payment processing & reference reuse
        if (paymentReference != null && _sharedOrders.value.any { it.paymentReference == paymentReference }) {
            return emptyList()
        }

        // Server-Side Payment Verification (Requirements 1, 2, 4, 7, 9)
        // Never rely on frontend success messages; strictly verify on the server against the gateway.
        val resolvedPaymentRef = paymentReference ?: _sharedPaymentGateway.generateUniquePaymentReference()
        val verification = _sharedPaymentGateway.verifyPaymentOnServer(
            reference = resolvedPaymentRef,
            expectedOrderTotalNgn = breakdown.finalOrderTotalNgn,
            customerEmail = customerEmail,
            customerName = customerName,
            channel = paymentMethod.name.lowercase()
        )
        if (!verification.success) {
            return emptyList()
        }

        val checkoutGroupId = "CHK-${SecuritySanitizer.nextSecureInt(10000, 99999)}"

        // Step 1: Build individual SellerFulfilmentRecord entries per seller store in the cart
        val fulfilmentRecords = breakdown.sellerBreakdowns.map { sellerPart ->
            val orderNum = SecuritySanitizer.nextSecureInt(10000, 99999)
            val orderId = "SF-$orderNum"
            val trackingCode = "SF-TRK-${SecuritySanitizer.nextSecureInt(100000, 999999)}"
            val orderItems = sellerPart.items.map {
                OrderItemSummary(
                    productId = it.product.id,
                    productTitle = it.product.title,
                    storeId = sellerPart.storeId,
                    storeName = sellerPart.storeName,
                    quantity = it.quantity,
                    unitPriceNgn = it.product.basePriceNgn,
                    warrantyPeriod = it.product.warrantyPeriod
                )
            }
            SellerFulfilmentRecord(
                fulfilmentOrderId = orderId,
                storeId = sellerPart.storeId,
                storeName = sellerPart.storeName,
                trackingNumber = trackingCode,
                status = OrderStatus.CONFIRMED,
                items = orderItems,
                subtotalNgn = sellerPart.sellerSubtotalNgn,
                deliveryFeeNgn = sellerPart.sellerDeliveryFeeNgn,
                paymentGatewayFeeNgn = sellerPart.paymentGatewayFeeNgn,
                commissionRatePercent = sellerPart.commissionRatePercent,
                solarfixCommissionNgn = sellerPart.solarfixCommissionNgn,
                netSellerEarningsNgn = sellerPart.netSellerPayoutNgn,
                totalSellerOrderAmountNgn = sellerPart.sellerOrderTotalNgn
            )
        }

        // Step 2: Create a separate MarketplaceOrder fulfilment record per seller store
        val createdOrders = mutableListOf<MarketplaceOrder>()
        val createdTransactions = mutableListOf<MarketplaceTransaction>()

        fulfilmentRecords.forEach { record ->
            val initialPaid = when (paymentPlan) {
                PaymentPlanType.OUTRIGHT -> record.totalSellerOrderAmountNgn
                PaymentPlanType.INSTALLMENT_3M -> (record.totalSellerOrderAmountNgn * 0.40).roundToLong()
                PaymentPlanType.INSTALLMENT_6M -> (record.totalSellerOrderAmountNgn * 0.30).roundToLong()
            }

            val multiSellerNote = if (fulfilmentRecords.size > 1) {
                "Multi-Seller Checkout Group $checkoutGroupId (${fulfilmentRecords.size} separate seller fulfilment orders)"
            } else {
                "Single-Seller Direct Store Checkout"
            }

            val sellerOrder = MarketplaceOrder(
                id = record.fulfilmentOrderId,
                customerName = customerName,
                customerEmail = customerEmail,
                items = record.items,
                primaryStoreId = record.storeId,
                primaryStoreName = record.storeName,
                subtotalNgn = record.subtotalNgn,
                deliveryFeeNgn = record.deliveryFeeNgn,
                paymentGatewayFeeNgn = record.paymentGatewayFeeNgn,
                totalAmountNgn = record.totalSellerOrderAmountNgn,
                commissionPercent = record.commissionRatePercent,
                solarfixCommissionNgn = record.solarfixCommissionNgn,
                netSellerEarningsNgn = record.netSellerEarningsNgn,
                paymentPlan = paymentPlan,
                paymentMethod = paymentMethod,
                amountPaidSoFarNgn = initialPaid,
                deliveryAddress = address,
                status = OrderStatus.CONFIRMED,
                trackingNumber = record.trackingNumber,
                createdAt = "Today, Just now",
                estimatedDelivery = "Within 24 - 48 Hours",
                statusTimeline = listOf(
                    "Order ${record.fulfilmentOrderId} confirmed & protected by SolarFix Escrow ($multiSellerNote)",
                    "Payment verified via Paystack Gateway (Ref: ${verification.reference}): ₦${String.format("%,d", initialPaid)}",
                    "Transmitted to ${record.storeName} for QC & packaging"
                ),
                escrowReleased = false,
                checkoutGroupId = checkoutGroupId,
                sellerFulfilments = fulfilmentRecords,
                paymentReference = verification.reference,
                gatewayVerificationId = verification.gatewayReference,
                isGatewayVerified = true
            )
            createdOrders.add(sellerOrder)

            // Update this seller store's pending escrow, gross sales & successful sales count
            val unitsForStore = record.items.sumOf { it.quantity }
            _sharedStores.update { list ->
                list.map { st ->
                    if (st.id == record.storeId) {
                        st.copy(
                            totalGrossSalesNgn = st.totalGrossSalesNgn + record.subtotalNgn,
                            pendingEscrowNgn = st.pendingEscrowNgn + record.netSellerEarningsNgn,
                            totalCommissionPaidNgn = st.totalCommissionPaidNgn + record.solarfixCommissionNgn,
                            successfulSalesCount = st.successfulSalesCount + unitsForStore
                        )
                    } else st
                }
            }

            // Record Order Payment & Commission transactions for this seller store (preventing duplicate commission deduction)
            val commissionRegistered = RateLimitingAndFinancialSecurityManager.tryRegisterUniqueCommissionDeduction(
                orderId = record.fulfilmentOrderId,
                storeId = record.storeId,
                commissionNgn = record.solarfixCommissionNgn,
                netSellerEarningsNgn = record.netSellerEarningsNgn
            )
            val paymentTx = MarketplaceTransaction(
                id = "TX-${(100000..999999).random()}",
                type = TransactionType.ORDER_PAYMENT,
                title = "Order #${record.fulfilmentOrderId} Escrow Credit",
                subtitle = "${record.items.first().productTitle} (${paymentMethod.label})",
                amountNgn = record.subtotalNgn,
                storeId = record.storeId,
                storeName = record.storeName,
                orderId = record.fulfilmentOrderId,
                timestamp = "Just now",
                isCreditToSeller = true
            )
            if (commissionRegistered) {
                val commissionTx = MarketplaceTransaction(
                    id = "TX-${(100000..999999).random()}",
                    type = TransactionType.COMMISSION_DEDUCTION,
                    title = "SolarFix Commission (${record.commissionRatePercent}%)",
                    subtitle = "Platform fee deducted on Order #${record.fulfilmentOrderId}",
                    amountNgn = record.solarfixCommissionNgn,
                    storeId = record.storeId,
                    storeName = record.storeName,
                    orderId = record.fulfilmentOrderId,
                    timestamp = "Just now",
                    isCreditToSeller = false
                )
                createdTransactions.add(commissionTx)
            }
            createdTransactions.add(paymentTx)
        }

        // Step 3: Deduct stock counts for all products across all sellers in the cart
        _sharedProducts.update { list ->
            list.map { prod ->
                val cartEntry = currentCart.find { it.product.id == prod.id }
                if (cartEntry != null) {
                    val remaining = (prod.stockCount - cartEntry.quantity).coerceAtLeast(0)
                    prod.copy(stockCount = remaining, inStock = remaining > 0)
                } else prod
            }
        }

        _sharedTransactions.update { createdTransactions + it }
        _sharedOrders.update { createdOrders + it }
        clearCart()
        return createdOrders
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        val targetOrder = _sharedOrders.value.find { it.id == orderId } ?: return
        val shouldReleaseEscrow = (newStatus == OrderStatus.DELIVERED && !targetOrder.escrowReleased)

        val newTimelineEntry = when (newStatus) {
            OrderStatus.PROCESSING -> "Seller store started technical inspection & serial scan"
            OrderStatus.PACKED -> "Equipment packed with warranty seal & ready for courier pickup"
            OrderStatus.IN_TRANSIT -> "Dispatched via SolarFix Logistics (${targetOrder.trackingNumber})"
            OrderStatus.OUT_FOR_DELIVERY -> "Delivery rider is en route to ${targetOrder.deliveryAddress.city}"
            OrderStatus.DELIVERED -> "Delivered & confirmed. Net earnings released to Seller Available Balance"
            else -> "Status updated to ${newStatus.label}"
        }

        _sharedOrders.update { list ->
            list.map { order ->
                if (order.id == orderId) {
                    order.copy(
                        status = newStatus,
                        statusTimeline = order.statusTimeline + newTimelineEntry,
                        escrowReleased = order.escrowReleased || shouldReleaseEscrow
                    )
                } else order
            }
        }

        if (shouldReleaseEscrow && RateLimitingAndFinancialSecurityManager.tryRegisterUniqueEscrowRelease(
                orderId = orderId,
                storeId = targetOrder.primaryStoreId,
                netSellerEarningsNgn = targetOrder.netSellerEarningsNgn
            )
        ) {
            _sharedStores.update { list ->
                list.map { st ->
                    if (st.id == targetOrder.primaryStoreId) {
                        st.copy(
                            pendingEscrowNgn = (st.pendingEscrowNgn - targetOrder.netSellerEarningsNgn).coerceAtLeast(0L),
                            availableEarningsNgn = st.availableEarningsNgn + targetOrder.netSellerEarningsNgn,
                            successfulSalesCount = st.successfulSalesCount + 1
                        )
                    } else st
                }
            }
            val releaseTx = MarketplaceTransaction(
                id = "TX-${(100000..999999).random()}",
                type = TransactionType.ESCROW_RELEASE,
                title = "Escrow Released to Available Earnings",
                subtitle = "Order #$orderId delivered & verified",
                amountNgn = targetOrder.netSellerEarningsNgn,
                storeId = targetOrder.primaryStoreId,
                storeName = targetOrder.primaryStoreName,
                orderId = orderId,
                timestamp = "Just now",
                isCreditToSeller = true
            )
            _sharedTransactions.update { listOf(releaseTx) + it }
        }
    }

    /**
     * Allows a seller to explicitly accept a newly placed customer order according to SolarFix marketplace rules.
     */
    fun acceptOrderBySeller(
        orderId: String,
        callerStoreId: String? = null,
        isAdmin: Boolean = false
    ): Boolean {
        val targetOrder = _sharedOrders.value.find { it.id == orderId } ?: return false
        if (callerStoreId != null && !isAdmin) {
            val belongs = targetOrder.primaryStoreId == callerStoreId ||
                targetOrder.sellerFulfilments.any { it.storeId == callerStoreId }
            if (!belongs) return false
        }
        if (targetOrder.status == OrderStatus.REJECTED_BY_SELLER || targetOrder.status == OrderStatus.REFUNDED) {
            return false
        }
        val nextStatus = if (targetOrder.status == OrderStatus.CONFIRMED) OrderStatus.PROCESSING else targetOrder.status
        _sharedOrders.update { list ->
            list.map { ord ->
                if (ord.id == orderId) {
                    ord.copy(
                        status = nextStatus,
                        sellerAccepted = true,
                        sellerRejectionReason = null,
                        statusTimeline = ord.statusTimeline + "Order accepted by ${ord.primaryStoreName} — preparing QC & serial verification"
                    )
                } else ord
            }
        }
        return true
    }

    /**
     * Allows a seller to reject an unshipped order according to SolarFix marketplace rules (e.g., stock discrepancy or unserviceable zone),
     * automatically refunding the customer, restoring product stock, and reversing pending escrow.
     */
    fun rejectOrderBySeller(
        orderId: String,
        rejectionReason: String,
        callerStoreId: String? = null,
        isAdmin: Boolean = false
    ): Boolean {
        val targetOrder = _sharedOrders.value.find { it.id == orderId } ?: return false
        if (callerStoreId != null && !isAdmin) {
            val belongs = targetOrder.primaryStoreId == callerStoreId ||
                targetOrder.sellerFulfilments.any { it.storeId == callerStoreId }
            if (!belongs) return false
        }
        // Marketplace rule: only unshipped orders (CONFIRMED or PROCESSING) can be rejected by seller
        if (targetOrder.status != OrderStatus.CONFIRMED && targetOrder.status != OrderStatus.PROCESSING) {
            return false
        }
        val cleanReason = SecuritySanitizer.sanitizeText(rejectionReason, maxLength = 300).ifEmpty {
            "Out of Stock / Failed Pre-Dispatch QC"
        }

        _sharedOrders.update { list ->
            list.map { ord ->
                if (ord.id == orderId) {
                    ord.copy(
                        status = OrderStatus.REJECTED_BY_SELLER,
                        sellerAccepted = false,
                        sellerRejectionReason = cleanReason,
                        statusTimeline = ord.statusTimeline + "Order rejected by seller ($cleanReason) — 100% customer escrow refund issued"
                    )
                } else ord
            }
        }

        // Restore stock for each item in the rejected order
        _sharedProducts.update { list ->
            list.map { prod ->
                val itemEntry = targetOrder.items.find { it.productId == prod.id }
                if (itemEntry != null) {
                    val restored = prod.stockCount + itemEntry.quantity
                    prod.copy(stockCount = restored, inStock = restored > 0)
                } else prod
            }
        }

        // Reverse store pending escrow, gross sales, and commission
        val unitsInOrder = targetOrder.items.sumOf { it.quantity }
        _sharedStores.update { list ->
            list.map { st ->
                if (st.id == targetOrder.primaryStoreId) {
                    st.copy(
                        totalGrossSalesNgn = (st.totalGrossSalesNgn - targetOrder.subtotalNgn).coerceAtLeast(0L),
                        pendingEscrowNgn = (st.pendingEscrowNgn - targetOrder.netSellerEarningsNgn).coerceAtLeast(0L),
                        totalCommissionPaidNgn = (st.totalCommissionPaidNgn - targetOrder.solarfixCommissionNgn).coerceAtLeast(0L),
                        successfulSalesCount = (st.successfulSalesCount - unitsInOrder).coerceAtLeast(0)
                    )
                } else st
            }
        }

        val refundTx = MarketplaceTransaction(
            id = "TX-${SecuritySanitizer.nextSecureInt(100000, 999999)}",
            type = TransactionType.CUSTOMER_REFUND,
            title = "Order #$orderId Rejected — Buyer Refunded",
            subtitle = "Reason: $cleanReason",
            amountNgn = targetOrder.subtotalNgn,
            storeId = targetOrder.primaryStoreId,
            storeName = targetOrder.primaryStoreName,
            orderId = orderId,
            timestamp = "Just now",
            isCreditToSeller = false
        )
        _sharedTransactions.update { listOf(refundTx) + it }
        return true
    }

    fun reportOrderIssue(
        orderId: String,
        issueType: DisputeIssueType,
        description: String
    ): ProductDispute? {
        val order = _sharedOrders.value.find { it.id == orderId } ?: return null
        val firstItem = order.items.firstOrNull()
        val cleanDesc = SecuritySanitizer.sanitizeRichDescription(description, maxLength = SecuritySanitizer.MAX_INQUIRY_NOTES_LENGTH)
        val dispute = ProductDispute(
            id = "DSP-${SecuritySanitizer.nextSecureInt(1000, 9999)}",
            orderId = order.id,
            productId = firstItem?.productId ?: "prod_unknown",
            productTitle = firstItem?.productTitle ?: "Solar Hardware Order",
            storeId = order.primaryStoreId,
            storeName = order.primaryStoreName,
            customerName = order.customerName,
            issueType = issueType,
            description = cleanDesc,
            amountNgn = order.totalAmountNgn,
            status = DisputeStatus.OPEN,
            createdAt = "Just now"
        )
        _sharedDisputes.update { listOf(dispute) + it }
        _sharedOrders.update { list ->
            list.map {
                if (it.id == orderId) {
                    it.copy(
                        status = OrderStatus.DISPUTED,
                        statusTimeline = it.statusTimeline + "Customer reported issue: ${issueType.label}"
                    )
                } else it
            }
        }
        return dispute
    }

    fun resolveDispute(
        disputeId: String,
        resolution: DisputeStatus,
        adminNote: String,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.DISPUTE_RESOLUTION,
            targetEntityId = disputeId,
            description = "Resolve customer dispute to ${resolution.name}"
        )
        val targetDispute = _sharedDisputes.value.find { it.id == disputeId } ?: return
        val cleanNote = SecuritySanitizer.sanitizeText(adminNote, maxLength = SecuritySanitizer.MAX_ADMIN_NOTE_LENGTH)
        _sharedDisputes.update { list ->
            list.map { dsp ->
                if (dsp.id == disputeId) {
                    dsp.copy(status = resolution, adminResolutionNote = cleanNote)
                } else dsp
            }
        }

        if (resolution == DisputeStatus.RESOLVED_REFUND) {
            _sharedOrders.update { list ->
                list.map { ord ->
                    if (ord.id == targetDispute.orderId) {
                        ord.copy(
                            status = OrderStatus.REFUNDED,
                            statusTimeline = ord.statusTimeline + "Admin approved full customer refund: $cleanNote"
                        )
                    } else ord
                }
            }
            _sharedStores.update { list ->
                list.map { st ->
                    if (st.id == targetDispute.storeId) {
                        st.copy(
                            pendingEscrowNgn = (st.pendingEscrowNgn - targetDispute.amountNgn).coerceAtLeast(0L)
                        )
                    } else st
                }
            }
            val refundTx = MarketplaceTransaction(
                id = "TX-${SecuritySanitizer.nextSecureInt(100000, 999999)}",
                type = TransactionType.CUSTOMER_REFUND,
                title = "Customer Refund Issued (${targetDispute.orderId})",
                subtitle = "${targetDispute.issueType.label} • $cleanNote",
                amountNgn = targetDispute.amountNgn,
                storeId = targetDispute.storeId,
                storeName = targetDispute.storeName,
                orderId = targetDispute.orderId,
                timestamp = "Just now",
                isCreditToSeller = false
            )
            _sharedPaymentGateway.processRefund(
                originalReference = targetDispute.orderId,
                orderId = targetDispute.orderId,
                amountNgn = targetDispute.amountNgn,
                reason = adminNote,
                storeId = targetDispute.storeId,
                storeName = targetDispute.storeName,
                customerEmail = "customer@solarfix.africa"
            )
            _sharedTransactions.update { listOf(refundTx) + it }
        } else if (resolution == DisputeStatus.RESOLVED_REPLACEMENT) {
            _sharedOrders.update { list ->
                list.map { ord ->
                    if (ord.id == targetDispute.orderId) {
                        ord.copy(
                            status = OrderStatus.IN_TRANSIT,
                            statusTimeline = ord.statusTimeline + "Replacement unit dispatched under warranty: $adminNote"
                        )
                    } else ord
                }
            }
        }
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.DISPUTE_RESOLUTION,
                targetEntityId = disputeId,
                description = "Resolved dispute $disputeId as ${resolution.name}: $cleanNote",
                previousValue = "OPEN",
                newValue = resolution.name,
                stepUpMfaVerified = true
            )
        )
    }

    fun requestSellerWithdrawal(
        storeId: String,
        amountNgn: Long,
        bankName: String,
        accountNumber: String,
        accountName: String
    ): WithdrawalRequest? {
        val store = _sharedStores.value.find { it.id == storeId } ?: return null
        val verifiedSubaccount = _sharedPaymentGateway.sellerSubaccounts.value[storeId]?.accountNumber
        val destinationCheck = RateLimitingAndFinancialSecurityManager.validateSellerWithdrawalDestination(
            store = store,
            requestedAccountNumber = accountNumber,
            requestedBankName = bankName,
            amountNgn = amountNgn,
            verifiedSubaccountNumber = verifiedSubaccount
        )
        if (!destinationCheck.isValid) return null

        val req = WithdrawalRequest(
            id = "WTH-${SecuritySanitizer.nextSecureInt(1000, 9999)}",
            storeId = store.id,
            storeName = store.storeName,
            amountNgn = amountNgn,
            bankName = SecuritySanitizer.sanitizeText(bankName, maxLength = 100),
            accountNumber = accountNumber.trim(),
            accountName = SecuritySanitizer.sanitizeText(accountName, maxLength = 120),
            status = WithdrawalStatus.PENDING,
            requestedAt = "Just now"
        )

        _sharedStores.update { list ->
            list.map { st ->
                if (st.id == storeId) {
                    st.copy(availableEarningsNgn = (st.availableEarningsNgn - amountNgn).coerceAtLeast(0L))
                } else st
            }
        }
        _sharedWithdrawals.update { listOf(req) + it }

        RateLimitingAndFinancialSecurityManager.appendFinancialAuditEntry(
            eventType = FinancialAuditEventType.WITHDRAWAL_REQUESTED_TO_VERIFIED_BANK,
            storeId = store.id,
            orderOrReferenceId = req.id,
            amountNgn = amountNgn,
            commissionNgn = 0L,
            netSellerBalanceDeltaNgn = -amountNgn,
            actorEmail = store.ownerEmail,
            stepUpAuthorized = true,
            details = "Seller withdrawal requested (${req.id}) for ₦$amountNgn to verified account ${SecuritySanitizer.maskSensitiveValue(req.accountNumber, 4)} (${req.bankName})"
        )
        return req
    }

    fun processWithdrawalRequest(
        withdrawalId: String,
        approve: Boolean,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = if (approve) AdminActionType.WITHDRAWAL_APPROVAL else AdminActionType.WITHDRAWAL_REJECTION,
            targetEntityId = withdrawalId,
            description = "Process withdrawal request approve=$approve"
        )
        val req = _sharedWithdrawals.value.find { it.id == withdrawalId } ?: return
        if (req.status != WithdrawalStatus.PENDING) return

        val newStatus = if (approve) WithdrawalStatus.APPROVED_PAID else WithdrawalStatus.REJECTED
        _sharedWithdrawals.update { list ->
            list.map {
                if (it.id == withdrawalId) {
                    it.copy(status = newStatus, processedAt = "Processed Today")
                } else it
            }
        }

        if (approve) {
            _sharedStores.update { list ->
                list.map { st ->
                    if (st.id == req.storeId) {
                        st.copy(totalWithdrawnNgn = st.totalWithdrawnNgn + req.amountNgn)
                    } else st
                }
            }
            val payoutTx = MarketplaceTransaction(
                id = "TX-${SecuritySanitizer.nextSecureInt(100000, 999999)}",
                type = TransactionType.SELLER_WITHDRAWAL,
                title = "Bank Payout to ${req.bankName}",
                subtitle = "${req.accountName} (${SecuritySanitizer.maskSensitiveValue(req.accountNumber, 4)})",
                amountNgn = req.amountNgn,
                storeId = req.storeId,
                storeName = req.storeName,
                timestamp = "Just now",
                isCreditToSeller = false
            )
            _sharedTransactions.update { listOf(payoutTx) + it }
        } else {
            // Return funds to seller's available balance if rejected
            _sharedStores.update { list ->
                list.map { st ->
                    if (st.id == req.storeId) {
                        st.copy(availableEarningsNgn = st.availableEarningsNgn + req.amountNgn)
                    } else st
                }
            }
        }
        RateLimitingAndFinancialSecurityManager.appendFinancialAuditEntry(
            eventType = if (approve) FinancialAuditEventType.WITHDRAWAL_APPROVED_WITH_STEP_UP_AUTH else FinancialAuditEventType.WITHDRAWAL_REJECTED_FUNDS_RETURNED,
            storeId = req.storeId,
            orderOrReferenceId = withdrawalId,
            amountNgn = req.amountNgn,
            commissionNgn = 0L,
            netSellerBalanceDeltaNgn = if (approve) 0L else req.amountNgn,
            actorEmail = adminEmail,
            stepUpAuthorized = true,
            details = if (approve) "Admin approved payout $withdrawalId of ₦${req.amountNgn}" else "Admin rejected payout $withdrawalId and returned ₦${req.amountNgn} to available balance"
        )
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = if (approve) AdminActionType.WITHDRAWAL_APPROVAL else AdminActionType.WITHDRAWAL_REJECTION,
                targetEntityId = withdrawalId,
                description = if (approve) "Approved withdrawal of ₦${req.amountNgn} to ${req.bankName}" else "Rejected withdrawal of ₦${req.amountNgn}: ${adminNote ?: "Rejected"}",
                previousValue = WithdrawalStatus.PENDING.name,
                newValue = newStatus.name,
                stepUpMfaVerified = true
            )
        )
    }

    fun setSellerCustomCommission(
        storeId: String,
        customCommissionPercent: Double?,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.CUSTOM_COMMISSION_UPDATE,
            targetEntityId = storeId,
            description = "Update custom commission for store $storeId"
        )
        val prevComm = _sharedStores.value.find { it.id == storeId }?.customCommissionPercent?.toString() ?: "DEFAULT"
        val safeComm = customCommissionPercent?.coerceIn(0.0, 50.0)
        _sharedStores.update { list ->
            list.map { st ->
                if (st.id == storeId) {
                    st.copy(customCommissionPercent = safeComm)
                } else st
            }
        }
        RateLimitingAndFinancialSecurityManager.appendFinancialAuditEntry(
            eventType = FinancialAuditEventType.COMMISSION_RATE_UPDATED_BY_ADMIN,
            storeId = storeId,
            orderOrReferenceId = "COMM_UPDATE_$storeId",
            amountNgn = 0L,
            commissionNgn = 0L,
            netSellerBalanceDeltaNgn = 0L,
            actorEmail = adminEmail,
            stepUpAuthorized = true,
            details = "Admin updated custom commission rate for $storeId from $prevComm to ${safeComm ?: "DEFAULT"}%"
        )
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.CUSTOM_COMMISSION_UPDATE,
                targetEntityId = storeId,
                description = "Updated store custom commission to ${safeComm ?: "DEFAULT"}%",
                previousValue = prevComm,
                newValue = safeComm?.toString() ?: "DEFAULT",
                stepUpMfaVerified = true
            )
        )
    }

    fun updateMarketplaceSettings(
        newSettings: MarketplaceSettings,
        adminEmail: String = "admin@solarfix.africa",
        adminUid: String = ""
    ) {
        verifyAdminClearanceOrThrow(
            adminEmail = adminEmail,
            actionType = AdminActionType.MARKETPLACE_SETTINGS_UPDATE,
            targetEntityId = "marketplace_settings",
            description = "Update global marketplace financial settings"
        )
        val prevSettings = _sharedSettings.value.toString()
        val sanitized = newSettings.copy(
            defaultCommissionPercent = newSettings.defaultCommissionPercent.coerceIn(0.0, 50.0),
            escrowHoldHours = newSettings.escrowHoldHours.coerceIn(1, 720),
            minWithdrawalNgn = newSettings.minWithdrawalNgn.coerceIn(1000L, 50_000_000L)
        )
        _sharedSettings.value = sanitized
        recordAdminAction(
            AdminActionAuditRecord(
                adminEmail = adminEmail,
                adminUid = adminUid,
                actionType = AdminActionType.MARKETPLACE_SETTINGS_UPDATE,
                targetEntityId = "marketplace_settings",
                description = "Updated global marketplace settings: commission=${sanitized.defaultCommissionPercent}%, escrow=${sanitized.escrowHoldHours}h, minWithdrawal=₦${sanitized.minWithdrawalNgn}",
                previousValue = prevSettings,
                newValue = sanitized.toString(),
                stepUpMfaVerified = true
            )
        )
    }

    fun generateFinancialReport(): FinancialReportSummary {
        val orders = _sharedOrders.value
        val stores = _sharedStores.value
        val withdrawals = _sharedWithdrawals.value
        val disputes = _sharedDisputes.value

        val gmv = orders.sumOf { it.totalAmountNgn }
        val totalCommission = orders.sumOf { it.solarfixCommissionNgn }
        val totalPendingEscrow = stores.sumOf { it.pendingEscrowNgn }
        val totalAvailable = stores.sumOf { it.availableEarningsNgn }
        val totalPaidWithdrawals = withdrawals
            .filter { it.status == WithdrawalStatus.APPROVED_PAID }
            .sumOf { it.amountNgn }
        val totalRefunded = orders
            .filter { it.status == OrderStatus.REFUNDED }
            .sumOf { it.totalAmountNgn }

        return FinancialReportSummary(
            generatedAt = "Generated Live • SolarFix Multi-Vendor Ledger",
            totalOrdersCount = orders.size,
            grossMerchandiseValueNgn = gmv,
            totalSolarfixCommissionNgn = totalCommission,
            totalPendingEscrowNgn = totalPendingEscrow,
            totalSellerAvailableEarningsNgn = totalAvailable,
            totalWithdrawalsPaidNgn = totalPaidWithdrawals,
            totalRefundedNgn = totalRefunded,
            activeVerifiedStoresCount = stores.count { it.status == StoreVerificationStatus.APPROVED },
            openDisputesCount = disputes.count { it.status == DisputeStatus.OPEN }
        )
    }

    /**
     * Computes comprehensive Seller Sales Analytics for a store:
     * Total sales, Total revenue, Number of orders, Completed orders, Pending orders,
     * Available balance, SolarFix commissions, and Monthly sales performance.
     */
    fun computeSellerSalesAnalytics(storeId: String): SellerSalesAnalytics {
        val store = _sharedStores.value.find { it.id == storeId } ?: _sharedStores.value.first()
        val storeOrders = _sharedOrders.value.filter { it.primaryStoreId == store.id }
        val storeProducts = _sharedProducts.value.filter { it.storeId == store.id }
        val settings = _sharedSettings.value
        val commissionPct = store.customCommissionPercent ?: settings.defaultCommissionPercent
        val sellerProceedsPct = (100.0 - commissionPct).coerceAtLeast(0.0)

        val completedOrders = storeOrders.count { it.status == OrderStatus.DELIVERED }
        val pendingOrders = storeOrders.count {
            it.status in setOf(
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.PACKED,
                OrderStatus.IN_TRANSIT,
                OrderStatus.OUT_FOR_DELIVERY
            )
        }
        val rejectedOrRefundedOrders = storeOrders.count {
            it.status == OrderStatus.REJECTED_BY_SELLER || it.status == OrderStatus.REFUNDED
        }

        val validOrders = storeOrders.filter {
            it.status != OrderStatus.REJECTED_BY_SELLER && it.status != OrderStatus.REFUNDED
        }
        val liveOrderGross = validOrders.sumOf { it.subtotalNgn }
        val liveOrderCommission = validOrders.sumOf { it.solarfixCommissionNgn }

        val totalRevenueNgn = store.totalGrossSalesNgn.coerceAtLeast(liveOrderGross)
        val totalCommissionsNgn = store.totalCommissionPaidNgn.coerceAtLeast(liveOrderCommission)
        val netRevenueAfterCommissionNgn = (totalRevenueNgn - totalCommissionsNgn).coerceAtLeast(0L)
        val totalSalesCount = store.successfulSalesCount.coerceAtLeast(validOrders.sumOf { ord -> ord.items.sumOf { it.quantity } })

        // Build 6-month Monthly Sales Performance series (Jun 2026 - Nov 2026)
        val historicalBaseRevenue = (totalRevenueNgn - liveOrderGross).coerceAtLeast(0L)
        val historicalWeights = listOf(
            Triple("Jun 2026", "Jun", 0.12),
            Triple("Jul 2026", "Jul", 0.15),
            Triple("Aug 2026", "Aug", 0.18),
            Triple("Sep 2026", "Sep", 0.20),
            Triple("Oct 2026", "Oct", 0.22)
        )

        val historicalMonths = historicalWeights.mapIndexed { idx, (fullLabel, shortLabel, weight) ->
            val monthGross = (historicalBaseRevenue * weight).roundToLong()
            val monthComm = (monthGross * (commissionPct / 100.0)).roundToLong()
            val monthNet = (monthGross - monthComm).coerceAtLeast(0L)
            val estOrders = if (monthGross > 0L) (3 + idx * 2) else 0
            val estUnits = if (monthGross > 0L) (5 + idx * 3) else 0
            MonthlySalesMetric(
                monthLabel = fullLabel,
                shortMonth = shortLabel,
                ordersCount = estOrders,
                unitsSold = estUnits,
                grossRevenueNgn = monthGross,
                solarfixCommissionNgn = monthComm,
                netEarningsNgn = monthNet
            )
        }

        val currentMonthGross = (historicalBaseRevenue * 0.13).roundToLong() + liveOrderGross
        val currentMonthComm = (currentMonthGross * (commissionPct / 100.0)).roundToLong()
        val currentMonthNet = (currentMonthGross - currentMonthComm).coerceAtLeast(0L)
        val currentMonthOrders = validOrders.size + (if (historicalBaseRevenue > 0L) 6 else 0)
        val currentMonthUnits = validOrders.sumOf { ord -> ord.items.sumOf { it.quantity } } + (if (historicalBaseRevenue > 0L) 11 else 0)

        val currentMonthMetric = MonthlySalesMetric(
            monthLabel = "Nov 2026 (Current)",
            shortMonth = "Nov",
            ordersCount = currentMonthOrders,
            unitsSold = currentMonthUnits,
            grossRevenueNgn = currentMonthGross,
            solarfixCommissionNgn = currentMonthComm,
            netEarningsNgn = currentMonthNet
        )

        val withdrawnNgn = store.totalWithdrawnNgn
        val totalEarningsNgn = store.availableEarningsNgn + store.pendingEscrowNgn + withdrawnNgn

        return SellerSalesAnalytics(
            storeId = store.id,
            storeName = store.storeName,
            totalSalesCount = totalSalesCount,
            totalRevenueNgn = totalRevenueNgn,
            netRevenueAfterCommissionNgn = netRevenueAfterCommissionNgn,
            totalOrdersCount = storeOrders.size,
            completedOrdersCount = completedOrders,
            pendingOrdersCount = pendingOrders,
            rejectedOrRefundedOrdersCount = rejectedOrRefundedOrders,
            availableBalanceNgn = store.availableEarningsNgn,
            pendingPaymentsEscrowNgn = store.pendingEscrowNgn,
            withdrawnEarningsNgn = withdrawnNgn,
            totalEarningsNgn = totalEarningsNgn,
            solarfixCommissionsNgn = totalCommissionsNgn,
            commissionRatePercent = commissionPct,
            sellerProceedsPercent = sellerProceedsPct,
            activeProductsCount = storeProducts.count { it.isActive },
            deactivatedProductsCount = storeProducts.count { !it.isActive },
            outOfStockProductsCount = storeProducts.count { it.stockCount <= 0 || !it.inStock },
            monthlyPerformance = historicalMonths + currentMonthMetric
        )
    }

    companion object {
        private val initialStores = listOf(
            SellerStore(
                id = "store_nekes",
                storeName = "NEKES Solar Energy Store",
                ownerUid = "owner_nekes",
                ownerName = "Kingsley Nekes",
                ownerEmail = "nekeskingsley@gmail.com",
                businessRegNumber = "RC-1948201 • COREN/NEMSA Tier-1",
                taxIdNumber = "TIN-23940182-0001",
                physicalAddress = "Plot 14 Admiralty Way, Lekki Phase 1",
                city = "Ikeja & Lekki",
                state = "Lagos State",
                location = "Ikeja & Lekki Phase 1, Lagos",
                phone = "+234 803 700 1234",
                tagline = "Premier Hybrid Inverters, Grade-A LiFePO4 Batteries & Tier-1 PV Panels",
                description = "Official flagship store inside SolarFix. Every inverter, lithium battery pack, and solar panel sold by NEKES Solar Energy Store undergoes full load-bank testing before dispatch and includes direct OEM warranty coverage.",
                warrantyPolicy = "5-Year OEM Replacement & Repair Warranty + 48-Hour SolarFix Escrow Guarantee",
                rating = 5.0f,
                reviewsCount = 248,
                successfulSalesCount = 412,
                status = StoreVerificationStatus.APPROVED,
                accreditationBadge = "VERIFIED SELLER • Official Flagship",
                storeOpeningDate = "10 Jan 2025",
                joinedDate = "Jan 2025",
                totalGrossSalesNgn = 14850000L,
                pendingEscrowNgn = 1554000L,
                availableEarningsNgn = 4620000L,
                totalCommissionPaidNgn = 1113750L,
                storeBrandColorHex = 0xFF0F766E,
                idDocumentType = "CAC Certificate + International Passport",
                idDocumentNumber = "CAC-RC-1948201 / PASS-A094821",
                bankAccountName = "NEKES SOLAR ENERGY LTD",
                bankName = "Zenith Bank PLC",
                bankAccountNumber = "1019482015",
                adminVerificationNote = "Verified by SolarFix Compliance — Flagship Store Approved."
            ),
            SellerStore(
                id = "store_felicity",
                storeName = "Felicity Solar Official Direct",
                ownerUid = "owner_felicity",
                ownerName = "Chibuike Nwosu (Felicity NG)",
                ownerEmail = "sales@felicitysolar.ng",
                businessRegNumber = "RC-1084421 • OEM Factory Direct",
                physicalAddress = "Block B12 Alaba International Electronics Market",
                city = "Ojo / Festac",
                state = "Lagos State",
                location = "Alaba International & Festac, Lagos",
                phone = "+234 809 221 9000",
                tagline = "Authorized Factory Store for Felicity LiFePO4 Batteries & MPPTs",
                description = "Direct factory distribution hub for Felicity Solar in West Africa. All lithium batteries feature laser-engraved verifiable serial numbers and local service center support in Lagos, Abuja, and Kano.",
                warrantyPolicy = "5 Years Manufacturer Warranty on Lithium Batteries",
                rating = 4.9f,
                reviewsCount = 310,
                successfulSalesCount = 589,
                status = StoreVerificationStatus.APPROVED,
                accreditationBadge = "VERIFIED SELLER • Authorized OEM Distributor",
                storeOpeningDate = "04 Feb 2025",
                joinedDate = "Feb 2025",
                totalGrossSalesNgn = 21400000L,
                pendingEscrowNgn = 1341250L,
                availableEarningsNgn = 3890000L,
                totalCommissionPaidNgn = 1605000L,
                storeBrandColorHex = 0xFF1E3A8A,
                idDocumentType = "CAC Form CO7 + National ID (NIN)",
                idDocumentNumber = "NIN-49102837461",
                bankAccountName = "FELICITY SOLAR DIRECT NG",
                bankName = "Guaranty Trust Bank (GTCO)",
                bankAccountNumber = "0149827310",
                adminVerificationNote = "Factory authorization & warehouse premises verified."
            ),
            SellerStore(
                id = "store_deye",
                storeName = "Deye & Sunsynk Hub Nigeria",
                ownerUid = "owner_deye",
                ownerName = "Engr. Babatunde Alabi",
                ownerEmail = "tunde@deyehub.ng",
                businessRegNumber = "RC-1559012 • COREN #38192",
                physicalAddress = "24 Adetokunbo Ademola Crescent, Wuse II",
                city = "Wuse II",
                state = "Abuja FCT",
                location = "Wuse II, Abuja & Victoria Island, Lagos",
                phone = "+234 802 987 6543",
                tagline = "High-Voltage Hybrid Inverters, Parallel Kits & Smart BMS Accessories",
                description = "Specialists in Deye, Sunsynk, and Growatt parallel hybrid inverters for estates, hospitals, and commercial offices.",
                warrantyPolicy = "5 Years Comprehensive Hybrid Inverter Warranty",
                rating = 4.9f,
                reviewsCount = 195,
                successfulSalesCount = 318,
                status = StoreVerificationStatus.APPROVED,
                accreditationBadge = "VERIFIED SELLER • COREN Master Partner",
                storeOpeningDate = "12 Mar 2025",
                joinedDate = "Mar 2025",
                totalGrossSalesNgn = 18200000L,
                pendingEscrowNgn = 906500L,
                availableEarningsNgn = 2750000L,
                totalCommissionPaidNgn = 1365000L,
                storeBrandColorHex = 0xFF92400E,
                idDocumentType = "COREN Practicing License + Driver's License",
                idDocumentNumber = "COREN-38192-NG",
                bankAccountName = "DEYE SUNSYNK HUB NIG LTD",
                bankName = "Access Bank PLC",
                bankAccountNumber = "0089123409",
                adminVerificationNote = "COREN license & Abuja showroom verified."
            ),
            SellerStore(
                id = "store_apex",
                storeName = "Apex Solar & Balance-of-System Store",
                ownerUid = "owner_apex",
                ownerName = "Engr. Emmanuel Okafor",
                ownerEmail = "emmanuel@apexsolar.ng",
                businessRegNumber = "RC-1402981 • NEMSA Certified",
                physicalAddress = "88 Trans-Amadi Industrial Layout",
                city = "Port Harcourt",
                state = "Rivers State",
                location = "Port Harcourt & Ikeja, Nigeria",
                phone = "+234 803 456 7890",
                tagline = "Certified DC Breakers, Surge Protectors, Pure Copper Cables & Combiner Boxes",
                description = "Supplying genuine IEC-certified DC protection gear, tinned copper battery interconnects, and earthing kits.",
                warrantyPolicy = "2 Years Replacement Guarantee on Protection Gear",
                rating = 4.8f,
                reviewsCount = 164,
                successfulSalesCount = 276,
                status = StoreVerificationStatus.APPROVED,
                accreditationBadge = "VERIFIED SELLER • High-Voltage Certified",
                storeOpeningDate = "18 Apr 2025",
                joinedDate = "Apr 2025",
                totalGrossSalesNgn = 4950000L,
                pendingEscrowNgn = 240000L,
                availableEarningsNgn = 1120000L,
                totalCommissionPaidNgn = 371250L,
                storeBrandColorHex = 0xFF4C1D95,
                idDocumentType = "NEMSA Certificate + Voter's Card",
                idDocumentNumber = "NEMSA-PH-99201",
                bankAccountName = "APEX SOLAR ENGINEERING LTD",
                bankName = "First Bank of Nigeria",
                bankAccountNumber = "2034918273",
                adminVerificationNote = "Verified electrical protection supplier."
            ),
            SellerStore(
                id = "store_sungrid",
                storeName = "SunGrid Clean Energy Hub",
                ownerUid = "owner_sungrid",
                ownerName = "Musa Ibrahim",
                ownerEmail = "musa@sungrid.ng",
                businessRegNumber = "RC-2019488",
                physicalAddress = "19 Audu Bako Way, Nassarawa GRA",
                city = "Kano",
                state = "Kano State",
                location = "Nassarawa GRA, Kano",
                phone = "+234 806 334 9911",
                tagline = "Solar Borehole Pumps, VFD Inverters & Mono-PERC Panels",
                description = "Northern Nigeria's supplier for agricultural solar water pumps and residential lithium backup systems.",
                warrantyPolicy = "2 Years Standard Store Warranty",
                rating = 4.7f,
                reviewsCount = 12,
                successfulSalesCount = 0,
                status = StoreVerificationStatus.PENDING,
                accreditationBadge = "Pending Verification",
                storeOpeningDate = "Applied Today",
                joinedDate = "Applied Today",
                totalGrossSalesNgn = 0L,
                pendingEscrowNgn = 0L,
                availableEarningsNgn = 0L,
                totalCommissionPaidNgn = 0L,
                idDocumentType = "National ID (NIN Slip) + CAC Certificate",
                idDocumentNumber = "NIN-77281900421 / RC-2019488",
                bankAccountName = "SUNGRID CLEAN ENERGY HUB LTD",
                bankName = "United Bank for Africa (UBA)",
                bankAccountNumber = "1023948102",
                adminVerificationNote = "New seller application awaiting Administrator KYC/KYB verification."
            )
        )

        private val initialProducts = listOf(
            MarketplaceProduct(
                id = "prod_deye_5kw",
                title = "Deye 5kW 48V Pure Sine Wave Hybrid Inverter (SUN-5K-SG03LP1)",
                category = ProductCategory.INVERTERS,
                brand = "Deye",
                modelNumber = "SUN-5K-SG03LP1-EU",
                powerRating = "5,000 Watts Continuous / 10,000W Surge",
                voltage = "48V Battery / 230V AC Output",
                basePriceNgn = 1680000,
                rating = 5.0f,
                reviewsCount = 195,
                badge = "Installer Choice",
                inStock = true,
                specs = mapOf(
                    "Model" to "SUN-5K-SG03LP1-EU",
                    "Dual MPPT" to "2 Trackers (125V - 425V Range)",
                    "Max PV Input" to "6,500 Watts",
                    "Generator Support" to "Auto Generator Start (AGS) Port",
                    "Touchscreen" to "Color LCD Touch Display",
                    "Warranty" to "5 Years Warranty"
                ),
                description = "The gold standard in modern hybrid solar inverters. Features parallel stacking up to 16 units, smart load shedding port for high-draw ACs, generator peak shaving, and comprehensive Wi-Fi telemetry.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                deliveryAvailability = "Same-Day Dispatch in Lagos • 24-48H Nationwide Freight",
                returnPolicy = "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy",
                storeId = "store_nekes",
                warrantyPeriod = "5 Years Manufacturer Warranty",
                stockCount = 14,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-8841",
                solarfixInspectionDate = "Oct 2026"
            ),
            MarketplaceProduct(
                id = "prod_jinko_550w",
                title = "Jinko Solar 550W Tiger Pro Mono-Facial Panel",
                category = ProductCategory.PANELS,
                brand = "Jinko Solar",
                modelNumber = "JKM550M-72HL4-V",
                powerRating = "550 Watts",
                voltage = "49.8V Voc / 41.5V Vmp",
                basePriceNgn = 125000,
                rating = 4.9f,
                reviewsCount = 142,
                badge = "Best Seller",
                inStock = true,
                specs = mapOf(
                    "Model" to "JKM550M-72HL4-V",
                    "Efficiency" to "21.33%",
                    "Cell Type" to "MBB N-Type Monocrystalline",
                    "Dimensions" to "2278 x 1134 x 35 mm",
                    "Warranty" to "25-year Linear Power Warranty",
                    "Certifications" to "TUV, CE, IEC 61215"
                ),
                description = "Tier 1 ultra-high efficiency solar panel engineered for high temperatures and tropical African climate conditions. Excellent low-light performance during cloudy Harmattan mornings.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                deliveryAvailability = "Available Nationwide (Reinforced Timber Pallet Crating)",
                returnPolicy = "7-Day Micro-Crack & DOA Replacement Guarantee",
                storeId = "store_nekes",
                warrantyPeriod = "25 Years Linear Output Warranty",
                stockCount = 80,
                installmentAllowed = true,
                maxInstallmentMonths = 3,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-8842",
                solarfixInspectionDate = "Oct 2026"
            ),
            MarketplaceProduct(
                id = "prod_pylontech_35",
                title = "Pylontech US3000C 3.5kWh 48V Lithium Battery",
                category = ProductCategory.BATTERIES,
                brand = "Pylontech",
                modelNumber = "US3000C-48V",
                powerRating = "3.55 kWh (74Ah)",
                voltage = "48V Nominal",
                basePriceNgn = 1180000,
                rating = 4.9f,
                reviewsCount = 184,
                badge = "German Standard",
                inStock = true,
                specs = mapOf(
                    "Model" to "US3000C-48V",
                    "Depth of Discharge" to "95% DoD",
                    "Cycle Life" to "6,000+ Cycles",
                    "Design Life" to "15+ Years",
                    "Max Charge/Discharge" to "74A (1C Peak)",
                    "Warranty" to "7 Years Extended Warranty"
                ),
                description = "Globally certified rack-mounted lithium battery renowned for extreme reliability, seamless inverter synchronization, and modular expandability up to 16 units in parallel.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_nekes",
                warrantyPeriod = "7 Years Extended OEM Warranty",
                stockCount = 12,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-8904",
                solarfixInspectionDate = "Nov 2026"
            ),
            MarketplaceProduct(
                id = "prod_felicity_5kwh",
                title = "Felicity Solar 5.12kWh 48V LiFePO4 Lithium Battery Wall-Mount",
                category = ProductCategory.BATTERIES,
                brand = "Felicity Solar",
                modelNumber = "LPBF48100-M",
                powerRating = "5.12 kWh (100Ah)",
                voltage = "51.2V Nominal (48V System)",
                basePriceNgn = 1450000,
                rating = 4.9f,
                reviewsCount = 310,
                badge = "Top Rated",
                inStock = true,
                specs = mapOf(
                    "Model" to "LPBF48100-M",
                    "Usable Capacity" to "5.12 kWh @ 90% DoD",
                    "Cycle Life" to "6,000+ Cycles @ 25°C",
                    "BMS" to "Integrated Smart BMS with CAN/RS485",
                    "Compatibility" to "Growatt, Deye, Felicity, Must, Victron",
                    "Warranty" to "5 Years Manufacturer Warranty"
                ),
                description = "Grade-A Lithium Iron Phosphate (LiFePO4) storage solution with built-in multi-protocol BMS communication. Built-in breaker switch and LCD display showing real-time cell voltages.",
                sellerName = "Felicity Solar Official Direct",
                sellerLocation = "Alaba International & Festac, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_felicity",
                warrantyPeriod = "5 Years Manufacturer Warranty",
                stockCount = 25,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-8910",
                solarfixInspectionDate = "Nov 2026"
            ),
            MarketplaceProduct(
                id = "prod_canadian_600w",
                title = "Canadian Solar 600W HiKu7 Mono-PERC Panel",
                category = ProductCategory.PANELS,
                brand = "Canadian Solar",
                modelNumber = "CS7L-600MS",
                powerRating = "600 Watts",
                voltage = "45.2V Vmp / 13.3A Imp",
                basePriceNgn = 138000,
                rating = 4.8f,
                reviewsCount = 89,
                badge = "Heavy Duty",
                inStock = true,
                specs = mapOf(
                    "Model" to "CS7L-600MS",
                    "Efficiency" to "21.6%",
                    "Cell Type" to "144 Half-cut Monocrystalline",
                    "Weight" to "31.2 kg",
                    "Warranty" to "12-year Product / 25-year Power",
                    "Frame" to "Anodized Aluminum Alloy"
                ),
                description = "High power density panel ideal for commercial rooftops and estate residential installations requiring maximum wattage per square meter.",
                sellerName = "Felicity Solar Official Direct",
                sellerLocation = "Alaba International & Festac, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_felicity",
                warrantyPeriod = "12-Year Product / 25-Year Output Warranty",
                stockCount = 60,
                installmentAllowed = true,
                maxInstallmentMonths = 3,
                // Verified Seller, but not independently inspected by SolarFix yet
                solarfixInspectionStatus = SolarFixInspectionStatus.NOT_INSPECTED,
                solarfixInspectionReportId = null
            ),
            MarketplaceProduct(
                id = "prod_luminous_220ah",
                title = "Luminous 220Ah / 12V Tubular Deep Cycle Battery",
                category = ProductCategory.TUBULAR_BATTERIES,
                brand = "Luminous",
                modelNumber = "ILT-22060",
                powerRating = "2.64 kWh Gross (220Ah)",
                voltage = "12V",
                basePriceNgn = 380000,
                rating = 4.6f,
                reviewsCount = 420,
                badge = "Tall Tubular",
                inStock = true,
                specs = mapOf(
                    "Model" to "ILT-22060",
                    "Plate Technology" to "High-pressure Cast Tall Tubular Plates",
                    "Design Life" to "1,200 Cycles @ 80% DoD",
                    "Electrolyte" to "Lead Acid with Float Level Indicators",
                    "Weight" to "68 kg",
                    "Warranty" to "18 Months Replacement Guarantee"
                ),
                description = "Rugged tall-tubular battery specifically built to withstand erratic African power grid conditions and repeated deep discharge cycles. Built-in ceramic vent plugs minimize acid fume leakage.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_nekes",
                warrantyPeriod = "18 Months Replacement Warranty",
                stockCount = 30,
                installmentAllowed = true,
                maxInstallmentMonths = 3,
                solarfixInspectionStatus = SolarFixInspectionStatus.NOT_INSPECTED,
                solarfixInspectionReportId = null
            ),
            MarketplaceProduct(
                id = "prod_growatt_5kw",
                title = "Growatt SPF 5000ES 5kW Off-Grid Inverter with 100A MPPT",
                category = ProductCategory.INVERTERS,
                brand = "Growatt",
                modelNumber = "SPF-5000ES",
                powerRating = "5,000 Watts",
                voltage = "48V Battery / 450V Max PV Input",
                basePriceNgn = 980000,
                rating = 4.8f,
                reviewsCount = 278,
                badge = "Popular",
                inStock = true,
                specs = mapOf(
                    "Model" to "SPF-5000ES",
                    "MPPT Controller" to "Integrated 100A (450VDC Max)",
                    "Parallel Capability" to "Up to 6 units (30kW)",
                    "Battery Compatibility" to "Lithium (BMS) and Lead Acid",
                    "Weight" to "11.5 kg",
                    "Warranty" to "2 Years Standard Warranty"
                ),
                description = "Workhorse inverter installed in thousands of Nigerian and Ghanaian homes. Can run directly from solar panels without batteries during daylight hours (Battery-free mode).",
                sellerName = "Deye & Sunsynk Hub Nigeria",
                sellerLocation = "Wuse II, Abuja & Victoria Island, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_deye",
                warrantyPeriod = "2 Years Official Service Warranty",
                stockCount = 19,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-8955",
                solarfixInspectionDate = "Nov 2026"
            ),
            MarketplaceProduct(
                id = "prod_must_3kw",
                title = "Must Solar 3kW 24V Pure Sine Wave Hybrid Inverter (PV1800)",
                category = ProductCategory.INVERTERS,
                brand = "Must Solar",
                modelNumber = "PV18-3024-VPM",
                powerRating = "3,000 Watts (3kVA)",
                voltage = "24V Battery / 80A MPPT",
                basePriceNgn = 580000,
                rating = 4.7f,
                reviewsCount = 160,
                badge = "Compact Star",
                inStock = true,
                specs = mapOf(
                    "Model" to "PV18-3024-VPM",
                    "Efficiency" to "93%",
                    "Max Solar Input" to "2,000 Watts",
                    "Surge Power" to "6,000 Watts (5 sec)",
                    "Display" to "Smart LCD with configurable priority",
                    "Warranty" to "18 Months Warranty"
                ),
                description = "Reliable 24V inverter solution for 2-3 bedroom apartments, home offices, and small business shops running laptops, lights, fans, TV, and an energy-efficient inverter refrigerator.",
                sellerName = "Deye & Sunsynk Hub Nigeria",
                sellerLocation = "Wuse II, Abuja & Victoria Island, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_deye",
                warrantyPeriod = "18 Months Manufacturer Warranty",
                stockCount = 22,
                installmentAllowed = true,
                maxInstallmentMonths = 3,
                solarfixInspectionStatus = SolarFixInspectionStatus.NOT_INSPECTED,
                solarfixInspectionReportId = null
            ),
            MarketplaceProduct(
                id = "prod_srne_60a_mppt",
                title = "SRNE 60A 150V MPPT Solar Charge Controller",
                category = ProductCategory.CONTROLLERS,
                brand = "SRNE",
                modelNumber = "ML4860-150V",
                powerRating = "60 Amps Charging Current",
                voltage = "12V / 24V / 36V / 48V Auto Detect",
                basePriceNgn = 175000,
                rating = 4.8f,
                reviewsCount = 92,
                badge = "High Efficiency",
                inStock = true,
                specs = mapOf(
                    "Model" to "ML4860-150V",
                    "Tracking Efficiency" to "99.5%",
                    "Max PV Input Voltage" to "150V DC",
                    "Communication" to "RS485 / Bluetooth Optional",
                    "Cooling" to "Die-cast Aluminum Fin Cooling",
                    "Warranty" to "2 Years"
                ),
                description = "Advanced Maximum Power Point Tracking (MPPT) algorithm ensures you extract up to 30% more power from your solar array compared to standard PWM controllers.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_nekes",
                warrantyPeriod = "2 Years Replacement Warranty",
                stockCount = 34,
                installmentAllowed = true,
                maxInstallmentMonths = 3
            ),
            MarketplaceProduct(
                id = "prod_dc_cable_25mm",
                title = "25mm² Pure Copper Double Insulated Battery Cable (5 Meters)",
                category = ProductCategory.SOLAR_CABLES_WIRES,
                brand = "Top Cable Solar",
                modelNumber = "H1Z2Z2-K-25MM",
                powerRating = "Up to 150A Continuous DC",
                voltage = "Up to 1000V DC",
                basePriceNgn = 45000,
                rating = 4.9f,
                reviewsCount = 115,
                badge = "Essential Safety",
                inStock = true,
                specs = mapOf(
                    "Model" to "H1Z2Z2-K-25MM",
                    "Conductor" to "100% Oxygen-Free Annealed Copper",
                    "Insulation" to "Flame Retardant XLPE / PVC Double Jacket",
                    "Terminals" to "Tinned Copper Lug Terminals Included",
                    "Temperature Rating" to "-40°C to +105°C"
                ),
                description = "Heavy duty battery-to-inverter DC interconnect cable. Prevents overheating, voltage drop, and electrical fire risks associated with cheap substandard aluminum cables.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "5 Years Cable Integrity Guarantee",
                stockCount = 50,
                installmentAllowed = false,
                maxInstallmentMonths = 1
            ),
            MarketplaceProduct(
                id = "prod_suntree_breaker",
                title = "Suntree 600V 32A 2-Pole DC Isolator Circuit Breaker",
                category = ProductCategory.CIRCUIT_BREAKERS_PROTECTION,
                brand = "Suntree",
                modelNumber = "SL7-63-2P-DC32A",
                powerRating = "32A DC Breaking Current",
                voltage = "Up to 600V DC",
                basePriceNgn = 18500,
                rating = 4.9f,
                reviewsCount = 205,
                badge = "Certified",
                inStock = true,
                specs = mapOf(
                    "Model" to "SL7-63-2P-DC32A",
                    "Poles" to "2 Poles (Positive & Negative Isolation)",
                    "Standard" to "IEC 60947-2 DC Standard",
                    "Mounting" to "Standard 35mm DIN Rail Mount",
                    "Arc Quenching" to "Magnetic Arc Shoot Chambers"
                ),
                description = "Dedicated DC breaker for solar PV string isolation. Standard AC breakers must never be used for high-voltage DC due to arc fire hazards.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "2 Years Replacement Warranty",
                stockCount = 95,
                installmentAllowed = false,
                maxInstallmentMonths = 1
            ),
            MarketplaceProduct(
                id = "prod_spd_surge",
                title = "TOMZN 600V DC Surge Protective Device (SPD) Type 2",
                category = ProductCategory.CIRCUIT_BREAKERS_PROTECTION,
                brand = "TOMZN",
                modelNumber = "TZG40-PV-600V",
                powerRating = "20kA - 40kA Lightning Discharge",
                voltage = "600V DC Maximum Continuous",
                basePriceNgn = 22000,
                rating = 4.8f,
                reviewsCount = 140,
                badge = "Protection",
                inStock = true,
                specs = mapOf(
                    "Model" to "TZG40-PV-600V",
                    "Protection Level" to "Type 2 / Class II",
                    "Status Indicator" to "Green (Healthy) / Red (Replace Module)",
                    "Response Time" to "< 25 nanoseconds",
                    "Mounting" to "DIN Rail"
                ),
                description = "Protects your inverter and charge controller from lightning-induced voltage surges traveling down the roof solar cables during tropical thunderstorms.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "2 Years Replacement Warranty",
                stockCount = 72,
                installmentAllowed = false,
                maxInstallmentMonths = 1
            ),
            MarketplaceProduct(
                id = "prod_ecoflow_delta_pro",
                title = "EcoFlow Delta Pro 3.6kWh Portable Solar Generator Station",
                category = ProductCategory.SOLAR_GENERATORS,
                brand = "EcoFlow",
                modelNumber = "DELTA-PRO-3600W",
                powerRating = "3,600W AC Output (7,200W Surge)",
                voltage = "230V Pure Sine Wave / 1600W MPPT",
                basePriceNgn = 2850000,
                rating = 4.9f,
                reviewsCount = 64,
                badge = "Plug & Play",
                inStock = true,
                specs = mapOf(
                    "Model" to "DELTA-PRO-3600W",
                    "Battery Chemistry" to "LiFePO4 (6,500 Cycles)",
                    "Solar Input" to "1,600W Max (11-150V)",
                    "Expandability" to "Up to 10.8kWh with Extra Batteries"
                ),
                description = "All-in-one portable solar generator for apartments, clinics, and mobile field engineers requiring zero-wiring instant solar backup.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_nekes",
                warrantyPeriod = "5 Years Manufacturer Warranty",
                stockCount = 9,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-9012",
                solarfixInspectionDate = "Nov 2026"
            ),
            MarketplaceProduct(
                id = "prod_felicity_streetlight_150w",
                title = "Felicity 150W All-in-One Highway Solar Streetlight with LiFePO4",
                category = ProductCategory.SOLAR_STREETLIGHTS,
                brand = "Felicity Solar",
                modelNumber = "FL-AIO-150W-PRO",
                powerRating = "150W High-Lumen Bridgelux LED",
                voltage = "12.8V 80Ah Integrated LiFePO4",
                basePriceNgn = 165000,
                rating = 4.8f,
                reviewsCount = 112,
                badge = "IP66 Waterproof",
                inStock = true,
                specs = mapOf(
                    "Model" to "FL-AIO-150W-PRO",
                    "Luminous Flux" to "18,000 Lumens",
                    "Motion Sensor" to "Microwave Radar + Dusk-to-Dawn",
                    "Rainy Day Autonomy" to "3–4 Nights Continuous"
                ),
                description = "Heavy-duty die-cast aluminum solar streetlight for estates, commercial perimeters, and community roads.",
                sellerName = "Felicity Solar Official Direct",
                sellerLocation = "Alaba International & Festac, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_felicity",
                warrantyPeriod = "3 Years Replacement Warranty",
                stockCount = 45,
                installmentAllowed = true,
                maxInstallmentMonths = 3
            ),
            MarketplaceProduct(
                id = "prod_staubli_mc4_pack",
                title = "Stäubli Original MC4 IP68 1500V Solar Branch & Inline Connectors (10 Pairs)",
                category = ProductCategory.SOLAR_CONNECTORS,
                brand = "Stäubli Multi-Contact",
                modelNumber = "MC4-EVO2-1500V",
                powerRating = "45A Continuous Rated Current",
                voltage = "1500V DC TUV Certified",
                basePriceNgn = 28000,
                rating = 4.9f,
                reviewsCount = 87,
                badge = "Anti-Arc",
                inStock = true,
                specs = mapOf(
                    "Model" to "MC4-EVO2-1500V",
                    "Contact Material" to "Silver-Plated MULTILAM Copper",
                    "Ingress Protection" to "IP68 Submersible Mated",
                    "Cable Cross-Section" to "4mm² / 6mm² / 10mm²"
                ),
                description = "Genuine Stäubli MC4 connectors that eliminate roof connector melting and high-voltage DC arc faults.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "5 Years Connector Seal Warranty",
                stockCount = 120,
                installmentAllowed = false,
                maxInstallmentMonths = 1
            ),
            MarketplaceProduct(
                id = "prod_aluminum_roof_rails",
                title = "Anodized Aluminum Solar Roof Mounting Rail Kit (8-Panel Array Set)",
                category = ProductCategory.MOUNTING_STRUCTURES,
                brand = "Clenergy SolarMount",
                modelNumber = "PV-EZRACK-8P",
                powerRating = "Supports 8x 550W–650W Panels",
                voltage = "Grounded Bonding Frame",
                basePriceNgn = 195000,
                rating = 4.8f,
                reviewsCount = 53,
                badge = "Wind Rated",
                inStock = true,
                specs = mapOf(
                    "Model" to "PV-EZRACK-8P",
                    "Material" to "6005-T5 Anodized Aluminum + SUS304 Stainless Bolts",
                    "Wind Load" to "Up to 60 m/s Hurricane & Squall Rated",
                    "Included Hardware" to "4x 4.2m Rails, Mid/End Clamps, L-Feet & Earthing Clips"
                ),
                description = "Corrosion-resistant aluminum roof mounting structure kit for pitched Gerard stone-coated or long-span aluminum roofs.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "15 Years Structural Warranty",
                stockCount = 28,
                installmentAllowed = true,
                maxInstallmentMonths = 3
            ),
            MarketplaceProduct(
                id = "prod_complete_5kva_kit",
                title = "Complete 5kW Hybrid Solar System Kit (Deye 5kW + 5.12kWh LiFePO4 + 8x 550W Panels)",
                category = ProductCategory.COMPLETE_SOLAR_KITS,
                brand = "NEKES Turnkey Solar",
                modelNumber = "NK-KIT-5KW-LFP",
                powerRating = "5kW Inverter + 5.12kWh Battery + 4.4kWp PV",
                voltage = "48V Complete System Bundle",
                basePriceNgn = 4150000,
                rating = 5.0f,
                reviewsCount = 48,
                badge = "Complete Kit",
                inStock = true,
                specs = mapOf(
                    "Model" to "NK-KIT-5KW-LFP",
                    "Inverter" to "1x Deye 5kW Hybrid Inverter (SUN-5K-SG03LP1)",
                    "Battery" to "1x 5.12kWh 48V 100Ah LiFePO4 Wall Battery",
                    "Solar Array" to "8x Jinko 550W Mono-PERC Panels (4,400W)",
                    "Protection Box" to "Pre-wired Combiner Box, SPD, Breakers & 6mm² Cables"
                ),
                description = "Full residential & office solar bundle engineered to power 2x Inverter ACs, deep freezer, fridge, pumping machine, lighting, and home entertainment.",
                sellerName = "NEKES Solar Energy Store",
                sellerLocation = "Ikeja & Lekki Phase 1, Lagos",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_nekes",
                warrantyPeriod = "5 Years System & Component Warranty",
                stockCount = 7,
                installmentAllowed = true,
                maxInstallmentMonths = 6,
                solarfixInspectionStatus = SolarFixInspectionStatus.INSPECTED_PASSED,
                solarfixInspectionReportId = "SFLAB-2026-9040",
                solarfixInspectionDate = "Nov 2026"
            ),
            MarketplaceProduct(
                id = "prod_copper_earth_kit",
                title = "Pure Copper 4ft Earth Rod, Bentonite Salt & Brass Clamp Grounding Kit",
                category = ProductCategory.ACCESSORIES,
                brand = "Furse Earthing",
                modelNumber = "FE-EARTH-KIT-4FT",
                powerRating = "< 2 Ohm Target Earth Resistance",
                voltage = "AC/DC Chassis Grounding",
                basePriceNgn = 35000,
                rating = 4.9f,
                reviewsCount = 76,
                badge = "Safety Accessory",
                inStock = true,
                specs = mapOf(
                    "Model" to "FE-EARTH-KIT-4FT",
                    "Rod Material" to "99.9% Electrolytic Copper Bonded Steel",
                    "Earth Enhancement" to "10kg Conductive Bentonite Compound",
                    "Cable Included" to "10m 10mm² Green/Yellow Earth Cable"
                ),
                description = "Eliminates inverter chassis tingling shocks and gives surge protection devices (SPDs) a safe path to discharge lightning energy into the ground.",
                sellerName = "Apex Solar & Balance-of-System Store",
                sellerLocation = "Port Harcourt & Ikeja, Nigeria",
                isCertifiedSeller = true,
                sellerCertificationBadge = "VERIFIED SELLER",
                condition = "Brand New",
                storeId = "store_apex",
                warrantyPeriod = "5 Years Anti-Corrosion Warranty",
                stockCount = 64,
                installmentAllowed = false,
                maxInstallmentMonths = 1
            )
        )

        private val initialReviews = listOf(
            ProductReview(
                id = "rev_101",
                productId = "prod_deye_5kw",
                storeId = "store_nekes",
                customerName = "Arc. Olumide Soyinka",
                rating = 5,
                comment = "Bought this Deye 5kW hybrid inverter from NEKES Solar Energy Store using the 3-month CDcare-style installment plan. Original serial number verified on Deye Cloud, powers my two 1.5HP inverter ACs effortlessly!",
                date = "2 days ago",
                verifiedPurchase = true,
                sellerReply = "Thank you Arc. Olumide! Your 5-year warranty is registered in our system. Enjoy uninterrupted solar power!",
                sellerReplyDate = "Yesterday"
            ),
            ProductReview(
                id = "rev_102",
                productId = "prod_jinko_550w",
                storeId = "store_nekes",
                customerName = "Dr. Mrs. Ngozi Obi",
                rating = 5,
                comment = "Ordered 8 panels of Jinko 550W from NEKES Solar Energy Store. Delivered well-crated to Enugu with zero micro-cracks. Generating 3.9kW even at 10:30 AM!",
                date = "5 days ago",
                verifiedPurchase = true,
                sellerReply = null,
                sellerReplyDate = null
            ),
            ProductReview(
                id = "rev_103",
                productId = "prod_felicity_5kwh",
                storeId = "store_felicity",
                customerName = "Alhaji Garba Danjuma",
                rating = 5,
                comment = "Genuine 5.12kWh wall-mount LiFePO4 battery. CAN communication cable synced immediately with my hybrid inverter.",
                date = "1 week ago",
                verifiedPurchase = true,
                sellerReply = "We appreciate your patronage Alhaji Garba! Keep your DoD at 90% for 6,000+ cycles.",
                sellerReplyDate = "5 days ago"
            ),
            ProductReview(
                id = "rev_104",
                productId = "prod_pylontech_35",
                storeId = "store_nekes",
                customerName = "Engr. Kunle Adesina",
                rating = 5,
                comment = "Cleanest lithium battery cells I have tested with BMS diagnostic tool. Cell delta is 0.004V. Highly recommend NEKES Store.",
                date = "1 week ago",
                verifiedPurchase = true,
                sellerReply = null,
                sellerReplyDate = null
            )
        )

        private val initialAddresses = listOf(
            DeliveryAddress(
                id = "addr_default_1",
                label = "Home Residence",
                recipientName = "Kingsley Nekes",
                phone = "+234 803 700 1234",
                streetAddress = "Plot 14 Admiralty Way, Lekki Phase 1",
                city = "Lekki",
                state = "Lagos State",
                isDefault = true
            ),
            DeliveryAddress(
                id = "addr_site_2",
                label = "Project / Office Site",
                recipientName = "Kingsley Nekes",
                phone = "+234 803 700 1234",
                streetAddress = "22 Allen Avenue, Beside Zenith Bank",
                city = "Ikeja",
                state = "Lagos State",
                isDefault = false
            )
        )

        private val initialOrders = listOf(
            MarketplaceOrder(
                id = "SF-9042",
                customerName = "Arc. Olumide Soyinka",
                customerEmail = "olumide@solarclient.ng",
                items = listOf(
                    OrderItemSummary(
                        productId = "prod_deye_5kw",
                        productTitle = "Deye 5kW 48V Pure Sine Wave Hybrid Inverter (SUN-5K-SG03LP1)",
                        storeId = "store_nekes",
                        storeName = "NEKES Solar Energy Store",
                        quantity = 1,
                        unitPriceNgn = 1680000,
                        warrantyPeriod = "5 Years Manufacturer Warranty"
                    )
                ),
                primaryStoreId = "store_nekes",
                primaryStoreName = "NEKES Solar Energy Store",
                subtotalNgn = 1680000,
                deliveryFeeNgn = 15000,
                totalAmountNgn = 1695000,
                commissionPercent = 7.5,
                solarfixCommissionNgn = 126000,
                netSellerEarningsNgn = 1554000,
                paymentPlan = PaymentPlanType.INSTALLMENT_3M,
                paymentMethod = PaymentMethodType.SMART_INSTALLMENT,
                amountPaidSoFarNgn = 1130000,
                deliveryAddress = DeliveryAddress(
                    id = "addr_1",
                    label = "Home",
                    recipientName = "Arc. Olumide Soyinka",
                    phone = "+234 802 331 9982",
                    streetAddress = "18 Bourdillon Road, Ikoyi",
                    city = "Ikoyi",
                    state = "Lagos State",
                    isDefault = true
                ),
                status = OrderStatus.IN_TRANSIT,
                trackingNumber = "SF-TRK-884120",
                createdAt = "Yesterday, 2:15 PM",
                estimatedDelivery = "Today by 5:00 PM",
                statusTimeline = listOf(
                    "Order SF-9042 confirmed & protected by SolarFix 48H Escrow",
                    "CDcare-Style 3-Month Smart Plan Deposit verified",
                    "NEKES Solar Energy Store completed load-bank QC test & serial scan",
                    "Dispatched via SolarFix Express Logistics (SF-TRK-884120)"
                ),
                escrowReleased = false
            ),
            MarketplaceOrder(
                id = "SF-8995",
                customerName = "Dr. Mrs. Ngozi Obi",
                customerEmail = "ngozi.obi@hospital.ng",
                items = listOf(
                    OrderItemSummary(
                        productId = "prod_jinko_550w",
                        productTitle = "Jinko Solar 550W Tiger Pro Mono-Facial Panel",
                        storeId = "store_nekes",
                        storeName = "NEKES Solar Energy Store",
                        quantity = 6,
                        unitPriceNgn = 125000,
                        warrantyPeriod = "25 Years Linear Output Warranty"
                    )
                ),
                primaryStoreId = "store_nekes",
                primaryStoreName = "NEKES Solar Energy Store",
                subtotalNgn = 750000,
                deliveryFeeNgn = 15000,
                totalAmountNgn = 765000,
                commissionPercent = 7.5,
                solarfixCommissionNgn = 56250,
                netSellerEarningsNgn = 693750,
                paymentPlan = PaymentPlanType.OUTRIGHT,
                paymentMethod = PaymentMethodType.BANK_TRANSFER,
                amountPaidSoFarNgn = 765000,
                deliveryAddress = DeliveryAddress(
                    id = "addr_2",
                    label = "Clinic Roof Project",
                    recipientName = "Dr. Mrs. Ngozi Obi",
                    phone = "+234 803 555 0192",
                    streetAddress = "45 Bisalla Road, Independence Layout",
                    city = "Enugu",
                    state = "Enugu State",
                    isDefault = true
                ),
                status = OrderStatus.DELIVERED,
                trackingNumber = "SF-TRK-772019",
                createdAt = "5 days ago",
                estimatedDelivery = "Delivered",
                statusTimeline = listOf(
                    "Order SF-8995 confirmed & full outright payment received",
                    "Packed in reinforced timber pallet at NEKES Solar Energy Store",
                    "Delivered & signed by recipient in Enugu",
                    "48-Hour Inspection passed — ₦693,750 released to Seller Available Earnings"
                ),
                escrowReleased = true
            ),
            MarketplaceOrder(
                id = "SF-8960",
                customerName = "Alhaji Garba Danjuma",
                customerEmail = "garba@abujaestates.ng",
                items = listOf(
                    OrderItemSummary(
                        productId = "prod_felicity_5kwh",
                        productTitle = "Felicity Solar 5.12kWh 48V LiFePO4 Lithium Battery Wall-Mount",
                        storeId = "store_felicity",
                        storeName = "Felicity Solar Official Direct",
                        quantity = 1,
                        unitPriceNgn = 1450000,
                        warrantyPeriod = "5 Years Manufacturer Warranty"
                    )
                ),
                primaryStoreId = "store_felicity",
                primaryStoreName = "Felicity Solar Official Direct",
                subtotalNgn = 1450000,
                deliveryFeeNgn = 15000,
                totalAmountNgn = 1465000,
                commissionPercent = 7.5,
                solarfixCommissionNgn = 108750,
                netSellerEarningsNgn = 1341250,
                paymentPlan = PaymentPlanType.OUTRIGHT,
                paymentMethod = PaymentMethodType.CARD,
                amountPaidSoFarNgn = 1465000,
                deliveryAddress = DeliveryAddress(
                    id = "addr_3",
                    label = "Abuja Residence",
                    recipientName = "Alhaji Garba Danjuma",
                    phone = "+234 809 112 4400",
                    streetAddress = "12 Danube Street, Maitama",
                    city = "Abuja",
                    state = "FCT",
                    isDefault = true
                ),
                status = OrderStatus.PACKED,
                trackingNumber = "SF-TRK-650912",
                createdAt = "2 days ago",
                estimatedDelivery = "Tomorrow",
                statusTimeline = listOf(
                    "Order SF-8960 confirmed via Card Payment",
                    "BMS firmware verified & packed at Felicity Solar Official Direct"
                ),
                escrowReleased = false
            )
        )

        private val initialDisputes = listOf(
            ProductDispute(
                id = "DSP-4012",
                orderId = "SF-8902",
                productId = "prod_spd_surge",
                productTitle = "TOMZN 600V DC Surge Protective Device (SPD) Type 2",
                storeId = "store_apex",
                storeName = "Apex Solar & Balance-of-System Store",
                customerName = "Dr. Chinedu Eze (Port Harcourt)",
                issueType = DisputeIssueType.DAMAGED,
                description = "Outer DIN rail plastic clip on one SPD cartridge arrived cracked from transit impact. Requesting replacement unit or refund.",
                amountNgn = 42000L,
                status = DisputeStatus.OPEN,
                createdAt = "1 day ago"
            )
        )

        private val initialWithdrawals = listOf(
            WithdrawalRequest(
                id = "WTH-3091",
                storeId = "store_nekes",
                storeName = "NEKES Solar Energy Store",
                amountNgn = 1200000L,
                bankName = "Zenith Bank PLC",
                accountNumber = "1019482015",
                accountName = "NEKES SOLAR ENERGY LTD",
                status = WithdrawalStatus.PENDING,
                requestedAt = "2 hours ago"
            ),
            WithdrawalRequest(
                id = "WTH-2980",
                storeId = "store_felicity",
                storeName = "Felicity Solar Official Direct",
                amountNgn = 2500000L,
                bankName = "Guaranty Trust Bank (GTCO)",
                accountNumber = "0149827310",
                accountName = "FELICITY SOLAR DIRECT NG",
                status = WithdrawalStatus.APPROVED_PAID,
                requestedAt = "3 days ago",
                processedAt = "2 days ago"
            )
        )

        private val initialTransactions = listOf(
            MarketplaceTransaction(
                id = "TX-904201",
                type = TransactionType.ORDER_PAYMENT,
                title = "Order #SF-9042 Escrow Credit",
                subtitle = "Deye 5kW Hybrid Inverter • NEKES Solar Energy Store",
                amountNgn = 1680000L,
                storeId = "store_nekes",
                storeName = "NEKES Solar Energy Store",
                orderId = "SF-9042",
                timestamp = "Yesterday",
                isCreditToSeller = true
            ),
            MarketplaceTransaction(
                id = "TX-904202",
                type = TransactionType.COMMISSION_DEDUCTION,
                title = "SolarFix Platform Commission (7.5%)",
                subtitle = "Deducted from Order #SF-9042",
                amountNgn = 126000L,
                storeId = "store_nekes",
                storeName = "NEKES Solar Energy Store",
                orderId = "SF-9042",
                timestamp = "Yesterday",
                isCreditToSeller = false
            ),
            MarketplaceTransaction(
                id = "TX-899501",
                type = TransactionType.ESCROW_RELEASE,
                title = "Escrow Released to Available Balance",
                subtitle = "Order #SF-8995 (6x Jinko 550W Panels) Delivered",
                amountNgn = 693750L,
                storeId = "store_nekes",
                storeName = "NEKES Solar Energy Store",
                orderId = "SF-8995",
                timestamp = "3 days ago",
                isCreditToSeller = true
            ),
            MarketplaceTransaction(
                id = "TX-298001",
                type = TransactionType.SELLER_WITHDRAWAL,
                title = "Bank Payout to GTCO (0149827310)",
                subtitle = "Felicity Solar Official Direct • Approved by Admin",
                amountNgn = 2500000L,
                storeId = "store_felicity",
                storeName = "Felicity Solar Official Direct",
                timestamp = "2 days ago",
                isCreditToSeller = false
            )
        )

        private val initialMaintenanceTasks = listOf(
            MaintenanceTask(
                id = "maint_panel_cleaning",
                title = "Clean Solar Panel Glass Surfaces",
                frequency = "Monthly (or Bi-weekly in Harmattan)",
                description = "Dust, bird droppings, and diesel generator soot block sunlight and cause localized hot-spots that degrade cells over time.",
                safetyTips = "Wash only early in the morning before sun heat causes thermal glass shock. Use soft clean water and a non-abrasive sponge. Never use harsh abrasive detergents or walk on panels.",
                isCompleted = false
            ),
            MaintenanceTask(
                id = "maint_terminal_check",
                title = "Inspect Battery Terminals & Cable Lugs",
                frequency = "Every 3 Months",
                description = "Check for loose battery bolt connections, heating discolouration, or greenish copper oxidation that causes dangerous high resistance.",
                safetyTips = "Use an insulated wrench (wrap non-insulated tools in electrical tape). Coat lead acid terminals with petroleum jelly after tightening.",
                isCompleted = true,
                lastDoneDate = "15 Oct 2026"
            ),
            MaintenanceTask(
                id = "maint_inverter_filter",
                title = "Clear Inverter Dust Filters & Air Grilles",
                frequency = "Every 2 Months",
                description = "Inverter internal fans draw in air cooling. Dust mats choke cooling airflow, forcing the inverter into Error 02 (Over Temperature) derating.",
                safetyTips = "Switch off inverter DC breaker and AC breaker before dusting. Use a gentle dry brush or can of compressed air.",
                isCompleted = false
            ),
            MaintenanceTask(
                id = "maint_earth_check",
                title = "Moisten Earthing Pit & Test Surge Protectors",
                frequency = "Every 6 Months",
                description = "During dry seasons, ground soil dries out, increasing earthing resistance and leaving inverter vulnerable to lightning damage.",
                safetyTips = "Inspect the green indicator flag on your DC Surge Protection Device (SPD). If the window shows red, replace the cartridge immediately.",
                isCompleted = false
            ),
            MaintenanceTask(
                id = "maint_tubular_water",
                title = "Top-up Tubular Battery Distilled Water",
                frequency = "Monthly (For Tubular Battery Owners)",
                description = "Exposed lead plates sulfure and permanently lose capacity within days if the electrolyte level falls below the red indicator ring.",
                safetyTips = "Use ONLY pure distilled/deionized water. Never add raw tap water or battery acid!",
                isCompleted = false
            )
        )

        private val _sharedStores = MutableStateFlow(initialStores)
        private val _sharedProducts = MutableStateFlow(initialProducts)
        private val _sharedReviews = MutableStateFlow(initialReviews)
        private val _sharedAddresses = MutableStateFlow(initialAddresses)
        private val _sharedOrders = MutableStateFlow(initialOrders)
        private val _sharedDisputes = MutableStateFlow(initialDisputes)
        private val _sharedWithdrawals = MutableStateFlow(initialWithdrawals)
        private val _sharedTransactions = MutableStateFlow(initialTransactions)
        private val _sharedSettings = MutableStateFlow(MarketplaceSettings())
        private val _sharedAdminAuditLogs = MutableStateFlow<List<AdminActionAuditRecord>>(emptyList())
        private val _sharedCartItems = MutableStateFlow<List<CartItem>>(emptyList())
        private val _sharedSelectedCurrency = MutableStateFlow(AvailableCurrencies[0])
        private val _sharedPrePurchaseInquiries = MutableStateFlow<List<PrePurchaseVerificationInquiry>>(emptyList())
        private val _sharedPaymentGateway = PaystackPaymentGatewayManager()
        private val _sharedMaintenanceTasks = MutableStateFlow(initialMaintenanceTasks)
        private val _sharedUserProfile = MutableStateFlow(
            SolarSystemProfile(
                systemSizeKva = 5.0,
                batteryType = "Lithium LiFePO4 (48V)",
                batteryCapacityKwh = 5.12,
                solarPanelWatts = 3300,
                installerName = "NEKES Solar Energy Store",
                installYear = 2024
            )
        )
    }
}
