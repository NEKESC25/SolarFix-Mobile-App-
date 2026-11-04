package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.SolarfixHeader
import com.example.ui.theme.CleanGreen
import com.example.ui.viewmodel.CustomerSubScreen
import com.example.ui.viewmodel.MarketplacePortalMode
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel
) {
    val portalMode by viewModel.portalMode.collectAsState()
    val customerSubScreen by viewModel.customerSubScreen.collectAsState()
    val selectedStoreId by viewModel.selectedStoreId.collectAsState()
    val activeSellerStoreId by viewModel.activeSellerStoreId.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val disputes by viewModel.disputes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterCertifiedOnly by viewModel.filterCertifiedOnly.collectAsState()
    val selectedPriceRange by viewModel.selectedPriceRange.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedConditionFilter by viewModel.selectedConditionFilter.collectAsState()
    val selectedSortOption by viewModel.selectedSortOption.collectAsState()

    val savedProductIds by viewModel.savedProductIds.collectAsState()
    val compareProductIds by viewModel.compareProductIds.collectAsState()
    val isSellModalOpen by viewModel.isSellModalOpen.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val isCartOpen by viewModel.isCartOpen.collectAsState()
    val isCheckoutConfirmationOpen by viewModel.isCheckoutConfirmationOpen.collectAsState()
    val isCheckoutSuccess by viewModel.isCheckoutSuccess.collectAsState()
    val lastPlacedOrder by viewModel.lastPlacedOrder.collectAsState()
    val lastPlacedOrders by viewModel.lastPlacedOrders.collectAsState()
    val orderForIssueReport by viewModel.orderForIssueReport.collectAsState()
    val bannerFeedback by viewModel.bannerFeedback.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    var showAdvancedFilters by remember { mutableStateOf(false) }

    val approvedStores = stores.filter { it.status == StoreVerificationStatus.APPROVED }
    val filteredProducts = viewModel.getFilteredProducts()
    val availableBrands = viewModel.getAvailableBrands()
    val availableLocations = viewModel.getAvailableLocations()
    val activeFilterCount = viewModel.getActiveFilterCount()
    val cartBreakdown = viewModel.getCartBreakdown()
    val activeSellerStore = stores.find { it.id == activeSellerStoreId } ?: stores.firstOrNull()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                SolarfixHeader(
                    title = "SolarFix Marketplace",
                    subtitle = "Multi-Vendor Solar Stores • 48H Escrow & Installments",
                    currentCurrency = currency,
                    onCurrencySelected = { viewModel.setCurrency(it) },
                    cartItemCount = viewModel.getTotalCartItemsCount(),
                    onCartClicked = { viewModel.openCart(true) }
                )

                // Role / Portal Switcher inside SolarFix Marketplace: [Shop] | [Become a Seller] | [Seller Hub]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketplacePortalMode.values().forEach { mode ->
                        val isSelected = portalMode == mode
                        Button(
                            onClick = { viewModel.setPortalMode(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("portal_mode_${mode.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    MarketplacePortalMode.CUSTOMER_SHOP -> Icons.Default.ShoppingBag
                                    MarketplacePortalMode.SELLER_REGISTRATION -> Icons.Default.AppRegistration
                                    MarketplacePortalMode.SELLER_DASHBOARD -> Icons.Default.Storefront
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Feedback Banner
                AnimatedVisibility(visible = bannerFeedback != null) {
                    bannerFeedback?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CleanGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.showFeedback(null) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Customer Sub-Navigation & Search (Only when in Customer Shop & Catalog view)
                if (portalMode == MarketplacePortalMode.CUSTOMER_SHOP) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.CATALOG,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                label = { Text("Shop All", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_catalog")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.SELLERS_DIRECTORY || customerSubScreen == CustomerSubScreen.STOREFRONT,
                                onClick = { viewModel.openSellersDirectory() },
                                label = { Text("Sellers (${approvedStores.size})", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_sellers")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.ORDERS_AND_TRACKING,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.ORDERS_AND_TRACKING) },
                                label = { Text("Orders & Track (${orders.size})", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_orders")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.COMPARE,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.COMPARE) },
                                label = { Text("Compare (${compareProductIds.size})", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_compare")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.WISHLIST,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.WISHLIST) },
                                label = { Text("Wishlist (${savedProductIds.size})", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_wishlist")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.ADDRESS_BOOK,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.ADDRESS_BOOK) },
                                label = { Text("Addresses (${addresses.size})", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_addresses")
                            )
                        }
                        item {
                            FilterChip(
                                selected = customerSubScreen == CustomerSubScreen.PAYMENT_AUDIT,
                                onClick = { viewModel.openCustomerSubScreen(CustomerSubScreen.PAYMENT_AUDIT) },
                                label = { Text("Payment & Escrow", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("customer_nav_payment_audit")
                            )
                        }
                    }

                    if (customerSubScreen == CustomerSubScreen.CATALOG) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search panels, batteries, inverters, brands, models, or seller stores...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("marketplace_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.openSellModal(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("marketplace_sell_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Sell Item", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = { viewModel.openSellerRegistration() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("marketplace_become_seller_button")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CleanGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register as Seller", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            FilterChip(
                                selected = filterCertifiedOnly,
                                onClick = { viewModel.toggleCertifiedFilter() },
                                label = { Text("✓ VERIFIED", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    if (filterCertifiedOnly) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                modifier = Modifier.testTag("filter_certified_chip")
                            )

                            FilterChip(
                                selected = showAdvancedFilters || activeFilterCount > 0,
                                onClick = { showAdvancedFilters = !showAdvancedFilters },
                                label = {
                                    Text(
                                        text = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = "Toggle Filters", modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.testTag("shopping_filters_toggle_button")
                            )
                        }

                        // Expandable Shopping Filters Panel: Price Range, Brand, Location, Condition & Sort
                        AnimatedVisibility(visible = showAdvancedFilters) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .testTag("advanced_filters_panel")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Filter by Price, Brand, Location & Condition (${filteredProducts.size} results)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (activeFilterCount > 0) {
                                            TextButton(
                                                onClick = { viewModel.resetAllFilters() },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier
                                                    .height(26.dp)
                                                    .testTag("reset_all_filters_button")
                                            ) {
                                                Text("Reset All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // 1. Price Range Filters
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Text("Price:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        items(PriceRangeFilter.values()) { range ->
                                            FilterChip(
                                                selected = selectedPriceRange == range,
                                                onClick = { viewModel.selectPriceRange(range) },
                                                label = { Text(range.label, fontSize = 11.sp) },
                                                modifier = Modifier.testTag("price_filter_chip_${range.name.lowercase()}")
                                            )
                                        }
                                    }

                                    // 2. Brand Filters
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Text("Brand:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        item {
                                            FilterChip(
                                                selected = selectedBrand == null,
                                                onClick = { viewModel.selectBrand(null) },
                                                label = { Text("All Brands", fontSize = 11.sp) },
                                                modifier = Modifier.testTag("brand_filter_chip_all")
                                            )
                                        }
                                        items(availableBrands) { brand ->
                                            FilterChip(
                                                selected = selectedBrand.equals(brand, ignoreCase = true),
                                                onClick = {
                                                    viewModel.selectBrand(if (selectedBrand.equals(brand, ignoreCase = true)) null else brand)
                                                },
                                                label = { Text(brand, fontSize = 11.sp) },
                                                modifier = Modifier.testTag("brand_filter_chip_${brand.lowercase().replace(" ", "_")}")
                                            )
                                        }
                                    }

                                    // 3. Location Filters
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Text("Location:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        item {
                                            FilterChip(
                                                selected = selectedLocation == null,
                                                onClick = { viewModel.selectLocation(null) },
                                                label = { Text("All Locations", fontSize = 11.sp) },
                                                modifier = Modifier.testTag("location_filter_chip_all")
                                            )
                                        }
                                        items(availableLocations) { loc ->
                                            FilterChip(
                                                selected = selectedLocation.equals(loc, ignoreCase = true),
                                                onClick = {
                                                    viewModel.selectLocation(if (selectedLocation.equals(loc, ignoreCase = true)) null else loc)
                                                },
                                                label = { Text(loc, fontSize = 11.sp) },
                                                modifier = Modifier.testTag("location_filter_chip_${loc.lowercase().replace(" ", "_")}")
                                            )
                                        }
                                    }

                                    // 4. Condition & Sort Options
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Text("Condition & Sort:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        listOf("ALL" to "All Conditions", "Brand New" to "Brand New", "Used" to "Used").forEach { (condKey, condLabel) ->
                                            item {
                                                FilterChip(
                                                    selected = selectedConditionFilter.equals(condKey, ignoreCase = true),
                                                    onClick = { viewModel.selectConditionFilter(condKey) },
                                                    label = { Text(condLabel, fontSize = 11.sp) },
                                                    modifier = Modifier.testTag("condition_filter_chip_${condKey.lowercase().replace(" ", "_")}")
                                                )
                                            }
                                        }
                                        items(ProductSortOption.values()) { sortOpt ->
                                            FilterChip(
                                                selected = selectedSortOption == sortOpt,
                                                onClick = { viewModel.selectSortOption(sortOpt) },
                                                label = { Text(sortOpt.label, fontSize = 11.sp) },
                                                modifier = Modifier.testTag("sort_filter_chip_${sortOpt.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Product Categories Filter Row (All 13 Solar Categories + All)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ProductCategory.values()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = { Text(category.label, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (portalMode) {
                MarketplacePortalMode.SELLER_REGISTRATION -> {
                    SellerRegistrationContent(
                        viewModel = viewModel,
                        onBackToMarketplace = { viewModel.setPortalMode(MarketplacePortalMode.CUSTOMER_SHOP) }
                    )
                }

                MarketplacePortalMode.SELLER_DASHBOARD -> {
                    SellerDashboardContent(
                        viewModel = viewModel,
                        onSwitchToCustomerShop = { viewModel.setPortalMode(MarketplacePortalMode.CUSTOMER_SHOP) },
                        onOpenSellerRegistration = { viewModel.setPortalMode(MarketplacePortalMode.SELLER_REGISTRATION) }
                    )
                }

                MarketplacePortalMode.CUSTOMER_SHOP -> {
                    when (customerSubScreen) {
                        CustomerSubScreen.SELLERS_DIRECTORY -> {
                            VerifiedSellersDirectoryView(
                                stores = approvedStores.map { it.toPublicProfile() },
                                allProducts = allProducts.filter {
                                    it.isActive && it.moderationStatus == ProductModerationStatus.APPROVED
                                },
                                currency = currency,
                                onBackToMarketplace = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onOpenSellerStore = { storeIdOrRoute -> viewModel.openSellerStoreInsideApp(storeIdOrRoute) }
                            )
                        }

                        CustomerSubScreen.STOREFRONT -> {
                            val store = stores.find { it.id == selectedStoreId } ?: approvedStores.firstOrNull()
                            if (store != null) {
                                val storeProds = allProducts.filter {
                                    it.storeId == store.id &&
                                        it.isActive &&
                                        it.moderationStatus == ProductModerationStatus.APPROVED
                                }
                                val storeRevs = reviews.filter { it.storeId == store.id }
                                // Strictly pass PublicStoreProfile so customers never have access to KYC or bank details
                                InternalSellerStorefrontView(
                                    store = store.toPublicProfile(),
                                    storeProducts = storeProds,
                                    storeReviews = storeRevs,
                                    currency = currency,
                                    savedProductIds = savedProductIds,
                                    compareProductIds = compareProductIds,
                                    onBackToCatalog = { viewModel.closeSellerStore() },
                                    onOpenSellersDirectory = { viewModel.openSellersDirectory() },
                                    onOpenComparisonScreen = { viewModel.openCustomerSubScreen(CustomerSubScreen.COMPARE) },
                                    onSelectProduct = { viewModel.selectProduct(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onToggleCompare = { viewModel.toggleCompareProduct(it) },
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onBuyNowDirect = { prod ->
                                        viewModel.addToCart(prod)
                                        viewModel.checkoutOrder()
                                    }
                                )
                            }
                        }

                        CustomerSubScreen.COMPARE -> {
                            val comparedList = allProducts.filter { compareProductIds.contains(it.id) }
                            ProductComparisonView(
                                comparedProducts = comparedList,
                                currency = currency,
                                onBack = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onRemoveFromCompare = { viewModel.toggleCompareProduct(it) },
                                onClearAll = { viewModel.clearComparison() },
                                onAddToCart = { viewModel.addToCart(it) },
                                onOpenStore = { viewModel.openSellerStoreInsideApp(it) }
                            )
                        }

                        CustomerSubScreen.WISHLIST -> {
                            val savedList = allProducts.filter { savedProductIds.contains(it.id) }
                            CustomerWishlistView(
                                savedProducts = savedList,
                                currency = currency,
                                onBack = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onSelectProduct = { viewModel.selectProduct(it) },
                                onRemoveSaved = { viewModel.toggleFavorite(it) },
                                onMoveToCart = { viewModel.addToCart(it) },
                                onMoveAllToCart = { viewModel.moveAllWishlistToCart() },
                                onOpenStore = { viewModel.openSellerStoreInsideApp(it) }
                            )
                        }

                        CustomerSubScreen.ORDERS_AND_TRACKING -> {
                            CustomerOrdersAndTrackingView(
                                orders = orders,
                                disputes = disputes,
                                currency = currency,
                                onBack = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onOpenStore = { viewModel.openSellerStoreInsideApp(it) },
                                onReportIssue = { viewModel.openIssueReportModal(it) }
                            )
                        }

                        CustomerSubScreen.ADDRESS_BOOK -> {
                            DeliveryAddressManagerView(
                                addresses = addresses,
                                onBack = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onAddAddress = { label, rec, ph, st, ct, stName, def ->
                                    viewModel.addAddress(label, rec, ph, st, ct, stName, def)
                                },
                                onSetDefault = { viewModel.setDefaultAddress(it) },
                                onDelete = { viewModel.deleteAddress(it) }
                            )
                        }

                        CustomerSubScreen.PAYMENT_AUDIT -> {
                            val gatewayTransactions by viewModel.gatewayTransactions.collectAsState()
                            val reconciliationRecords by viewModel.reconciliationRecords.collectAsState()
                            val refundRecords by viewModel.refundRecords.collectAsState()
                            val sellerSubaccounts by viewModel.sellerSubaccounts.collectAsState()

                            PaymentGatewayReconciliationView(
                                transactions = gatewayTransactions,
                                reconciliationRecords = reconciliationRecords,
                                refundRecords = refundRecords,
                                sellerSubaccounts = sellerSubaccounts.values.toList(),
                                currency = currency,
                                onBack = { viewModel.openCustomerSubScreen(CustomerSubScreen.CATALOG) },
                                onRunReconciliation = { viewModel.generateReconciliationReport() },
                                onSimulateWebhook = { ref, evt -> viewModel.simulateIncomingWebhook(ref, evt) },
                                onProcessRefund = { ref, amount, reason -> viewModel.processRefundForOrder(ref, amount, reason) }
                            )
                        }

                        CustomerSubScreen.CATALOG -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("marketplace_product_grid")
                            ) {
                                // Verified Seller Stores Carousel (Full Span)
                                item(span = { GridItemSpan(2) }) {
                                    VerifiedStoresCarouselSection(
                                        stores = approvedStores.map { it.toPublicProfile() },
                                        onStoreClicked = { storeId -> viewModel.openSellerStoreInsideApp(storeId) },
                                        onViewAllSellers = { viewModel.openSellersDirectory() }
                                    )
                                }

                                if (filteredProducts.isEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No solar equipment matching your filter", style = MaterialTheme.typography.titleMedium)
                                            TextButton(
                                                onClick = { viewModel.resetAllFilters() }
                                            ) {
                                                Text("Reset All Filters")
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredProducts) { product ->
                                        MultiVendorMarketplaceCard(
                                            product = product,
                                            currency = currency,
                                            isSaved = savedProductIds.contains(product.id),
                                            isCompared = compareProductIds.contains(product.id),
                                            onCardClick = { viewModel.selectProduct(product) },
                                            onStoreClick = { viewModel.openSellerStoreInsideApp(product.storeId) },
                                            onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                            onToggleCompare = { viewModel.toggleCompareProduct(product.id) },
                                            onAddToCart = { viewModel.addToCart(product) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Upload / Sell Solar Hardware Modal
            if (isSellModalOpen) {
                SellSolarHardwareDialog(
                    activeStore = activeSellerStore,
                    onDismiss = { viewModel.openSellModal(false) },
                    onPublishListing = { title, category, brand, priceNgn, location, sellerName, isCertified, condition, specs, desc, photoBitmap, badge, warranty, stock ->
                        viewModel.addNewListing(
                            title = title,
                            category = category,
                            brand = brand,
                            priceNgn = priceNgn,
                            location = location,
                            sellerName = sellerName,
                            isCertifiedSeller = isCertified,
                            condition = condition,
                            specs = specs,
                            description = desc,
                            photoBitmap = photoBitmap,
                            certificationBadge = badge,
                            storeId = activeSellerStore?.id ?: "store_nekes",
                            warrantyPeriod = warranty,
                            stockCount = stock
                        )
                    }
                )
            }

            // 2. Product Detail Sheet (with Related Products, Seller Store Link, Reviews, Wishlist & Compare)
            if (selectedProduct != null) {
                val prod = selectedProduct!!
                val prodReviews = viewModel.getReviewsForProduct(prod.id)
                val relatedProds = viewModel.getRelatedProducts(prod)
                ProductDetailModal(
                    product = prod,
                    currency = currency,
                    reviews = prodReviews,
                    onDismiss = { viewModel.selectProduct(null) },
                    onVisitSellerStore = { storeId -> viewModel.openSellerStoreInsideApp(storeId) },
                    onSubmitReview = { name, rating, comment ->
                        viewModel.submitReview(prod.id, name, rating, comment)
                    },
                    onAddToCart = { chosenPlan ->
                        viewModel.addToCart(prod, chosenPlan)
                        viewModel.selectProduct(null)
                    },
                    relatedProducts = relatedProds,
                    isSaved = savedProductIds.contains(prod.id),
                    isComparing = compareProductIds.contains(prod.id),
                    onToggleFavorite = { viewModel.toggleFavorite(prod.id) },
                    onToggleCompare = { viewModel.toggleCompareProduct(prod.id) },
                    onSelectRelatedProduct = { related -> viewModel.selectProduct(related) },
                    onAddRelatedToCart = { related -> viewModel.addToCart(related) }
                )
            }

            // 3. Multi-Seller Shopping Cart Modal
            if (isCartOpen) {
                CartSheet(
                    cartItems = cartItems,
                    currency = currency,
                    totalPriceFormatted = currency.formatPrice(viewModel.getTotalCartPriceNgn()),
                    onDismiss = { viewModel.openCart(false) },
                    onIncrement = { viewModel.updateCartItemQuantity(it, 1) },
                    onDecrement = { viewModel.updateCartItemQuantity(it, -1) },
                    onCheckout = { viewModel.checkoutOrder() },
                    cartBreakdown = cartBreakdown,
                    onRemoveItem = { viewModel.removeCartItem(it) },
                    onClearCart = { viewModel.clearCart() },
                    onVisitSellerStore = { storeId -> viewModel.openSellerStoreInsideApp(storeId) }
                )
            }

            // 4. Pre-Purchase Confirmation & Secure Payment Checkout Modal
            if (isCheckoutConfirmationOpen) {
                PrePurchaseConfirmationAndPaymentDialog(
                    cartItems = cartItems,
                    addresses = addresses,
                    currency = currency,
                    allowInstallments = settings.allowInstallmentPlans,
                    onDismiss = { viewModel.openCheckoutConfirmation(false) },
                    onManageAddresses = {
                        viewModel.openCheckoutConfirmation(false)
                        viewModel.openCustomerSubScreen(CustomerSubScreen.ADDRESS_BOOK)
                    },
                    onConfirmPayment = { customerName, customerEmail, address, paymentPlan, paymentMethod, paymentReference ->
                        viewModel.confirmAndPlaceOrder(customerName, customerEmail, address, paymentPlan, paymentMethod, paymentReference)
                    },
                    cartBreakdown = cartBreakdown,
                    allProducts = allProducts,
                    onRequestEquipmentVerification = { productId, reqType, notes ->
                        viewModel.requestExpensiveEquipmentVerification(productId, reqType, notes)
                    },
                    onRefreshCartPrices = {
                        viewModel.refreshCartPricesAndStock()
                    }
                )
            }

            // 5. Report Damaged / Missing / Incorrect Product Modal
            if (orderForIssueReport != null) {
                ReportOrderIssueDialog(
                    order = orderForIssueReport!!,
                    onDismiss = { viewModel.openIssueReportModal(null) },
                    onSubmitDispute = { issueType, desc ->
                        viewModel.submitOrderDispute(orderForIssueReport!!.id, issueType, desc)
                    }
                )
            }

            // 6. Checkout Success Confirmation with Live Multi-Seller Order Tracking
            if (isCheckoutSuccess) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCheckoutSuccess() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CleanGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (lastPlacedOrders.size > 1) {
                                "${lastPlacedOrders.size} Seller Orders Placed!"
                            } else {
                                "Order Placed Successfully!"
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    text = {
                        val ord = lastPlacedOrder
                        val multiSummary = if (lastPlacedOrders.size > 1) {
                            lastPlacedOrders.joinToString("\n") { o ->
                                "• #${o.id} (${o.primaryStoreName}) — ${currency.formatPrice(o.totalAmountNgn)} [Track: ${o.trackingNumber}]"
                            }
                        } else null

                        Text(
                            text = when {
                                multiSummary != null -> {
                                    "Your multi-seller cart has been split into ${lastPlacedOrders.size} separate seller fulfilment orders under 48-Hour SolarFix Escrow:\n\n$multiSummary"
                                }
                                ord != null -> {
                                    "Order #${ord.id} with ${ord.primaryStoreName} is confirmed under 48-Hour Escrow Protection.\n\nTracking Number: ${ord.trackingNumber}"
                                }
                                else -> {
                                    "Your solar equipment request has been transmitted with 48-Hour Escrow Protection."
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissCheckoutSuccess()
                                viewModel.openCustomerSubScreen(CustomerSubScreen.ORDERS_AND_TRACKING)
                            },
                            modifier = Modifier.testTag("dismiss_checkout_success_button")
                        ) {
                            Text("Track Order Live")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun VerifiedStoresCarouselSection(
    stores: List<PublicStoreProfile>,
    onStoreClicked: (String) -> Unit,
    onViewAllSellers: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Verified Solar Stores Inside SolarFix",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Marketplace → Sellers • Tap any store to shop in-app",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onViewAllSellers,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("view_all_sellers_button")
            ) {
                Text(
                    text = "View All Sellers (${stores.size}) →",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(stores) { store ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .width(265.dp)
                        .clickable { onStoreClicked(store.id) }
                        .testTag("store_carousel_card_${store.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StoreLogoEmblem(
                                storeName = store.storeName,
                                logoBitmap = store.logoBitmap,
                                brandColorHex = store.storeBrandColorHex,
                                sizeDp = 28
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = store.storeName,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = CleanGreen.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = CleanGreen, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "VERIFIED SELLER",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = CleanGreen,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = store.tagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${store.cityStateLocation} • ${store.storeOpeningDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★ ${store.rating} (${store.reviewsCount}) • ${store.successfulSalesCount} Sales",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Open Store →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiVendorMarketplaceCard(
    product: MarketplaceProduct,
    currency: Currency,
    isSaved: Boolean,
    isCompared: Boolean,
    onCardClick: () -> Unit,
    onStoreClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit,
    onAddToCart: () -> Unit
) {
    val monthlyInstallment = product.basePriceNgn / product.maxInstallmentMonths.coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("marketplace_card_${product.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (product.photoBitmap != null) {
                    Image(
                        bitmap = product.photoBitmap.asImageBitmap(),
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CategoryHeroIllustration(
                        category = product.category,
                        powerRating = product.powerRating
                    )
                }

                // VERIFIED SELLER Badge on Product Listing
                if (product.isCertifiedSeller) {
                    Surface(
                        color = Color(0xFF15803D),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VERIFIED SELLER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                // Compare & Wishlist Action Icons (Top Right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleCompare,
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isCompared) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Compare Product",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save Item",
                            tint = if (isSaved) Color.Red else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Warranty Pill (Bottom Left)
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = product.warrantyPeriod,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // STRICT RULE: Only show SOLARFIX INSPECTED badge if an actual independent SolarFix inspection has taken place!
                if (product.hasActualSolarFixInspection) {
                    Surface(
                        color = Color(0xFF1D4ED8),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .testTag("card_solarfix_inspected_${product.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "SOLARFIX INSPECTED",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currency.formatPrice(product.basePriceNgn),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Surface(
                        color = if (product.normalizedCondition == "Brand New") CleanGreen.copy(alpha = 0.14f) else Color(0xFFF59E0B).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.normalizedCondition,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (product.normalizedCondition == "Brand New") CleanGreen else Color(0xFFD97706),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (product.installmentAllowed) {
                    Text(
                        text = "${currency.formatPrice(monthlyInstallment)}/mo (${product.maxInstallmentMonths}m Plan)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CleanGreen,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${product.brand} • Model: ${product.effectiveModelNumber} • ${product.stockCount} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Clickable Seller Store Link (Opens Storefront Inside SolarFix)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStoreClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = CleanGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = product.sellerName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CleanGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = onAddToCart,
                    enabled = product.inStock && product.stockCount > 0,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (product.stockCount > 0) "Add to Cart" else "Sold Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
