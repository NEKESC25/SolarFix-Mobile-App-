package com.example.data.model

import android.graphics.Bitmap

enum class InverterBrand(val displayName: String) {
    GROWATT("Growatt"),
    FELICITY("Felicity Solar"),
    MUST("Must Power"),
    LUMINOUS("Luminous"),
    DEYE("Deye / Sunsynk"),
    SRNE("SRNE Solar")
}

enum class ErrorSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class InverterErrorCode(
    val brand: InverterBrand,
    val code: String,
    val title: String,
    val probableCause: String,
    val stepByStepFix: List<String>,
    val severity: ErrorSeverity,
    val safetyWarning: String? = null
)

data class SymptomDiagnostic(
    val id: String,
    val title: String,
    val iconName: String,
    val description: String,
    val quickDiagnosis: String,
    val steps: List<String>,
    val professionalRequired: Boolean = false
)

enum class ProductCategory(val label: String) {
    ALL("All Products"),
    PANELS("Solar Panels"),
    INVERTERS("Inverters"),
    BATTERIES("Lithium Batteries"),
    TUBULAR_BATTERIES("Tubular Batteries"),
    CONTROLLERS("Charge Controllers"),
    SOLAR_GENERATORS("Solar Generators"),
    SOLAR_STREETLIGHTS("Solar Streetlights"),
    SOLAR_CABLES_WIRES("Solar Cables & Wires"),
    SOLAR_CONNECTORS("Solar Connectors"),
    CIRCUIT_BREAKERS_PROTECTION("Circuit Breakers & Protection Devices"),
    MOUNTING_STRUCTURES("Mounting Structures"),
    COMPLETE_SOLAR_KITS("Complete Solar Kits"),
    ACCESSORIES("Other Solar Accessories");

    companion object {
        val LITHIUM_BATTERIES = BATTERIES
        val CHARGE_CONTROLLERS = CONTROLLERS
        val OTHER_ACCESSORIES = ACCESSORIES
    }
}

enum class StoreVerificationStatus(val label: String) {
    APPROVED("VERIFIED SELLER"),
    PENDING("Pending Verification"),
    MORE_INFO_REQUIRED("Additional Info Requested"),
    SUSPENDED("Suspended"),
    REJECTED("Rejected")
}

enum class ProductModerationStatus(val label: String) {
    APPROVED("Approved"),
    PENDING_REVIEW("Pending Review"),
    SUSPICIOUS_HIGH_RISK("Suspicious / High-Risk"),
    EVIDENCE_REQUESTED("Additional Evidence Requested"),
    REMOVED_COUNTERFEIT("Removed (Counterfeit / Misleading)"),
    FLAGGED("Flagged / Hidden")
}

/**
 * Tracks whether a specific product listing has undergone an actual independent physical/lab
 * inspection by SolarFix engineers. Distinct from Seller Verification!
 */
enum class SolarFixInspectionStatus(val label: String) {
    NOT_INSPECTED("Not Inspected by SolarFix"),
    INSPECTION_PENDING("SolarFix Lab Inspection Scheduled"),
    INSPECTED_PASSED("SolarFix Physical Inspection Passed"),
    INSPECTED_FAILED("Failed Physical Inspection (Counterfeit/Substandard)")
}

/**
 * Builds a clean URL-free slug for internal SolarFix navigation routes.
 * Example: "NEKES Solar Energy Store" -> "nekes-solar-energy-store"
 */
fun buildStoreRouteSlug(storeName: String, fallbackId: String): String {
    val slug = storeName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return slug.ifEmpty { fallbackId.lowercase() }
}

/**
 * Public-safe store representation for Customer views inside SolarFix.
 * Strictly excludes sensitive KYC identification documents, premises verification files, and bank details,
 * while providing all 12 public storefront attributes and internal in-app routing metadata.
 */
data class PublicStoreProfile(
    val id: String, // Internal store identifier (e.g., "store_nekes")
    val inAppStoreRoute: String, // e.g., "marketplace/sellers/nekes-solar-energy-store"
    val inAppBreadcrumb: String, // e.g., "Marketplace → Sellers → NEKES Solar Energy Store"
    val storeName: String, // Unique store name
    val city: String,
    val state: String,
    val cityStateLocation: String, // Business location
    val publicContactPhone: String, // Contact information (Phone)
    val publicContactEmail: String, // Contact information (Email)
    val tagline: String,
    val description: String, // Business description
    val warrantyPolicy: String,
    val rating: Float, // Customer rating
    val reviewsCount: Int, // Customer reviews count
    val successfulSalesCount: Int, // Number of successful sales
    val isVerifiedSeller: Boolean,
    val verifiedBadgeText: String, // Verification badge ("VERIFIED SELLER")
    val accreditationBadge: String,
    val storeOpeningDate: String, // Store opening date
    val joinedDate: String,
    val logoBitmap: Bitmap? = null, // Store logo
    val coverImageBitmap: Bitmap? = null, // Store cover image
    val storeBrandColorHex: Long = 0xFF0F766E
)

data class SellerStore(
    val id: String,
    val storeName: String,
    val ownerUid: String,
    val ownerName: String,
    val ownerEmail: String,
    val businessRegNumber: String,
    val taxIdNumber: String = "TIN-VERIFIED",
    val physicalAddress: String = "Plot 14 Industrial Estate",
    val city: String = "Ikeja",
    val state: String = "Lagos State",
    val location: String,
    val phone: String,
    val tagline: String,
    val description: String,
    val warrantyPolicy: String = "Official OEM Warranty + SolarFix 48-Hour Escrow Buyer Protection",
    val rating: Float = 4.9f,
    val reviewsCount: Int = 48,
    val successfulSalesCount: Int = 128,
    val status: StoreVerificationStatus = StoreVerificationStatus.PENDING,
    val accreditationBadge: String = "VERIFIED SELLER • NEMSA & COREN",
    val storeOpeningDate: String = "15 Jan 2025",
    val joinedDate: String = "Jan 2025",
    val totalGrossSalesNgn: Long = 0L,
    val pendingEscrowNgn: Long = 0L,
    val availableEarningsNgn: Long = 0L,
    val totalWithdrawnNgn: Long = 0L,
    val totalCommissionPaidNgn: Long = 0L,
    val customCommissionPercent: Double? = null,
    val logoBitmap: Bitmap? = null,
    val coverImageBitmap: Bitmap? = null,
    val storeBrandColorHex: Long = 0xFF0F766E,
    // --- Sensitive KYC & Banking Fields (Restricted to Seller & Admin ONLY) ---
    val idDocumentType: String = "National ID (NIN) / International Passport",
    val idDocumentNumber: String = "NIN-VERIFIED-8841",
    val idDocumentBitmap: Bitmap? = null,
    val idDocumentProvided: Boolean = true,
    val premisesPhotoBitmap: Bitmap? = null,
    val premisesPhotoProvided: Boolean = true,
    val bankAccountName: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val adminVerificationNote: String = "Verified by SolarFix Compliance Administration."
) {
    val currentCommissionPercent: Double
        get() = customCommissionPercent ?: 2.0

    val sellerProceedsPercent: Double
        get() = (100.0 - currentCommissionPercent).coerceAtLeast(0.0)

    val totalNetEarningsNgn: Long
        get() = availableEarningsNgn + pendingEscrowNgn + totalWithdrawnNgn

    val isApprovedSeller: Boolean
        get() = status == StoreVerificationStatus.APPROVED

    val inAppStoreRoute: String
        get() = "marketplace/sellers/${buildStoreRouteSlug(storeName, id)}"

    val inAppBreadcrumb: String
        get() = "Marketplace → Sellers → $storeName"

    fun toPublicProfile(): PublicStoreProfile {
        return PublicStoreProfile(
            id = id,
            inAppStoreRoute = inAppStoreRoute,
            inAppBreadcrumb = inAppBreadcrumb,
            storeName = storeName,
            city = city,
            state = state,
            cityStateLocation = location.ifBlank { "$city, $state" },
            publicContactPhone = phone,
            publicContactEmail = ownerEmail,
            tagline = tagline,
            description = description,
            warrantyPolicy = warrantyPolicy,
            rating = rating,
            reviewsCount = reviewsCount,
            successfulSalesCount = successfulSalesCount,
            isVerifiedSeller = (status == StoreVerificationStatus.APPROVED),
            verifiedBadgeText = if (status == StoreVerificationStatus.APPROVED) "VERIFIED SELLER" else status.label,
            accreditationBadge = if (status == StoreVerificationStatus.APPROVED) accreditationBadge else status.label,
            storeOpeningDate = storeOpeningDate.ifBlank { joinedDate },
            joinedDate = joinedDate,
            logoBitmap = logoBitmap,
            coverImageBitmap = coverImageBitmap,
            storeBrandColorHex = storeBrandColorHex
        )
    }
}

data class ProductReview(
    val id: String,
    val productId: String,
    val storeId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val verifiedPurchase: Boolean = true,
    val sellerReply: String? = null,
    val sellerReplyDate: String? = null
)

data class MarketplaceProduct(
    val id: String,
    val title: String, // 1. Product name
    val category: ProductCategory,
    val brand: String, // 2. Brand
    val modelNumber: String = "", // 3. Model
    val powerRating: String,
    val voltage: String,
    val basePriceNgn: Long, // 7. Price in Nigerian Naira
    val rating: Float,
    val reviewsCount: Int,
    val badge: String? = null,
    val inStock: Boolean = true,
    val specs: Map<String, String>, // 6. Technical specifications
    val description: String, // 5. Description
    val sellerName: String = "NEKES Solar Energy Store", // 11. Seller information
    val sellerLocation: String = "Lagos, Nigeria",
    val isCertifiedSeller: Boolean = true, // 12. Seller verification status
    val sellerCertificationBadge: String = "VERIFIED SELLER",
    val condition: String = "Brand New", // 10. Product condition (Brand New or Used)
    val photoBitmap: Bitmap? = null, // 4. Product photographs
    val photoBitmaps: List<Bitmap> = emptyList(),
    val videoUrlOrUri: String? = null,
    val videoDescription: String = "360° Load Test & Serial Inspection Video",
    val deliveryAvailability: String = "Available Nationwide (24-48H Dispatch)", // 13. Delivery availability
    val deliveryOptions: List<String> = listOf(
        "Doorstep Delivery (24-48H)",
        "Warehouse Self-Pickup",
        "Nationwide Heavy Solar Freight"
    ),
    val returnPolicy: String = "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy", // 14. Return policy
    val isActive: Boolean = true,
    val storeId: String = "store_nekes",
    val warrantyPeriod: String = "5 Years Manufacturer Warranty", // 9. Warranty information
    val stockCount: Int = 18, // 8. Available quantity
    val installmentAllowed: Boolean = true,
    val maxInstallmentMonths: Int = 6,
    val moderationStatus: ProductModerationStatus = ProductModerationStatus.APPROVED,
    val photoAngles: List<String> = listOf("Front View", "Spec Label & Serial", "Terminal & Ports"),
    // Authenticity & Independent Inspection fields
    val sellerAuthenticityConfirmed: Boolean = true,
    val authenticityEvidenceNote: String = "Seller confirmed genuine OEM hardware with serial & batch verification",
    val adminModerationNote: String = "Listing compliant with SolarFix authenticity standards.",
    val solarfixInspectionStatus: SolarFixInspectionStatus = SolarFixInspectionStatus.NOT_INSPECTED,
    val solarfixInspectionReportId: String? = null,
    val solarfixInspectionDate: String? = null
) {
    val allPhotos: List<Bitmap>
        get() = when {
            photoBitmaps.isNotEmpty() -> photoBitmaps
            photoBitmap != null -> listOf(photoBitmap)
            else -> emptyList()
        }

    val effectiveModelNumber: String
        get() = when {
            modelNumber.isNotBlank() -> modelNumber
            !specs["Model"].isNullOrBlank() -> specs["Model"]!!
            else -> "${brand.uppercase().take(4)}-${powerRating.uppercase().replace(" ", "")}"
        }

    val formattedPriceNgn: String
        get() = String.format("₦%,d", basePriceNgn)

    val normalizedCondition: String
        get() = if (condition.lowercase().contains("used") || condition.lowercase().contains("refurb")) {
            "Used"
        } else {
            "Brand New"
        }

    val sellerVerificationStatusLabel: String
        get() = if (isCertifiedSeller) "VERIFIED SELLER" else "Pending Seller Verification"

    /**
     * Strict guard: Returns true ONLY if an actual independent physical/lab inspection by SolarFix
     * has taken place and passed with a recorded inspection report ID.
     * Seller verification alone NEVER triggers a SolarFix inspection badge.
     */
    val hasActualSolarFixInspection: Boolean
        get() = solarfixInspectionStatus == SolarFixInspectionStatus.INSPECTED_PASSED &&
            !solarfixInspectionReportId.isNullOrBlank()
}

data class CartItem(
    val product: MarketplaceProduct,
    val quantity: Int,
    val selectedPaymentPlan: PaymentPlanType = PaymentPlanType.OUTRIGHT
)

enum class PaymentPlanType(val label: String, val months: Int) {
    OUTRIGHT("Outright Payment (100%)", 1),
    INSTALLMENT_3M("CDcare-Style 3 Months (0% Interest)", 3),
    INSTALLMENT_6M("CDcare-Style 6 Months Smart Plan", 6)
}

enum class PaymentMethodType(val label: String, val subtitle: String) {
    CARD("Debit / Credit Card", "Instant Visa, Mastercard & Verve via Paystack/Flutterwave"),
    BANK_TRANSFER("Instant Bank Transfer", "Dedicated SolarFix Escrow Virtual Account"),
    USSD("Bank USSD Code", "Dial *737#, *901#, *966# or *894# inside SolarFix"),
    SMART_INSTALLMENT("SolarFix Smart Installment Auto-Debit", "40% Initial Deposit + Monthly Auto-Deduct")
}

data class DeliveryAddress(
    val id: String,
    val label: String, // e.g., "Home", "Office / Site"
    val recipientName: String,
    val phone: String,
    val streetAddress: String,
    val city: String,
    val state: String,
    val isDefault: Boolean = false,
    val userUid: String = "",
    val userEmail: String = ""
) {
    fun formattedLine(): String = "$streetAddress, $city, $state ($phone)"
}

enum class OrderStatus(val label: String, val stepIndex: Int) {
    CONFIRMED("Order Confirmed", 1),
    PROCESSING("Processing at Store", 2),
    PACKED("Packed & QC Tested", 3),
    IN_TRANSIT("In Transit", 4),
    OUT_FOR_DELIVERY("Out for Delivery", 5),
    DELIVERED("Delivered & Verified", 6),
    DISPUTED("Dispute Under Review", 3),
    REFUNDED("Refunded to Customer", 0),
    REJECTED_BY_SELLER("Rejected by Seller (Auto-Refunded)", 0)
}

data class OrderItemSummary(
    val productId: String,
    val productTitle: String,
    val storeId: String,
    val storeName: String,
    val quantity: Int,
    val unitPriceNgn: Long,
    val warrantyPeriod: String
)

enum class PriceRangeFilter(val label: String, val minNgn: Long, val maxNgn: Long) {
    ALL("All Prices", 0L, Long.MAX_VALUE),
    UNDER_100K("Under ₦100k", 0L, 100_000L),
    RANGE_100K_TO_500K("₦100k – ₦500k", 100_000L, 500_000L),
    RANGE_500K_TO_1_5M("₦500k – ₦1.5M", 500_000L, 1_500_000L),
    ABOVE_1_5M("Above ₦1.5M", 1_500_000L, Long.MAX_VALUE)
}

enum class ProductSortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW_TO_HIGH("Price: Low → High"),
    PRICE_HIGH_TO_LOW("Price: High → Low"),
    HIGHEST_RATED("Highest Rated")
}

data class SellerCartBreakdown(
    val storeId: String,
    val storeName: String,
    val storeLocation: String,
    val isVerifiedSeller: Boolean,
    val items: List<CartItem>,
    val sellerSubtotalNgn: Long,
    val sellerDeliveryFeeNgn: Long,
    val paymentGatewayFeeNgn: Long = 0L,
    val commissionRatePercent: Double,
    val solarfixCommissionNgn: Long,
    val netSellerPayoutNgn: Long,
    val sellerOrderTotalNgn: Long
)

data class CartSummaryBreakdown(
    val sellerBreakdowns: List<SellerCartBreakdown>,
    val totalItemsCount: Int,
    val totalSellersCount: Int,
    val subtotalNgn: Long,
    val totalDeliveryCostNgn: Long,
    val totalPaymentGatewayFeeNgn: Long = 0L,
    val totalSolarfixCommissionNgn: Long,
    val totalNetSellerPayoutNgn: Long,
    val finalOrderTotalNgn: Long
)

data class SellerFulfilmentRecord(
    val fulfilmentOrderId: String,
    val storeId: String,
    val storeName: String,
    val trackingNumber: String,
    val status: OrderStatus,
    val items: List<OrderItemSummary>,
    val subtotalNgn: Long,
    val deliveryFeeNgn: Long,
    val paymentGatewayFeeNgn: Long = 0L,
    val commissionRatePercent: Double,
    val solarfixCommissionNgn: Long,
    val netSellerEarningsNgn: Long,
    val totalSellerOrderAmountNgn: Long
)

data class MarketplaceOrder(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val customerUid: String = "",
    val items: List<OrderItemSummary>,
    val primaryStoreId: String,
    val primaryStoreName: String,
    val subtotalNgn: Long,
    val deliveryFeeNgn: Long,
    val paymentGatewayFeeNgn: Long = 0L,
    val totalAmountNgn: Long,
    val commissionPercent: Double,
    val solarfixCommissionNgn: Long,
    val netSellerEarningsNgn: Long,
    val paymentPlan: PaymentPlanType,
    val paymentMethod: PaymentMethodType,
    val amountPaidSoFarNgn: Long,
    val deliveryAddress: DeliveryAddress,
    val status: OrderStatus,
    val trackingNumber: String,
    val createdAt: String,
    val estimatedDelivery: String,
    val statusTimeline: List<String> = emptyList(),
    val escrowReleased: Boolean = false,
    val sellerAccepted: Boolean = false,
    val sellerRejectionReason: String? = null,
    val orderMonthLabel: String = "Nov 2026",
    val checkoutGroupId: String? = null,
    val sellerFulfilments: List<SellerFulfilmentRecord> = emptyList(),
    val paymentReference: String? = null,
    val gatewayVerificationId: String? = null,
    val isGatewayVerified: Boolean = true
)

enum class DisputeIssueType(val label: String) {
    DAMAGED("Damaged Solar Equipment on Arrival"),
    MISSING("Missing Parts / Incomplete Package"),
    INCORRECT("Incorrect Model or Specification")
}

enum class DisputeStatus(val label: String) {
    OPEN("Open - Under Admin Review"),
    RESOLVED_REFUND("Resolved - Full Customer Refund Issued"),
    RESOLVED_REPLACEMENT("Resolved - Replacement Dispatched"),
    REJECTED("Closed - Claim Unsubstantiated")
}

data class ProductDispute(
    val id: String,
    val orderId: String,
    val productId: String,
    val productTitle: String,
    val storeId: String,
    val storeName: String,
    val customerName: String,
    val issueType: DisputeIssueType,
    val description: String,
    val amountNgn: Long,
    val status: DisputeStatus = DisputeStatus.OPEN,
    val adminResolutionNote: String? = null,
    val createdAt: String = "Today"
)

enum class WithdrawalStatus(val label: String) {
    PENDING("Pending Admin Approval"),
    APPROVED_PAID("Disbursed to Bank"),
    REJECTED("Rejected")
}

data class WithdrawalRequest(
    val id: String,
    val storeId: String,
    val storeName: String,
    val amountNgn: Long,
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val requestedAt: String = "Today",
    val processedAt: String? = null
)

enum class TransactionType(val label: String) {
    ORDER_PAYMENT("Customer Order Payment"),
    COMMISSION_DEDUCTION("SolarFix Platform Commission"),
    ESCROW_RELEASE("Escrow Released to Seller"),
    SELLER_WITHDRAWAL("Seller Bank Withdrawal"),
    CUSTOMER_REFUND("Customer Dispute Refund")
}

data class MarketplaceTransaction(
    val id: String,
    val type: TransactionType,
    val title: String,
    val subtitle: String,
    val amountNgn: Long,
    val storeId: String,
    val storeName: String,
    val orderId: String? = null,
    val timestamp: String,
    val isCreditToSeller: Boolean
)

data class MarketplaceSettings(
    val defaultCommissionPercent: Double = 2.0,
    val escrowHoldHours: Int = 48,
    val minWithdrawalNgn: Long = 50000L,
    val allowInstallmentPlans: Boolean = true,
    val requireAdminSellerApproval: Boolean = true,
    val buyerProtectionEnabled: Boolean = true
)

data class FinancialReportSummary(
    val generatedAt: String,
    val totalOrdersCount: Int,
    val grossMerchandiseValueNgn: Long,
    val totalSolarfixCommissionNgn: Long,
    val totalPendingEscrowNgn: Long,
    val totalSellerAvailableEarningsNgn: Long,
    val totalWithdrawalsPaidNgn: Long,
    val totalRefundedNgn: Long,
    val activeVerifiedStoresCount: Int,
    val openDisputesCount: Int
)

data class MonthlySalesMetric(
    val monthLabel: String, // e.g., "Jun 2026", "Jul 2026", "Aug 2026", "Sep 2026", "Oct 2026", "Nov 2026"
    val shortMonth: String, // e.g., "Jun", "Jul", "Aug", "Sep", "Oct", "Nov"
    val ordersCount: Int,
    val unitsSold: Int,
    val grossRevenueNgn: Long,
    val solarfixCommissionNgn: Long,
    val netEarningsNgn: Long
)

data class SellerSalesAnalytics(
    val storeId: String,
    val storeName: String,
    val totalSalesCount: Int, // Total sales (units/completed sales)
    val totalRevenueNgn: Long, // Total gross revenue
    val netRevenueAfterCommissionNgn: Long, // Net seller earnings
    val totalOrdersCount: Int, // Number of orders
    val completedOrdersCount: Int, // Completed orders (DELIVERED)
    val pendingOrdersCount: Int, // Pending orders (CONFIRMED, PROCESSING, PACKED, IN_TRANSIT, OUT_FOR_DELIVERY)
    val rejectedOrRefundedOrdersCount: Int,
    val availableBalanceNgn: Long, // Available balance ready for withdrawal
    val pendingPaymentsEscrowNgn: Long, // Pending payments held in escrow
    val withdrawnEarningsNgn: Long = 0L, // Total withdrawn earnings
    val totalEarningsNgn: Long = 0L, // Total earnings = available + pending + withdrawn
    val solarfixCommissionsNgn: Long, // Total SolarFix commission deductions
    val commissionRatePercent: Double,
    val sellerProceedsPercent: Double = 98.0,
    val activeProductsCount: Int,
    val deactivatedProductsCount: Int,
    val outOfStockProductsCount: Int,
    val monthlyPerformance: List<MonthlySalesMetric>
)

enum class AdminActionType {
    STORE_VERIFICATION_STATUS_CHANGE,
    CUSTOM_COMMISSION_UPDATE,
    MARKETPLACE_SETTINGS_UPDATE,
    PRODUCT_MODERATION_STATUS_CHANGE,
    PHYSICAL_INSPECTION_UPDATE,
    COUNTERFEIT_PRODUCT_REMOVED,
    DISPUTE_RESOLUTION,
    WITHDRAWAL_APPROVAL,
    WITHDRAWAL_REJECTION,
    MANUAL_ESCROW_RELEASE,
    PRIVILEGE_MODIFICATION_ATTEMPT_BLOCKED
}

/**
 * Immutable audit record capturing all administrative actions for compliance and accountability.
 */
data class AdminActionAuditRecord(
    val auditId: String = "AUD-${java.util.UUID.randomUUID().toString().take(12)}",
    val adminEmail: String,
    val adminUid: String = "",
    val actionType: AdminActionType,
    val targetEntityId: String,
    val description: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val stepUpMfaVerified: Boolean = true
)


data class Currency(
    val code: String,
    val symbol: String,
    val exchangeRateToNgn: Double, // 1 Unit of Currency = X NGN
    val name: String
) {
    fun formatPrice(priceInNgn: Long): String {
        val converted = priceInNgn / exchangeRateToNgn
        return when (code) {
            "NGN" -> String.format("₦%,d", priceInNgn)
            "USD" -> String.format("$%,.2f", converted)
            "KES" -> String.format("KSh %,.0f", converted)
            "GHS" -> String.format("GH₵ %,.2f", converted)
            else -> String.format("%s %,.2f", symbol, converted)
        }
    }
}

val AvailableCurrencies = listOf(
    Currency("NGN", "₦", 1.0, "Nigerian Naira (NGN)"),
    Currency("USD", "$", 1520.0, "US Dollar (USD)"),
    Currency("KES", "KSh", 11.8, "Kenyan Shilling (KES)"),
    Currency("GHS", "GH₵", 98.0, "Ghanaian Cedi (GHS)")
)

data class HourlyYield(
    val hourLabel: String,
    val solarWatts: Float,
    val loadWatts: Float
)

data class EnergyMetrics(
    val solarWattsNow: Int,
    val batterySocPercent: Int,
    val batteryVoltage: Double,
    val loadWattsNow: Int,
    val isGridAvailable: Boolean,
    val dailyPvKwh: Double,
    val generatorFuelSavedLitres: Double,
    val estimatedHoursLeftAtCurrentLoad: Double,
    val hourlyHistory: List<HourlyYield>
)

data class SolarSystemProfile(
    val systemSizeKva: Double = 5.0,
    val batteryType: String = "Lithium LiFePO4 (48V)",
    val batteryCapacityKwh: Double = 5.12,
    val solarPanelWatts: Int = 3300, // 6x 550W
    val installerName: String = "Verified Solarfix Partner",
    val installYear: Int = 2024
)

data class MaintenanceTask(
    val id: String,
    val title: String,
    val frequency: String,
    val description: String,
    val safetyTips: String,
    val isCompleted: Boolean = false,
    val lastDoneDate: String? = null
)

data class TechnicianContact(
    val id: String,
    val name: String,
    val businessName: String,
    val location: String,
    val phone: String,
    val rating: Float,
    val completedJobs: Int,
    val badges: List<String>,
    val verified: Boolean = true
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val timestamp: String,
    val isStreaming: Boolean = false,
    val quickSuggestions: List<String> = emptyList(),
    val attachedBitmap: Bitmap? = null
)

data class PrePurchaseVerificationInquiry(
    val id: String,
    val productId: String,
    val productTitle: String,
    val storeId: String,
    val storeName: String,
    val requestType: String,
    val customerNotes: String,
    val customerContact: String,
    val timestamp: String,
    val status: String = "PENDING_SELLER_CONFIRMATION"
)
