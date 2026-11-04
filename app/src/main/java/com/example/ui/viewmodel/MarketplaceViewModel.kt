package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.data.model.*
import com.example.data.payment.*
import com.example.data.repository.SolarRepository
import com.example.util.SecuritySanitizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class MarketplacePortalMode(val label: String) {
    CUSTOMER_SHOP("Shop"),
    SELLER_REGISTRATION("Become a Seller"),
    SELLER_DASHBOARD("Seller Hub")
}

enum class CustomerSubScreen(val title: String) {
    CATALOG("Solar Equipment Marketplace"),
    SELLERS_DIRECTORY("Verified Solar Sellers"),
    STOREFRONT("Seller Storefront"),
    ORDERS_AND_TRACKING("Orders & Tracking"),
    WISHLIST("Saved Wishlist"),
    COMPARE("Compare Solar Gear"),
    ADDRESS_BOOK("Delivery Addresses"),
    PAYMENT_AUDIT("Payment Reconciliation & Escrow")
}

class MarketplaceViewModel @JvmOverloads constructor(
    private val repository: SolarRepository = SolarRepository()
) : ViewModel() {

    val products: StateFlow<List<MarketplaceProduct>> = repository.productsFlow
    val stores: StateFlow<List<SellerStore>> = repository.storesFlow
    val reviews: StateFlow<List<ProductReview>> = repository.reviewsFlow
    val addresses: StateFlow<List<DeliveryAddress>> = repository.addressesFlow
    val orders: StateFlow<List<MarketplaceOrder>> = repository.ordersFlow
    val disputes: StateFlow<List<ProductDispute>> = repository.disputesFlow
    val withdrawals: StateFlow<List<WithdrawalRequest>> = repository.withdrawalsFlow
    val transactions: StateFlow<List<MarketplaceTransaction>> = repository.transactionsFlow
    val settings: StateFlow<MarketplaceSettings> = repository.settingsFlow
    val adminAuditLogs: StateFlow<List<AdminActionAuditRecord>> = repository.adminAuditLogFlow

    private val _portalMode = MutableStateFlow(MarketplacePortalMode.CUSTOMER_SHOP)
    val portalMode: StateFlow<MarketplacePortalMode> = _portalMode.asStateFlow()

    private val _customerSubScreen = MutableStateFlow(CustomerSubScreen.CATALOG)
    val customerSubScreen: StateFlow<CustomerSubScreen> = _customerSubScreen.asStateFlow()

    private val _selectedStoreId = MutableStateFlow<String?>(null)
    val selectedStoreId: StateFlow<String?> = _selectedStoreId.asStateFlow()

    private val _activeSellerStoreId = MutableStateFlow("store_nekes")
    val activeSellerStoreId: StateFlow<String> = _activeSellerStoreId.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCertifiedOnly = MutableStateFlow(false)
    val filterCertifiedOnly: StateFlow<Boolean> = _filterCertifiedOnly.asStateFlow()

    private val _selectedPriceRange = MutableStateFlow(PriceRangeFilter.ALL)
    val selectedPriceRange: StateFlow<PriceRangeFilter> = _selectedPriceRange.asStateFlow()

    private val _customMinPriceNgn = MutableStateFlow<Long?>(null)
    val customMinPriceNgn: StateFlow<Long?> = _customMinPriceNgn.asStateFlow()

    private val _customMaxPriceNgn = MutableStateFlow<Long?>(null)
    val customMaxPriceNgn: StateFlow<Long?> = _customMaxPriceNgn.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _selectedLocation = MutableStateFlow<String?>(null)
    val selectedLocation: StateFlow<String?> = _selectedLocation.asStateFlow()

    private val _selectedConditionFilter = MutableStateFlow("ALL")
    val selectedConditionFilter: StateFlow<String> = _selectedConditionFilter.asStateFlow()

    private val _selectedSortOption = MutableStateFlow(ProductSortOption.FEATURED)
    val selectedSortOption: StateFlow<ProductSortOption> = _selectedSortOption.asStateFlow()

    private val _savedProductIds = MutableStateFlow<Set<String>>(setOf("prod_deye_5kw", "prod_felicity_5kwh"))
    val savedProductIds: StateFlow<Set<String>> = _savedProductIds.asStateFlow()

    private val _compareProductIds = MutableStateFlow<Set<String>>(emptySet())
    val compareProductIds: StateFlow<Set<String>> = _compareProductIds.asStateFlow()

    private val _isSellModalOpen = MutableStateFlow(false)
    val isSellModalOpen: StateFlow<Boolean> = _isSellModalOpen.asStateFlow()

    private val _selectedProduct = MutableStateFlow<MarketplaceProduct?>(null)
    val selectedProduct: StateFlow<MarketplaceProduct?> = _selectedProduct.asStateFlow()

    private val _isCartOpen = MutableStateFlow(false)
    val isCartOpen: StateFlow<Boolean> = _isCartOpen.asStateFlow()

    private val _isCheckoutConfirmationOpen = MutableStateFlow(false)
    val isCheckoutConfirmationOpen: StateFlow<Boolean> = _isCheckoutConfirmationOpen.asStateFlow()

    private val _isCheckoutSuccess = MutableStateFlow(false)
    val isCheckoutSuccess: StateFlow<Boolean> = _isCheckoutSuccess.asStateFlow()

    private val _lastPlacedOrder = MutableStateFlow<MarketplaceOrder?>(null)
    val lastPlacedOrder: StateFlow<MarketplaceOrder?> = _lastPlacedOrder.asStateFlow()

    private val _lastPlacedOrders = MutableStateFlow<List<MarketplaceOrder>>(emptyList())
    val lastPlacedOrders: StateFlow<List<MarketplaceOrder>> = _lastPlacedOrders.asStateFlow()

    private val _orderForIssueReport = MutableStateFlow<MarketplaceOrder?>(null)
    val orderForIssueReport: StateFlow<MarketplaceOrder?> = _orderForIssueReport.asStateFlow()

    private val _bannerFeedback = MutableStateFlow<String?>(null)
    val bannerFeedback: StateFlow<String?> = _bannerFeedback.asStateFlow()

    val currency: StateFlow<Currency> = repository.selectedCurrency
    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
    val prePurchaseInquiries: StateFlow<List<PrePurchaseVerificationInquiry>> = repository.prePurchaseInquiriesFlow
    val paymentGatewayManager = repository.paymentGateway
    val gatewayTransactions: StateFlow<List<PaymentVerificationResult>> = repository.gatewayTransactionsFlow
    val reconciliationRecords: StateFlow<List<PaymentReconciliationRecord>> = repository.reconciliationRecordsFlow
    val refundRecords: StateFlow<List<GatewayRefundRecord>> = repository.refundRecordsFlow
    val sellerSubaccounts: StateFlow<Map<String, PaystackSubaccount>> = repository.sellerSubaccountsFlow

    fun setPortalMode(mode: MarketplacePortalMode, currentUser: UserAccount? = null) {
        if (mode == MarketplacePortalMode.SELLER_DASHBOARD && currentUser != null) {
            val isAuthorizedSeller = currentUser.role == UserRole.VERIFIED_SELLER || currentUser.isAdmin
            val ownsStore = stores.value.any {
                it.ownerUid == currentUser.uid || it.ownerEmail.equals(currentUser.email, ignoreCase = true)
            }
            if (!isAuthorizedSeller && !ownsStore) {
                _bannerFeedback.value = "Become a Verified Seller to access the Seller Hub."
                _portalMode.value = MarketplacePortalMode.SELLER_REGISTRATION
                return
            }
            // If seller owns a store, ensure activeSellerStoreId points to their store
            val userStore = stores.value.find {
                it.ownerUid == currentUser.uid || it.ownerEmail.equals(currentUser.email, ignoreCase = true)
            }
            if (userStore != null) {
                _activeSellerStoreId.value = userStore.id
            }
        }
        _portalMode.value = mode
    }

    fun openSellerRegistration() {
        _portalMode.value = MarketplacePortalMode.SELLER_REGISTRATION
    }

    fun openCustomerSubScreen(subScreen: CustomerSubScreen) {
        _customerSubScreen.value = subScreen
    }

    fun openSellersDirectory() {
        _selectedStoreId.value = null
        _portalMode.value = MarketplacePortalMode.CUSTOMER_SHOP
        _customerSubScreen.value = CustomerSubScreen.SELLERS_DIRECTORY
    }

    fun openSellerStoreInsideApp(storeIdOrRoute: String) {
        val matchedStore = stores.value.find {
            it.id == storeIdOrRoute ||
                it.inAppStoreRoute.equals(storeIdOrRoute, ignoreCase = true) ||
                it.inAppBreadcrumb.equals(storeIdOrRoute, ignoreCase = true) ||
                it.storeName.equals(storeIdOrRoute, ignoreCase = true)
        }
        _selectedStoreId.value = matchedStore?.id ?: storeIdOrRoute
        _selectedProduct.value = null
        _portalMode.value = MarketplacePortalMode.CUSTOMER_SHOP
        _customerSubScreen.value = CustomerSubScreen.STOREFRONT
    }

    fun getActiveStoreInAppRoute(): String {
        val currentId = _selectedStoreId.value ?: return "marketplace/sellers"
        val store = stores.value.find { it.id == currentId }
        return store?.inAppStoreRoute ?: "marketplace/sellers/$currentId"
    }

    fun getActiveStoreInAppBreadcrumb(): String {
        val currentId = _selectedStoreId.value ?: return "Marketplace → Sellers"
        val store = stores.value.find { it.id == currentId }
        return store?.inAppBreadcrumb ?: "Marketplace → Sellers → $currentId"
    }

    fun closeSellerStore() {
        _selectedStoreId.value = null
        _customerSubScreen.value = CustomerSubScreen.CATALOG
    }

    fun selectActiveSellerStore(storeId: String, currentUser: UserAccount? = null) {
        if (currentUser != null && !currentUser.isAdmin) {
            val target = stores.value.find { it.id == storeId }
            val ownsTarget = target != null &&
                (target.ownerUid == currentUser.uid || target.ownerEmail.equals(currentUser.email, ignoreCase = true))
            if (!ownsTarget) {
                _bannerFeedback.value = "Access Denied: You can only manage your own registered store."
                return
            }
        }
        _activeSellerStoreId.value = storeId
    }

    // Customer Scoped Queries
    fun getOrdersForCustomer(customerEmail: String, customerUid: String? = null): List<MarketplaceOrder> {
        return repository.getOrdersForCustomer(customerEmail, customerUid)
    }

    fun getAddressesForCustomer(userUid: String, userEmail: String? = null): List<DeliveryAddress> {
        return repository.getAddressesForCustomer(userUid, userEmail)
    }

    // Seller Scoped Queries
    fun getProductsForSeller(storeId: String = _activeSellerStoreId.value): List<MarketplaceProduct> {
        return repository.getProductsForSeller(storeId)
    }

    fun getOrdersForSeller(storeId: String = _activeSellerStoreId.value): List<MarketplaceOrder> {
        return repository.getOrdersForSeller(storeId)
    }

    fun getPublicStoreProfiles(): List<PublicStoreProfile> {
        return repository.getPublicStoreProfiles()
    }

    fun selectCategory(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = SecuritySanitizer.sanitizeSearchQuery(query)
    }

    fun toggleCertifiedFilter() {
        _filterCertifiedOnly.value = !_filterCertifiedOnly.value
    }

    fun setCertifiedFilter(certifiedOnly: Boolean) {
        _filterCertifiedOnly.value = certifiedOnly
    }

    fun selectPriceRange(priceRange: PriceRangeFilter) {
        _selectedPriceRange.value = priceRange
        _customMinPriceNgn.value = null
        _customMaxPriceNgn.value = null
    }

    fun setCustomPriceRange(minNgn: Long?, maxNgn: Long?) {
        _customMinPriceNgn.value = minNgn
        _customMaxPriceNgn.value = maxNgn
        if (minNgn != null || maxNgn != null) {
            _selectedPriceRange.value = PriceRangeFilter.ALL
        }
    }

    fun selectBrand(brand: String?) {
        val cleaned = brand?.let { SecuritySanitizer.sanitizeText(it, maxLength = SecuritySanitizer.MAX_BRAND_MODEL_LENGTH) }
        _selectedBrand.value = cleaned?.takeIf { it.isNotBlank() && !it.equals("All Brands", ignoreCase = true) }
    }

    fun selectLocation(location: String?) {
        val cleaned = location?.let { SecuritySanitizer.sanitizeText(it, maxLength = SecuritySanitizer.MAX_CITY_STATE_LENGTH) }
        _selectedLocation.value = cleaned?.takeIf { it.isNotBlank() && !it.equals("All Locations", ignoreCase = true) }
    }

    fun selectConditionFilter(condition: String) {
        _selectedConditionFilter.value = condition.ifBlank { "ALL" }
    }

    fun selectSortOption(sortOption: ProductSortOption) {
        _selectedSortOption.value = sortOption
    }

    fun resetAllFilters() {
        _selectedCategory.value = ProductCategory.ALL
        _searchQuery.value = ""
        _filterCertifiedOnly.value = false
        _selectedPriceRange.value = PriceRangeFilter.ALL
        _customMinPriceNgn.value = null
        _customMaxPriceNgn.value = null
        _selectedBrand.value = null
        _selectedLocation.value = null
        _selectedConditionFilter.value = "ALL"
        _selectedSortOption.value = ProductSortOption.FEATURED
    }

    fun getActiveFilterCount(): Int {
        var count = 0
        if (_selectedCategory.value != ProductCategory.ALL) count++
        if (_filterCertifiedOnly.value) count++
        if (_selectedPriceRange.value != PriceRangeFilter.ALL || _customMinPriceNgn.value != null || _customMaxPriceNgn.value != null) count++
        if (_selectedBrand.value != null) count++
        if (_selectedLocation.value != null) count++
        if (_selectedConditionFilter.value != "ALL") count++
        if (_selectedSortOption.value != ProductSortOption.FEATURED) count++
        return count
    }

    fun getAvailableBrands(): List<String> {
        return products.value
            .filter { it.isActive && it.moderationStatus == ProductModerationStatus.APPROVED }
            .map { it.brand.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    fun getAvailableLocations(): List<String> {
        val coreCities = listOf("Lagos", "Abuja", "Port Harcourt", "Kano")
        val dynamicCities = products.value
            .filter { it.isActive && it.moderationStatus == ProductModerationStatus.APPROVED }
            .mapNotNull { prod ->
                when {
                    prod.sellerLocation.contains("Lagos", ignoreCase = true) -> "Lagos"
                    prod.sellerLocation.contains("Abuja", ignoreCase = true) -> "Abuja"
                    prod.sellerLocation.contains("Port Harcourt", ignoreCase = true) -> "Port Harcourt"
                    prod.sellerLocation.contains("Kano", ignoreCase = true) -> "Kano"
                    else -> prod.sellerLocation.substringAfterLast(",").trim().takeIf { it.isNotEmpty() }
                }
            }
        return (coreCities + dynamicCities).distinct()
    }

    fun toggleFavorite(productId: String) {
        _savedProductIds.update { current ->
            if (current.contains(productId)) current - productId else current + productId
        }
    }

    fun moveAllWishlistToCart() {
        val savedProducts = products.value.filter { _savedProductIds.value.contains(it.id) && it.inStock && it.stockCount > 0 }
        if (savedProducts.isEmpty()) {
            showFeedback("No in-stock wishlist items to move to cart.")
            return
        }
        savedProducts.forEach { prod ->
            repository.addToCart(prod, PaymentPlanType.OUTRIGHT)
        }
        showFeedback("Added ${savedProducts.size} wishlist item(s) to your cart.")
    }

    fun toggleCompareProduct(productId: String) {
        _compareProductIds.update { current ->
            if (current.contains(productId)) {
                current - productId
            } else if (current.size >= 4) {
                showFeedback("You can compare up to 4 solar products side-by-side.")
                current
            } else {
                current + productId
            }
        }
    }

    fun clearComparison() {
        _compareProductIds.value = emptySet()
    }

    fun openSellModal(open: Boolean) {
        if (open) {
            val activeStore = stores.value.find { it.id == _activeSellerStoreId.value }
            if (activeStore != null && activeStore.status != StoreVerificationStatus.APPROVED) {
                showFeedback(
                    "Only APPROVED sellers can publish products. '${activeStore.storeName}' is currently '${activeStore.status.label}'."
                )
                return
            }
        }
        _isSellModalOpen.value = open
    }

    fun selectProduct(product: MarketplaceProduct?) {
        _selectedProduct.value = product
    }

    fun setCurrency(newCurrency: Currency) {
        repository.selectCurrency(newCurrency)
    }

    fun openCart(open: Boolean) {
        _isCartOpen.value = open
    }

    fun openCheckoutConfirmation(open: Boolean) {
        _isCheckoutConfirmationOpen.value = open
    }

    fun addToCart(product: MarketplaceProduct, plan: PaymentPlanType = PaymentPlanType.OUTRIGHT) {
        if (!product.inStock || product.stockCount <= 0) {
            showFeedback("${product.title} is currently out of stock.")
            return
        }
        repository.addToCart(product, plan)
        showFeedback("Added ${product.title} to your cart.")
    }

    fun updateCartItemQuantity(productId: String, delta: Int) {
        repository.updateCartQuantity(productId, delta)
    }

    fun setCartItemQuantity(productId: String, quantity: Int) {
        repository.setCartItemQuantity(productId, quantity)
    }

    fun removeCartItem(productId: String) {
        val removedTitle = cartItems.value.find { it.product.id == productId }?.product?.title
        repository.removeFromCart(productId)
        if (removedTitle != null) {
            showFeedback("Removed $removedTitle from your cart.")
        }
    }

    fun clearCart() {
        repository.clearCart()
        showFeedback("Cleared shopping cart.")
    }

    fun updateCartPaymentPlan(productId: String, plan: PaymentPlanType) {
        repository.updateCartPaymentPlan(productId, plan)
    }

    fun getCartBreakdown(): CartSummaryBreakdown {
        return repository.calculateCartBreakdown()
    }

    fun showFeedback(message: String?) {
        _bannerFeedback.value = message
    }

    fun addNewListing(
        title: String,
        category: ProductCategory,
        brand: String,
        priceNgn: Long,
        location: String,
        sellerName: String,
        isCertifiedSeller: Boolean,
        condition: String,
        specs: Map<String, String>,
        description: String,
        photoBitmap: Bitmap?,
        certificationBadge: String = "VERIFIED SELLER",
        storeId: String = _activeSellerStoreId.value,
        warrantyPeriod: String = "2 Years Manufacturer Warranty",
        stockCount: Int = 10,
        installmentAllowed: Boolean = true,
        modelNumber: String = "",
        deliveryAvailability: String = "Available Nationwide (24-48H Dispatch)",
        returnPolicy: String = "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy",
        sellerAuthenticityConfirmed: Boolean = true,
        authenticityEvidenceNote: String = "OEM Serial & Batch Declaration Confirmed"
    ): Boolean {
        if (!sellerAuthenticityConfirmed) {
            showFeedback(
                "Action blocked: Sellers must confirm that their products are genuine and specifications are accurate."
            )
            return false
        }

        val store = stores.value.find { it.id == storeId }
        if (store != null && store.status != StoreVerificationStatus.APPROVED) {
            _isSellModalOpen.value = false
            showFeedback(
                "Action blocked: Only APPROVED sellers can publish products and operate their stores. Current status: ${store.status.label}."
            )
            return false
        }

        val resolvedStoreName = sellerName.trim().ifEmpty { store?.storeName ?: "NEKES Solar Energy Store" }
        val resolvedLocation = location.trim().ifEmpty { store?.location ?: "Lagos, Nigeria" }
        val isStoreApproved = store?.status == StoreVerificationStatus.APPROVED || isCertifiedSeller
        val resolvedBadge = certificationBadge.trim().ifEmpty {
            if (isStoreApproved) "VERIFIED SELLER" else (store?.accreditationBadge ?: "Pending Verification")
        }

        val newProduct = MarketplaceProduct(
            id = "seller_listing_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            category = category,
            brand = brand.trim().ifEmpty { "Certified OEM" },
            modelNumber = modelNumber.trim(),
            powerRating = specs["Rating"] ?: specs["Capacity"] ?: specs["Power Rating"] ?: "Verified Rating",
            voltage = specs["Voltage"] ?: "48V System",
            basePriceNgn = priceNgn,
            rating = 5.0f,
            reviewsCount = 1,
            badge = if (isStoreApproved) "VERIFIED SELLER" else "New Arrival",
            inStock = stockCount > 0,
            specs = specs,
            description = description.trim(),
            sellerName = resolvedStoreName,
            sellerLocation = resolvedLocation,
            isCertifiedSeller = isStoreApproved,
            sellerCertificationBadge = resolvedBadge,
            condition = condition,
            photoBitmap = photoBitmap,
            storeId = storeId,
            warrantyPeriod = warrantyPeriod,
            stockCount = stockCount,
            installmentAllowed = installmentAllowed,
            deliveryAvailability = deliveryAvailability.trim().ifEmpty { "Available Nationwide (24-48H Dispatch)" },
            returnPolicy = returnPolicy.trim().ifEmpty { "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy" },
            sellerAuthenticityConfirmed = true,
            authenticityEvidenceNote = authenticityEvidenceNote.trim().ifEmpty { "OEM Serial & Batch Declaration Confirmed" },
            solarfixInspectionStatus = SolarFixInspectionStatus.NOT_INSPECTED,
            solarfixInspectionReportId = null,
            moderationStatus = ProductModerationStatus.APPROVED
        )

        repository.addOrUpdateProduct(newProduct)
        _isSellModalOpen.value = false
        showFeedback("Published \"${newProduct.title}\" to $resolvedStoreName!")
        return true
    }

    /**
     * Comprehensive Seller Dashboard Product Editor action:
     * Supports both adding new products and editing existing products with multiple photographs,
     * product video URL/URI, custom specifications, warranty period, delivery options, model number,
     * return policy, and mandatory seller authenticity confirmation.
     */
    fun saveOrUpdateSellerProduct(
        existingProductId: String? = null,
        storeId: String = _activeSellerStoreId.value,
        title: String,
        category: ProductCategory,
        brand: String,
        powerRating: String,
        voltage: String,
        priceNgn: Long,
        stockCount: Int,
        condition: String,
        specs: Map<String, String>,
        description: String,
        warrantyPeriod: String,
        deliveryOptions: List<String>,
        photoBitmaps: List<Bitmap>,
        videoUrlOrUri: String?,
        videoDescription: String = "360° Load Test & Serial Inspection Video",
        installmentAllowed: Boolean = true,
        isActive: Boolean = true,
        modelNumber: String = "",
        deliveryAvailability: String = "",
        returnPolicy: String = "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy",
        sellerAuthenticityConfirmed: Boolean = true,
        authenticityEvidenceNote: String = "OEM Serial & Batch Declaration Confirmed"
    ): MarketplaceProduct? {
        if (!sellerAuthenticityConfirmed) {
            showFeedback(
                "Action blocked: Sellers must confirm that their products are genuine and specifications are accurate."
            )
            return null
        }

        val store = stores.value.find { it.id == storeId }
        if (store != null && store.status != StoreVerificationStatus.APPROVED) {
            showFeedback(
                "Action blocked: Only APPROVED sellers can publish or edit products. Current status: ${store.status.label}."
            )
            return null
        }

        val existingProduct = existingProductId?.let { id -> products.value.find { it.id == id } }
        val resolvedStoreName = store?.storeName ?: existingProduct?.sellerName ?: "NEKES Solar Energy Store"
        val resolvedLocation = store?.location ?: existingProduct?.sellerLocation ?: "Lagos, Nigeria"
        val primaryBitmap = photoBitmaps.firstOrNull() ?: existingProduct?.photoBitmap
        val resolvedDeliveryOpts = deliveryOptions.ifEmpty {
            listOf("Doorstep Delivery (24-48H)", "Warehouse Self-Pickup")
        }

        val cleanSpecs = specs.toMutableMap().apply {
            if (powerRating.isNotBlank() && !containsKey("Power Rating") && !containsKey("Rating")) {
                put("Power Rating", powerRating.trim())
            }
            if (voltage.isNotBlank() && !containsKey("Voltage")) {
                put("Voltage", voltage.trim())
            }
        }

        val savedProduct = MarketplaceProduct(
            id = existingProduct?.id ?: "seller_listing_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            category = category,
            brand = brand.trim().ifEmpty { "Certified OEM" },
            modelNumber = modelNumber.trim().ifEmpty { existingProduct?.modelNumber.orEmpty() },
            powerRating = powerRating.trim().ifEmpty { cleanSpecs["Power Rating"] ?: cleanSpecs["Rating"] ?: "Verified Rating" },
            voltage = voltage.trim().ifEmpty { cleanSpecs["Voltage"] ?: "48V System" },
            basePriceNgn = priceNgn.coerceAtLeast(1000L),
            rating = existingProduct?.rating ?: 5.0f,
            reviewsCount = existingProduct?.reviewsCount ?: 1,
            badge = "VERIFIED SELLER",
            inStock = stockCount > 0,
            specs = cleanSpecs,
            description = description.trim(),
            sellerName = resolvedStoreName,
            sellerLocation = resolvedLocation,
            isCertifiedSeller = true,
            sellerCertificationBadge = "VERIFIED SELLER",
            condition = condition.trim().ifEmpty { "Brand New" },
            photoBitmap = primaryBitmap,
            photoBitmaps = photoBitmaps,
            videoUrlOrUri = videoUrlOrUri?.trim()?.takeIf { it.isNotEmpty() },
            videoDescription = videoDescription.trim().ifEmpty { "360° Load Test & Serial Inspection Video" },
            deliveryOptions = resolvedDeliveryOpts,
            deliveryAvailability = deliveryAvailability.trim().ifEmpty {
                existingProduct?.deliveryAvailability ?: resolvedDeliveryOpts.joinToString(" • ")
            },
            returnPolicy = returnPolicy.trim().ifEmpty {
                existingProduct?.returnPolicy ?: "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy"
            },
            sellerAuthenticityConfirmed = true,
            authenticityEvidenceNote = authenticityEvidenceNote.trim().ifEmpty {
                existingProduct?.authenticityEvidenceNote ?: "OEM Serial & Batch Declaration Confirmed"
            },
            adminModerationNote = existingProduct?.adminModerationNote ?: "Listing active & seller authenticity declaration on file.",
            solarfixInspectionStatus = existingProduct?.solarfixInspectionStatus ?: SolarFixInspectionStatus.NOT_INSPECTED,
            solarfixInspectionReportId = existingProduct?.solarfixInspectionReportId,
            solarfixInspectionDate = existingProduct?.solarfixInspectionDate,
            isActive = isActive,
            storeId = storeId,
            warrantyPeriod = warrantyPeriod.trim().ifEmpty { "2 Years Manufacturer Warranty" },
            stockCount = stockCount.coerceAtLeast(0),
            installmentAllowed = installmentAllowed,
            maxInstallmentMonths = existingProduct?.maxInstallmentMonths ?: 6,
            moderationStatus = existingProduct?.moderationStatus ?: ProductModerationStatus.APPROVED
        )

        repository.addOrUpdateProduct(savedProduct)
        showFeedback(
            if (existingProduct == null) {
                "Added new product \"${savedProduct.title}\" to $resolvedStoreName!"
            } else {
                "Updated product \"${savedProduct.title}\" in $resolvedStoreName."
            }
        )
        return savedProduct
    }

    /**
     * Submits a new SolarFix Seller Registration & KYC/KYB Verification application.
     * Every new seller initially receives a Pending Verification (StoreVerificationStatus.PENDING) status.
     */
    fun submitSellerRegistration(
        fullNameOrBusinessName: String,
        ownerFullName: String,
        cacRegistrationNumber: String,
        businessPhone: String,
        businessEmail: String,
        physicalAddress: String,
        state: String,
        city: String,
        businessDescription: String,
        logoBitmap: Bitmap?,
        idDocumentType: String,
        idDocumentNumber: String,
        idDocumentBitmap: Bitmap?,
        premisesPhotoBitmap: Bitmap?,
        bankAccountName: String,
        bankName: String,
        bankAccountNumber: String
    ): SellerStore {
        val createdStore = repository.submitSellerRegistrationApplication(
            businessOrFullName = fullNameOrBusinessName,
            cacRegNumber = cacRegistrationNumber,
            businessPhone = businessPhone,
            businessEmail = businessEmail,
            physicalAddress = physicalAddress,
            city = city,
            state = state,
            businessDescription = businessDescription,
            logoBitmap = logoBitmap,
            idDocumentType = idDocumentType,
            idDocumentNumber = idDocumentNumber,
            idDocumentBitmap = idDocumentBitmap,
            premisesPhotoBitmap = premisesPhotoBitmap,
            bankAccountName = bankAccountName,
            bankName = bankName,
            bankAccountNumber = bankAccountNumber
        ).let { store ->
            if (ownerFullName.isNotBlank()) {
                val withOwner = store.copy(ownerName = ownerFullName.trim())
                repository.createOrUpdateStore(withOwner)
                withOwner
            } else {
                store
            }
        }
        _activeSellerStoreId.value = createdStore.id
        _portalMode.value = MarketplacePortalMode.SELLER_DASHBOARD
        showFeedback(
            "Seller application submitted for '${createdStore.storeName}'! Status: Pending Verification."
        )
        return createdStore
    }

    /**
     * Allows a seller whose application has status MORE_INFO_REQUIRED (or PENDING) to submit
     * updated KYC/KYB verification details so the Administrator can re-evaluate.
     */
    fun resubmitSellerVerificationInfo(
        storeId: String,
        cacRegistrationNumber: String,
        physicalAddress: String,
        idDocumentType: String,
        idDocumentNumber: String,
        bankAccountName: String,
        bankName: String,
        bankAccountNumber: String,
        sellerResponseNote: String
    ) {
        val existing = stores.value.find { it.id == storeId } ?: return
        val updated = existing.copy(
            businessRegNumber = cacRegistrationNumber.trim().ifEmpty { existing.businessRegNumber },
            physicalAddress = physicalAddress.trim().ifEmpty { existing.physicalAddress },
            idDocumentType = idDocumentType.trim().ifEmpty { existing.idDocumentType },
            idDocumentNumber = idDocumentNumber.trim().ifEmpty { existing.idDocumentNumber },
            idDocumentProvided = true,
            premisesPhotoProvided = true,
            bankAccountName = bankAccountName.trim().ifEmpty { existing.bankAccountName },
            bankName = bankName.trim().ifEmpty { existing.bankName },
            bankAccountNumber = bankAccountNumber.trim().ifEmpty { existing.bankAccountNumber },
            status = StoreVerificationStatus.PENDING,
            accreditationBadge = "Pending Verification",
            adminVerificationNote = "Seller submitted additional verification details: ${sellerResponseNote.ifBlank { "Updated KYC & CAC documents attached." }}"
        )
        repository.createOrUpdateStore(updated)
        showFeedback("Additional verification documents submitted! Status updated to Pending Verification.")
    }

    fun updateSellerProductPriceAndStock(productId: String, newPriceNgn: Long, newStockCount: Int) {
        val prod = products.value.find { it.id == productId }
        val store = stores.value.find { it.id == prod?.storeId }
        if (store != null && store.status != StoreVerificationStatus.APPROVED) {
            showFeedback("Only approved sellers can operate their store inventory.")
            return
        }
        repository.updateProductPriceAndStock(productId, newPriceNgn, newStockCount)
        showFeedback("Updated inventory price and available stock.")
    }

    fun deleteSellerProduct(productId: String) {
        repository.deleteProductListing(productId)
        showFeedback("Removed product listing from store catalog.")
    }

    fun toggleSellerProductActiveStatus(productId: String): Boolean {
        val prod = products.value.find { it.id == productId }
        val isNowActive = repository.toggleProductActiveStatus(productId)
        showFeedback(
            if (isNowActive) {
                "Activated \"${prod?.title ?: "Product"}\" — now visible in customer storefront."
            } else {
                "Deactivated \"${prod?.title ?: "Product"}\" — hidden from customer storefront."
            }
        )
        return isNowActive
    }

    fun saveSellerStoreProfile(
        storeId: String?,
        storeName: String,
        ownerName: String,
        ownerEmail: String,
        businessRegNumber: String,
        location: String,
        phone: String,
        tagline: String,
        description: String,
        warrantyPolicy: String,
        logoBitmap: Bitmap? = null,
        coverImageBitmap: Bitmap? = null
    ) {
        val existing = stores.value.find { it.id == storeId }
        val targetId = existing?.id ?: "store_${UUID.randomUUID().toString().take(6)}"
        val resolvedStatus = existing?.status ?: StoreVerificationStatus.PENDING
        val updatedStore = (existing ?: SellerStore(
            id = targetId,
            storeName = storeName.trim(),
            ownerUid = "owner_custom",
            ownerName = ownerName.trim(),
            ownerEmail = ownerEmail.trim(),
            businessRegNumber = businessRegNumber.trim().ifEmpty { "Individual Seller (CAC N/A)" },
            location = location.trim(),
            phone = phone.trim(),
            tagline = tagline.trim(),
            description = description.trim(),
            warrantyPolicy = warrantyPolicy.trim(),
            status = StoreVerificationStatus.PENDING,
            accreditationBadge = "Pending Verification"
        )).copy(
            storeName = storeName.trim(),
            ownerName = ownerName.trim(),
            ownerEmail = ownerEmail.trim(),
            businessRegNumber = businessRegNumber.trim().ifEmpty { "Individual Seller (CAC N/A)" },
            location = location.trim(),
            phone = phone.trim(),
            tagline = tagline.trim(),
            description = description.trim(),
            warrantyPolicy = warrantyPolicy.trim(),
            status = resolvedStatus,
            logoBitmap = logoBitmap ?: existing?.logoBitmap,
            coverImageBitmap = coverImageBitmap ?: existing?.coverImageBitmap
        )
        repository.createOrUpdateStore(updatedStore)
        _activeSellerStoreId.value = targetId
        showFeedback(
            if (existing == null) {
                "Submitted new store '${updatedStore.storeName}' (Status: Pending Verification)."
            } else {
                "Saved store profile for ${updatedStore.storeName}."
            }
        )
    }

    fun submitReview(productId: String, customerName: String, rating: Int, comment: String) {
        if (comment.isBlank()) return
        repository.submitProductReview(productId, customerName.ifBlank { "Verified SolarFix Buyer" }, rating, comment)
        // Refresh selectedProduct if open
        _selectedProduct.value = products.value.find { it.id == productId }
        showFeedback("Thank you! Your verified product review has been posted.")
    }

    fun replyToReview(reviewId: String, replyText: String) {
        if (replyText.isBlank()) return
        repository.replyToProductReview(reviewId, replyText)
        showFeedback("Posted official Seller Store response to customer review.")
    }

    fun addAddress(
        label: String,
        recipientName: String,
        phone: String,
        street: String,
        city: String,
        state: String,
        isDefault: Boolean
    ) {
        val addr = DeliveryAddress(
            id = "addr_${UUID.randomUUID().toString().take(6)}",
            label = label.trim().ifEmpty { "Delivery Address" },
            recipientName = recipientName.trim(),
            phone = phone.trim(),
            streetAddress = street.trim(),
            city = city.trim(),
            state = state.trim(),
            isDefault = isDefault
        )
        repository.addDeliveryAddress(addr)
        showFeedback("Saved delivery address: ${addr.label}")
    }

    fun setDefaultAddress(addressId: String) {
        repository.setDefaultAddress(addressId)
    }

    fun deleteAddress(addressId: String) {
        repository.deleteAddress(addressId)
    }

    fun checkoutOrder() {
        if (cartItems.value.isNotEmpty()) {
            _isCartOpen.value = false
            _isCheckoutConfirmationOpen.value = true
        }
    }

    fun getCartValidationIssues(): List<String> {
        return repository.validateCartBeforeCheckout()
    }

    fun refreshCartPricesAndStock(): List<String> {
        val notices = repository.refreshCartWithCurrentCatalog()
        if (notices.isNotEmpty()) {
            showFeedback("Cart refreshed: ${notices.joinToString("; ")}")
        } else {
            showFeedback("All cart items and prices are up to date.")
        }
        return notices
    }

    fun requestExpensiveEquipmentVerification(
        productId: String,
        requestType: String,
        notes: String,
        contact: String = ""
    ): PrePurchaseVerificationInquiry {
        val resolvedContact = contact.ifBlank { "+234 803 700 1234" }
        val inquiry = repository.submitPrePurchaseInquiry(productId, requestType, notes, resolvedContact)
        showFeedback(
            "Pre-payment verification request submitted for '${inquiry.productTitle}'. Store '${inquiry.storeName}' has been notified."
        )
        return inquiry
    }

    fun initializePaymentForCart(customerEmail: String, channel: String = "card"): PaymentInitializationResult {
        val breakdown = repository.calculateCartBreakdown()
        val storesInCart = stores.value.filter { st -> breakdown.sellerBreakdowns.any { it.storeId == st.id } }
        return repository.initializeGatewayPayment(customerEmail, breakdown.finalOrderTotalNgn, storesInCart)
    }

    fun verifyPaymentOnServer(
        reference: String,
        expectedAmountKobo: Long,
        email: String = "customer@solarfix.africa",
        name: String = "SolarFix Customer",
        channel: String = "card"
    ): PaymentVerificationResult {
        val expectedNgn = expectedAmountKobo / 100L
        return repository.verifyServerPayment(reference, expectedNgn, email, name, channel)
    }

    fun simulateIncomingWebhook(event: PaymentWebhookEvent): Boolean {
        val rawPayload = "{\"event\":\"${event.event}\",\"reference\":\"${event.reference}\"}"
        val success = repository.processPaymentWebhook(
            rawPayload = rawPayload,
            signatureHeader = event.hmacSignature,
            event = event
        )
        if (success) {
            showFeedback("Webhook event '${event.event}' verified via HMAC-SHA512: Reference ${event.reference} processed.")
        }
        return success
    }

    fun simulateIncomingWebhook(reference: String, eventType: String = "charge.success"): PaymentVerificationResult {
        val amountKobo = 25000000L
        val rawPayload = "{\"event\":\"$eventType\",\"reference\":\"$reference\"}"
        // Secure server-side calculation: Paystack signs payloads with PAYSTACK_SECRET_KEY in x-paystack-signature
        val authenticSignature = repository.paymentGateway.computeHmacSha512(rawPayload)
        val event = PaymentWebhookEvent(
            eventId = "evt_${System.currentTimeMillis()}",
            event = eventType,
            reference = reference,
            amountKobo = amountKobo,
            currency = "NGN",
            status = "success",
            channel = "card",
            customerEmail = "customer@solarfix.africa",
            paidAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            hmacSignature = authenticSignature,
            signatureValid = true
        )
        simulateIncomingWebhook(event)
        return repository.gatewayTransactionsFlow.value.find { it.reference == reference }
            ?: PaymentVerificationResult(
                success = true,
                reference = reference,
                gatewayReference = "PSTK-SIM-${System.currentTimeMillis().toString().takeLast(6)}",
                amountKobo = amountKobo,
                amountNgn = amountKobo / 100L,
                currency = "NGN",
                channel = "card",
                paidAt = event.paidAt,
                customerEmail = event.customerEmail,
                customerName = "Webhook Customer",
                gatewayResponse = "Webhook simulation processed"
            )
    }

    fun processRefundForOrder(orderId: String, reason: String): GatewayRefundRecord? {
        val res = repository.processOrderRefund(orderId, reason)
        if (res != null) {
            showFeedback("Refund of ₦${res.amountNgn} processed via Paystack API for Order #${orderId}.")
        } else {
            showFeedback("Unable to process refund for Order #${orderId}.")
        }
        return res
    }

    fun processRefundForOrder(orderId: String, amountNgn: Long, reason: String): GatewayRefundRecord? {
        return processRefundForOrder(orderId, reason)
    }

    fun generateReconciliationReport(dateLabel: String = "Today's Reconciliation"): PaymentReconciliationRecord {
        val record = repository.generatePaymentReconciliation(dateLabel)
        showFeedback("Reconciliation generated: ${record.status.label} (₦${record.grossCapturedNgn} Gross).")
        return record
    }

    fun confirmAndPlaceOrder(
        customerName: String,
        customerEmail: String,
        address: DeliveryAddress,
        paymentPlan: PaymentPlanType,
        paymentMethod: PaymentMethodType,
        paymentReference: String? = null
    ) {
        val validationIssues = repository.validateCartBeforeCheckout()
        if (validationIssues.isNotEmpty()) {
            showFeedback("Payment halted: ${validationIssues.first()}")
            return
        }

        val placedOrders = repository.placeMultiSellerOrders(
            customerName = customerName.ifBlank { "Verified SolarFix Customer" },
            customerEmail = customerEmail.ifBlank { "customer@solarfix.africa" },
            address = address,
            paymentPlan = paymentPlan,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference
        )
        if (placedOrders.isNotEmpty()) {
            _lastPlacedOrders.value = placedOrders
            _lastPlacedOrder.value = placedOrders.first()
            _isCheckoutConfirmationOpen.value = false
            _isCheckoutSuccess.value = true
            val ref = placedOrders.first().paymentReference ?: "Verified"
            showFeedback("Payment verified on server via Paystack ($ref). Order confirmed!")
        } else {
            showFeedback("Payment verification failed or duplicate detected. Please try again.")
        }
    }

    fun dismissCheckoutSuccess() {
        _isCheckoutSuccess.value = false
        _isCartOpen.value = false
    }

    fun openIssueReportModal(order: MarketplaceOrder?) {
        _orderForIssueReport.value = order
    }

    fun submitOrderDispute(orderId: String, issueType: DisputeIssueType, description: String) {
        repository.reportOrderIssue(orderId, issueType, description)
        _orderForIssueReport.value = null
        showFeedback("Dispute logged for Order #$orderId. Escrow payout frozen pending Admin resolution.")
    }

    fun advanceSellerOrderStatus(orderId: String, nextStatus: OrderStatus) {
        repository.updateOrderStatus(orderId, nextStatus)
        showFeedback("Updated Order #$orderId status to ${nextStatus.label}.")
    }

    fun acceptSellerOrder(orderId: String): Boolean {
        val ok = repository.acceptOrderBySeller(orderId)
        if (ok) {
            showFeedback("Accepted Order #$orderId! Status updated to Processing at Store.")
        } else {
            showFeedback("Unable to accept Order #$orderId in its current state.")
        }
        return ok
    }

    fun rejectSellerOrder(orderId: String, reason: String): Boolean {
        val ok = repository.rejectOrderBySeller(orderId, reason)
        if (ok) {
            showFeedback("Rejected Order #$orderId ($reason). Buyer refunded and stock restored.")
        } else {
            showFeedback("Marketplace Rule: Only unshipped orders can be rejected by the seller.")
        }
        return ok
    }

    fun getSellerSalesAnalytics(storeId: String = _activeSellerStoreId.value): SellerSalesAnalytics {
        return repository.computeSellerSalesAnalytics(storeId)
    }

    fun requestSellerWithdrawal(
        storeId: String,
        amountNgn: Long,
        bankName: String,
        accountNumber: String,
        accountName: String
    ): Boolean {
        val minAmount = settings.value.minWithdrawalNgn
        if (amountNgn < minAmount) {
            showFeedback("Minimum withdrawal amount is ₦${String.format("%,d", minAmount)}.")
            return false
        }
        val req = repository.requestSellerWithdrawal(storeId, amountNgn, bankName, accountNumber, accountName)
        return if (req != null) {
            showFeedback("Withdrawal request (${req.id}) for ₦${String.format("%,d", amountNgn)} submitted for Admin payout.")
            true
        } else {
            showFeedback("Insufficient available earnings balance for this withdrawal.")
            false
        }
    }

    // --- Administrator Control Methods ---

    fun setSellerVerificationStatus(
        storeId: String,
        status: StoreVerificationStatus,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.updateStoreVerificationStatus(storeId, status, adminNote, adminEmail)
    }

    fun setProductModerationStatus(
        productId: String,
        status: ProductModerationStatus,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.setProductModerationStatus(productId, status, adminNote, adminEmail)
    }

    fun flagSuspiciousProduct(productId: String, adminNote: String) {
        repository.flagSuspiciousProduct(productId, adminNote)
        showFeedback("Flagged product as Suspicious / High-Risk for compliance audit.")
    }

    fun requestProductAuthenticityEvidence(productId: String, adminNote: String) {
        repository.requestProductAuthenticityEvidence(productId, adminNote)
        showFeedback("Requested OEM serial & authenticity evidence from seller.")
    }

    fun submitSellerProductAuthenticityEvidence(productId: String, evidenceNote: String) {
        if (evidenceNote.isBlank()) return
        repository.submitSellerProductAuthenticityEvidence(productId, evidenceNote)
        showFeedback("Submitted OEM authenticity evidence for Administrator review.")
    }

    fun removeCounterfeitProduct(
        productId: String,
        adminReason: String,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.removeCounterfeitProduct(productId, adminReason, adminEmail)
        showFeedback("Removed counterfeit / misleading product listing from SolarFix Marketplace.")
    }

    fun recordSolarFixProductInspection(
        productId: String,
        passed: Boolean,
        reportId: String,
        inspectionNote: String = "Physical SolarFix lab inspection completed.",
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.recordSolarFixProductInspection(productId, passed, reportId, inspectionNote, adminEmail)
        showFeedback(
            if (passed) {
                "Recorded independent SolarFix lab inspection (#$reportId)!"
            } else {
                "Product failed independent SolarFix lab inspection and was removed."
            }
        )
    }

    fun processWithdrawal(
        withdrawalId: String,
        approve: Boolean,
        adminNote: String? = null,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.processWithdrawalRequest(withdrawalId, approve, adminNote, adminEmail)
    }

    fun resolveDispute(
        disputeId: String,
        resolution: DisputeStatus,
        adminNote: String,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.resolveDispute(disputeId, resolution, adminNote, adminEmail)
    }

    fun updateMarketplaceSettings(
        newSettings: MarketplaceSettings,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.updateMarketplaceSettings(newSettings, adminEmail)
    }

    fun setSellerCustomCommission(
        storeId: String,
        customCommissionPercent: Double?,
        adminEmail: String = "admin@solarfix.africa"
    ) {
        repository.setSellerCustomCommission(storeId, customCommissionPercent, adminEmail)
        val note = if (customCommissionPercent != null) "Custom Commission set to ${customCommissionPercent}%" else "Reset to default platform commission"
        showFeedback("Updated seller commission configuration: $note")
    }

    fun generateFinancialReport(): FinancialReportSummary {
        return repository.generateFinancialReport()
    }

    fun getFilteredProducts(storeFilterId: String? = null): List<MarketplaceProduct> {
        val category = _selectedCategory.value
        val query = _searchQuery.value.trim().lowercase()
        val certifiedOnly = _filterCertifiedOnly.value
        val priceRange = _selectedPriceRange.value
        val customMin = _customMinPriceNgn.value
        val customMax = _customMaxPriceNgn.value
        val brandFilter = _selectedBrand.value?.trim()
        val locationFilter = _selectedLocation.value?.trim()
        val conditionFilter = _selectedConditionFilter.value
        val sortOption = _selectedSortOption.value

        val approvedStoreIds = stores.value
            .filter { it.status == StoreVerificationStatus.APPROVED }
            .map { it.id }
            .toSet()

        val filtered = products.value.filter { product ->
            val storeApproved = approvedStoreIds.contains(product.storeId) || product.storeId.startsWith("seller_")
            val listingApproved = product.moderationStatus == ProductModerationStatus.APPROVED && product.isActive
            val matchesStore = (storeFilterId == null) || (product.storeId == storeFilterId)
            val matchesCategory = (category == ProductCategory.ALL) || (product.category == category)
            val matchesQuery = query.isEmpty() ||
                product.title.lowercase().contains(query) ||
                product.brand.lowercase().contains(query) ||
                product.effectiveModelNumber.lowercase().contains(query) ||
                product.powerRating.lowercase().contains(query) ||
                product.description.lowercase().contains(query) ||
                product.sellerLocation.lowercase().contains(query) ||
                product.sellerName.lowercase().contains(query) ||
                product.specs.any { (k, v) -> k.lowercase().contains(query) || v.lowercase().contains(query) }
            val matchesCertified = !certifiedOnly || product.isCertifiedSeller

            val minAllowed = customMin ?: priceRange.minNgn
            val maxAllowed = customMax ?: priceRange.maxNgn
            val matchesPrice = product.basePriceNgn in minAllowed..maxAllowed

            val matchesBrand = brandFilter.isNullOrEmpty() ||
                product.brand.equals(brandFilter, ignoreCase = true) ||
                product.brand.contains(brandFilter, ignoreCase = true)

            val matchesLocation = locationFilter.isNullOrEmpty() ||
                product.sellerLocation.contains(locationFilter, ignoreCase = true)

            val matchesCondition = conditionFilter == "ALL" ||
                product.condition.equals(conditionFilter, ignoreCase = true)

            storeApproved &&
                listingApproved &&
                matchesStore &&
                matchesCategory &&
                matchesQuery &&
                matchesCertified &&
                matchesPrice &&
                matchesBrand &&
                matchesLocation &&
                matchesCondition
        }

        return when (sortOption) {
            ProductSortOption.FEATURED -> filtered
            ProductSortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.basePriceNgn }
            ProductSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.basePriceNgn }
            ProductSortOption.HIGHEST_RATED -> filtered.sortedWith(
                compareByDescending<MarketplaceProduct> { it.rating }.thenByDescending { it.reviewsCount }
            )
        }
    }

    /**
     * Returns related solar equipment for a given product (same category, compatible system voltage,
     * or same brand, excluding the current product itself).
     */
    fun getRelatedProducts(product: MarketplaceProduct, limit: Int = 6): List<MarketplaceProduct> {
        val approvedStoreIds = stores.value
            .filter { it.status == StoreVerificationStatus.APPROVED }
            .map { it.id }
            .toSet()

        val candidates = products.value.filter { cand ->
            cand.id != product.id &&
                cand.isActive &&
                cand.moderationStatus == ProductModerationStatus.APPROVED &&
                (approvedStoreIds.contains(cand.storeId) || cand.storeId.startsWith("seller_"))
        }

        return candidates.sortedWith(
            compareByDescending<MarketplaceProduct> { it.category == product.category }
                .thenByDescending { it.voltage.equals(product.voltage, ignoreCase = true) }
                .thenByDescending { it.brand.equals(product.brand, ignoreCase = true) }
                .thenByDescending { it.rating }
        ).take(limit)
    }

    fun getReviewsForProduct(productId: String): List<ProductReview> {
        return reviews.value.filter { it.productId == productId }
    }

    fun getTotalCartItemsCount(): Int {
        return cartItems.value.sumOf { it.quantity }
    }

    fun getTotalCartPriceNgn(): Long {
        return cartItems.value.sumOf { it.product.basePriceNgn * it.quantity }
    }
}
