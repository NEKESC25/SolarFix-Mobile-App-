package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.CleanGreen

/**
 * In-App Verified Sellers Directory Screen ("Marketplace → Sellers").
 * Lists every approved seller store inside SolarFix with Store Cover Image, Store Logo,
 * Unique Store Name, Verification Badge, Location, Contact Info, Available Products,
 * Product Categories, Customer Ratings, Successful Sales, and Store Opening Date.
 */
@Composable
fun VerifiedSellersDirectoryView(
    stores: List<PublicStoreProfile>,
    allProducts: List<MarketplaceProduct>,
    currency: Currency,
    onBackToMarketplace: () -> Unit,
    onOpenSellerStore: (String) -> Unit
) {
    BackHandler { onBackToMarketplace() }

    var storeSearchQuery by remember { mutableStateOf("") }
    val filteredStores = remember(stores, storeSearchQuery) {
        val q = storeSearchQuery.trim().lowercase()
        if (q.isEmpty()) stores
        else stores.filter {
            it.storeName.lowercase().contains(q) ||
                it.cityStateLocation.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.tagline.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sellers_directory_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Breadcrumb Navigation Header: Marketplace → Sellers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Marketplace",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onBackToMarketplace() }
                        )
                        Text(
                            text = "  →  ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sellers (${stores.size} Verified Stores)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = CleanGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "In-App Stores Only",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CleanGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = storeSearchQuery,
                    onValueChange = { storeSearchQuery = it },
                    placeholder = { Text("Search seller stores by name, city, or equipment...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (storeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { storeSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sellers_directory_search_input")
                )
            }
        }

        items(filteredStores) { store ->
            val storeProds = allProducts.filter {
                it.storeId == store.id && it.moderationStatus == ProductModerationStatus.APPROVED
            }
            val categories = storeProds.map { it.category.label }.distinct()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSellerStore(store.id) }
                    .testTag("directory_store_card_${store.id}")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Store Cover Banner + Overlapping Store Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        if (store.coverImageBitmap != null) {
                            Image(
                                bitmap = store.coverImageBitmap.asImageBitmap(),
                                contentDescription = "${store.storeName} Cover Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF0B243B),
                                                Color(store.storeBrandColorHex),
                                                Color(0xFF065F46)
                                            )
                                        )
                                    )
                            )
                        }

                        // Route Badge (Top Left) + VERIFIED SELLER Badge (Top Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = store.inAppBreadcrumb,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            if (store.isVerifiedSeller) {
                                Surface(
                                    color = Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "VERIFIED SELLER",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // Store Logo Avatar + Title Row at Bottom of Cover
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            StoreLogoEmblem(
                                storeName = store.storeName,
                                logoBitmap = store.logoBitmap,
                                brandColorHex = store.storeBrandColorHex,
                                sizeDp = 44
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = store.storeName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${store.cityStateLocation} • Opened ${store.storeOpeningDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Store Details Body
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = store.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Metrics Row: Rating | Successful Sales | Available Products | Contact
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★ ${store.rating} (${store.reviewsCount} reviews)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "✓ ${store.successfulSalesCount} Successful Sales",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CleanGreen
                            )
                            Text(
                                text = "${storeProds.size} Products",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Categories Carried by Store
                        if (categories.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { catLabel ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = catLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contact: ${store.publicContactPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onOpenSellerStore(store.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("enter_store_btn_${store.id}")
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Store Inside SolarFix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreLogoEmblem(
    storeName: String,
    logoBitmap: android.graphics.Bitmap?,
    brandColorHex: Long,
    sizeDp: Int = 52
) {
    val initials = remember(storeName) {
        storeName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "SF" }
    }

    Surface(
        shape = CircleShape,
        color = Color(brandColorHex),
        modifier = Modifier
            .size(sizeDp.dp)
            .border(2.dp, Color.White, CircleShape)
    ) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap.asImageBitmap(),
                contentDescription = "$storeName Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    fontSize = (sizeDp * 0.36f).sp
                )
            }
        }
    }
}

/**
 * Internal Seller Storefront Screen inside SolarFix navigation.
 * Route Example: Marketplace → Sellers → NEKES Solar Energy Store
 * Internal Route Identifier: marketplace/sellers/nekes-solar-energy-store
 * Opens natively inside SolarFix without any external website or browser.
 * Uses [PublicStoreProfile] to strictly protect seller KYC documents and bank details from customers.
 */
@Composable
fun InternalSellerStorefrontView(
    store: PublicStoreProfile,
    storeProducts: List<MarketplaceProduct>,
    storeReviews: List<ProductReview>,
    currency: Currency,
    savedProductIds: Set<String>,
    compareProductIds: Set<String>,
    onBackToCatalog: () -> Unit,
    onOpenSellersDirectory: () -> Unit = onBackToCatalog,
    onOpenComparisonScreen: () -> Unit = {},
    onSelectProduct: (MarketplaceProduct) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleCompare: (String) -> Unit,
    onAddToCart: (MarketplaceProduct) -> Unit,
    onBuyNowDirect: (MarketplaceProduct) -> Unit = onAddToCart
) {
    BackHandler { onBackToCatalog() }

    var selectedStoreTab by remember { mutableStateOf(0) } // 0 = Products & Categories, 1 = Store Profile & Contact, 2 = Customer Reviews
    var selectedStoreCategory by remember { mutableStateOf(ProductCategory.ALL) }
    var inStoreSearchQuery by remember { mutableStateOf("") }

    // Available product categories carried by this seller store
    val availableStoreCategories = remember(storeProducts) {
        listOf(ProductCategory.ALL) + storeProducts.map { it.category }.distinct()
    }

    val filteredStoreProducts = remember(storeProducts, selectedStoreCategory, inStoreSearchQuery) {
        val q = inStoreSearchQuery.trim().lowercase()
        storeProducts.filter { prod ->
            val matchesCat = (selectedStoreCategory == ProductCategory.ALL) || (prod.category == selectedStoreCategory)
            val matchesQuery = q.isEmpty() ||
                prod.title.lowercase().contains(q) ||
                prod.brand.lowercase().contains(q) ||
                prod.powerRating.lowercase().contains(q)
            matchesCat && matchesQuery
        }
    }

    val comparedInThisStore = remember(storeProducts, compareProductIds) {
        storeProducts.filter { compareProductIds.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("internal_seller_storefront_${store.id}"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. In-App Store Route Breadcrumb Bar: Marketplace → Sellers → NEKES Solar Energy Store
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storefront_route_breadcrumb")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onBackToCatalog() }
                                    .testTag("back_from_storefront_button")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marketplace",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onBackToCatalog() }
                            )
                            Text(" → ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "Sellers",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onOpenSellersDirectory() }
                            )
                            Text(" → ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = store.storeName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "In-App Route: ${store.inAppStoreRoute} (ID: ${store.id})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        if (store.isVerifiedSeller) {
                            Surface(
                                color = CleanGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("storefront_verified_seller_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified Seller",
                                        tint = CleanGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "VERIFIED SELLER",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = CleanGreen,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Store Cover Image + Store Logo + All 12 Storefront Attributes Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Store Cover Image Banner + Store Logo Emblem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    ) {
                        if (store.coverImageBitmap != null) {
                            Image(
                                bitmap = store.coverImageBitmap.asImageBitmap(),
                                contentDescription = "${store.storeName} Cover Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF0B243B),
                                                Color(store.storeBrandColorHex),
                                                Color(0xFF1E4E3A)
                                            )
                                        )
                                    )
                            )
                        }

                        // Dark gradient scrim for legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Verification Badge + Store Opening Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (store.isVerifiedSeller) Color(0xFFF59E0B) else Color.Gray,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        if (store.isVerifiedSeller) {
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = if (store.isVerifiedSeller) "VERIFIED SELLER" else store.accreditationBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.Black
                                        )
                                    }
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Opened: ${store.storeOpeningDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Bottom Row: Store Logo + Unique Store Name + Tagline
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StoreLogoEmblem(
                                    storeName = store.storeName,
                                    logoBitmap = store.logoBitmap,
                                    brandColorHex = store.storeBrandColorHex,
                                    sizeDp = 56
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = store.storeName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = store.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Store Metrics & Quick Summary Strip
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 4 Key Store KPIs: Rating & Reviews | Successful Sales | Available Products | Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StoreKpiPill(
                                icon = Icons.Default.Star,
                                iconTint = Color(0xFFF59E0B),
                                title = "${store.rating} (${store.reviewsCount})",
                                subtitle = "Ratings & Reviews"
                            )
                            StoreKpiPill(
                                icon = Icons.Default.TaskAlt,
                                iconTint = CleanGreen,
                                title = "${store.successfulSalesCount} Sales",
                                subtitle = "Successful Orders"
                            )
                            StoreKpiPill(
                                icon = Icons.Default.Inventory2,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = "${storeProducts.size} Items",
                                subtitle = "In Store"
                            )
                        }

                        HorizontalDivider()

                        // Business Location & Contact Information Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = CleanGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = store.cityStateLocation,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = store.publicContactPhone,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. In-Store Price Comparison Bar (Active whenever customer selects products to compare)
        if (compareProductIds.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("in_store_comparison_bar")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "In-App Price & Spec Comparison (${compareProductIds.size}/4 Selected)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Button(
                                onClick = onOpenComparisonScreen,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("storefront_open_compare_button")
                            ) {
                                Text("Compare Side-by-Side", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (comparedInThisStore.isNotEmpty()) {
                            Text(
                                text = comparedInThisStore.joinToString("  |  ") { "${it.title.take(22)}: ${currency.formatPrice(it.basePriceNgn)}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 4. Store Sub-navigation Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStoreTab == 0,
                    onClick = { selectedStoreTab = 0 },
                    label = { Text("Products (${storeProducts.size})", fontSize = 12.sp) },
                    modifier = Modifier.testTag("store_tab_products")
                )
                FilterChip(
                    selected = selectedStoreTab == 1,
                    onClick = { selectedStoreTab = 1 },
                    label = { Text("Store Profile & Contact", fontSize = 12.sp) },
                    modifier = Modifier.testTag("store_tab_profile")
                )
                FilterChip(
                    selected = selectedStoreTab == 2,
                    onClick = { selectedStoreTab = 2 },
                    label = { Text("Reviews (${storeReviews.size})", fontSize = 12.sp) },
                    modifier = Modifier.testTag("store_tab_reviews")
                )
            }
        }

        when (selectedStoreTab) {
            0 -> {
                // In-Store Search & Product Categories Filter Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inStoreSearchQuery,
                            onValueChange = { inStoreSearchQuery = it },
                            placeholder = { Text("Search within ${store.storeName}...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (inStoreSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { inStoreSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("in_store_search_input")
                        )

                        Text(
                            text = "Store Product Categories (${availableStoreCategories.size - 1} Categories)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableStoreCategories) { cat ->
                                val countForCat = if (cat == ProductCategory.ALL) {
                                    storeProducts.size
                                } else {
                                    storeProducts.count { it.category == cat }
                                }
                                FilterChip(
                                    selected = selectedStoreCategory == cat,
                                    onClick = { selectedStoreCategory = cat },
                                    label = { Text("${cat.label} ($countForCat)", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("store_category_chip_${cat.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                if (filteredStoreProducts.isEmpty()) {
                    item {
                        Text(
                            text = "No equipment matching your filter in ${store.storeName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(filteredStoreProducts) { product ->
                        StoreProductListItemCard(
                            product = product,
                            currency = currency,
                            isSaved = savedProductIds.contains(product.id),
                            isCompared = compareProductIds.contains(product.id),
                            onSelect = { onSelectProduct(product) },
                            onToggleFavorite = { onToggleFavorite(product.id) },
                            onToggleCompare = { onToggleCompare(product.id) },
                            onAddToCart = { onAddToCart(product) },
                            onBuyNowDirect = { onBuyNowDirect(product) }
                        )
                    }
                }
            }

            1 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("About ${store.storeName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(store.description, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                            Text("Store Contact & Location Information", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("• Business Location: ${store.cityStateLocation}", style = MaterialTheme.typography.bodySmall)
                            Text("• Store Support Phone: ${store.publicContactPhone}", style = MaterialTheme.typography.bodySmall)
                            Text("• Store Support Email: ${store.publicContactEmail}", style = MaterialTheme.typography.bodySmall)
                            Text("• In-App Store Route: ${store.inAppBreadcrumb}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("• Internal Store Identifier: ${store.id}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Text("Warranty & Escrow Protection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(store.warrantyPolicy, style = MaterialTheme.typography.bodySmall, color = CleanGreen)
                            HorizontalDivider()
                            Text("Public Store Verification Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("• Verification Badge: ${if (store.isVerifiedSeller) "VERIFIED SELLER (Approved by SolarFix)" else "Pending Verification"}", style = MaterialTheme.typography.bodySmall, color = CleanGreen)
                            Text("• Accreditation: ${store.accreditationBadge}", style = MaterialTheme.typography.bodySmall)
                            Text("• Successful Sales Completed: ${store.successfulSalesCount} orders", style = MaterialTheme.typography.bodySmall)
                            Text("• Store Opening Date: ${store.storeOpeningDate}", style = MaterialTheme.typography.bodySmall)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = CleanGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Seller identity documents and bank settlement records are verified by SolarFix Compliance and kept strictly confidential.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                if (storeReviews.isEmpty()) {
                    item {
                        Text("No customer reviews yet for this store.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(storeReviews) { rev ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.customerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("★".repeat(rev.rating), color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                }
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                if (!rev.sellerReply.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                "Response from ${store.storeName}:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(rev.sellerReply, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreKpiPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StoreProductListItemCard(
    product: MarketplaceProduct,
    currency: Currency,
    isSaved: Boolean,
    isCompared: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNowDirect: () -> Unit = onAddToCart
) {
    val monthlyInstallment = (product.basePriceNgn / product.maxInstallmentMonths.coerceAtLeast(1))
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("store_product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = product.category.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (product.isCertifiedSeller) {
                            Text(
                                text = "✓ VERIFIED SELLER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CleanGreen,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.powerRating} • ${product.voltage} • ${product.warrantyPeriod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onToggleCompare, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = "Compare Price & Specs",
                            tint = if (isCompared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isSaved) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currency.formatPrice(product.basePriceNgn),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.installmentAllowed) {
                        Text(
                            text = "or ${currency.formatPrice(monthlyInstallment)}/mo (${product.maxInstallmentMonths}x plan)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CleanGreen
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAddToCart,
                        enabled = product.inStock && product.stockCount > 0,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cart", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onBuyNowDirect,
                        enabled = product.inStock && product.stockCount > 0,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (product.stockCount > 0) "Buy Now" else "Sold Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Side-by-Side Solar Product Comparison View
 */
@Composable
fun ProductComparisonView(
    comparedProducts: List<MarketplaceProduct>,
    currency: Currency,
    onBack: () -> Unit,
    onRemoveFromCompare: (String) -> Unit,
    onClearAll: () -> Unit,
    onAddToCart: (MarketplaceProduct) -> Unit,
    onOpenStore: (String) -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("product_comparison_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Compare Solar Equipment (${comparedProducts.size}/4)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (comparedProducts.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
        }

        if (comparedProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products selected for comparison", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Tap the compare icon (⇄) on any solar panel, battery, or inverter card to compare specs side-by-side.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                comparedProducts.forEach { prod ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.width(250.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = prod.brand,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onRemoveFromCompare(prod.id) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = prod.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                minLines = 2
                            )
                            Text(
                                text = currency.formatPrice(prod.basePriceNgn),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider()
                            CompareSpecRow("Store", prod.sellerName, onClick = { onOpenStore(prod.storeId) })
                            CompareSpecRow("Category", prod.category.label)
                            CompareSpecRow("Power Rating", prod.powerRating)
                            CompareSpecRow("Voltage", prod.voltage)
                            CompareSpecRow("Warranty", prod.warrantyPeriod)
                            CompareSpecRow("Stock Left", "${prod.stockCount} units")
                            CompareSpecRow("Installment", if (prod.installmentAllowed) "Up to ${prod.maxInstallmentMonths} mos" else "Outright Only")
                            prod.specs.entries.take(3).forEach { (k, v) ->
                                CompareSpecRow(k, v)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onAddToCart(prod) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add to Cart")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareSpecRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Customer Orders, Live Tracking & Purchase History + Issue Reporting View
 */
@Composable
fun CustomerOrdersAndTrackingView(
    orders: List<MarketplaceOrder>,
    disputes: List<ProductDispute>,
    currency: Currency,
    onBack: () -> Unit,
    onOpenStore: (String) -> Unit,
    onReportIssue: (MarketplaceOrder) -> Unit
) {
    BackHandler { onBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("customer_orders_tracking_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "My Orders, Live Tracking & Purchase History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Protected by SolarFix 48-Hour Escrow & OEM Warranty",
                        style = MaterialTheme.typography.labelSmall,
                        color = CleanGreen
                    )
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No solar orders placed yet", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        } else {
            items(orders) { order ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Order #${order.id} • ${order.createdAt}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Store: ${order.primaryStoreName}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onOpenStore(order.primaryStoreId) }
                                )
                            }
                            Surface(
                                color = when (order.status) {
                                    OrderStatus.DELIVERED -> CleanGreen.copy(alpha = 0.18f)
                                    OrderStatus.DISPUTED, OrderStatus.REFUNDED -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = order.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (order.status) {
                                        OrderStatus.DELIVERED -> CleanGreen
                                        OrderStatus.DISPUTED, OrderStatus.REFUNDED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Multi-Seller Checkout Group & Separate Seller Fulfilment Breakdown (if applicable)
                        if (order.sellerFulfilments.size > 1) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Multi-Seller Checkout Group (${order.checkoutGroupId ?: "Split Order"}) • ${order.sellerFulfilments.size} Seller Fulfilments",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    order.sellerFulfilments.forEach { leg ->
                                        Text(
                                            text = "• ${leg.storeName}: Order #${leg.fulfilmentOrderId} (${currency.formatPrice(leg.totalSellerOrderAmountNgn)}) — Track: ${leg.trackingNumber}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        // Order Items
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.productTitle}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = currency.formatPrice(item.unitPriceNgn * item.quantity),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "Warranty: ${item.warrantyPeriod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CleanGreen
                            )
                        }

                        HorizontalDivider()

                        // CDcare-Style Live Order Tracking Stepper
                        Text(
                            text = "Tracking: ${order.trackingNumber} (Est: ${order.estimatedDelivery})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        LinearProgressIndicator(
                            progress = { (order.status.stepIndex / 6f).coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (order.status == OrderStatus.DISPUTED) MaterialTheme.colorScheme.error else CleanGreen
                        )

                        // Timeline Log
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            order.statusTimeline.forEach { event ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CleanGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(event, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Subtotal: ${currency.formatPrice(order.subtotalNgn)} + Delivery: ${currency.formatPrice(order.deliveryFeeNgn)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Seller Order Total: ${currency.formatPrice(order.totalAmountNgn)} (${order.paymentPlan.label})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "SolarFix Commission (${order.commissionPercent}%): ${currency.formatPrice(order.solarfixCommissionNgn)} • Deliver to: ${order.deliveryAddress.city}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CleanGreen
                                )
                            }

                            OutlinedButton(
                                onClick = { onReportIssue(order) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("report_order_issue_${order.id}")
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report Issue", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customer Wishlist & Saved Items View
 */
@Composable
fun CustomerWishlistView(
    savedProducts: List<MarketplaceProduct>,
    currency: Currency,
    onBack: () -> Unit,
    onSelectProduct: (MarketplaceProduct) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onMoveToCart: (MarketplaceProduct) -> Unit,
    onMoveAllToCart: () -> Unit = {},
    onOpenStore: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("customer_wishlist_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Saved Solar Wishlist (${savedProducts.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (savedProducts.isNotEmpty()) {
                    Button(
                        onClick = onMoveAllToCart,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("wishlist_move_all_to_cart_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add All to Cart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (savedProducts.isEmpty()) {
            item {
                Text("Your wishlist is empty. Tap the heart icon on any solar equipment to save it for later.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(savedProducts) { prod ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectProduct(prod) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "${prod.sellerName} • ${prod.warrantyPeriod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onOpenStore(prod.storeId) }
                            )
                            Text(currency.formatPrice(prod.basePriceNgn), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = CleanGreen)
                        }
                        Row {
                            IconButton(onClick = { onRemoveSaved(prod.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove")
                            }
                            Button(
                                onClick = { onMoveToCart(prod) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add to Cart", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customer Delivery Address Manager View
 */
@Composable
fun DeliveryAddressManagerView(
    addresses: List<DeliveryAddress>,
    onBack: () -> Unit,
    onAddAddress: (String, String, String, String, String, String, Boolean) -> Unit,
    onSetDefault: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    BackHandler { onBack() }

    var label by remember { mutableStateOf("Home / Site") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("Lagos State") }
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("delivery_address_manager_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("Delivery Addresses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Button(
                    onClick = { showForm = !showForm },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showForm) "Cancel" else "Add Address")
                }
            }
        }

        if (showForm) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("New Delivery Location", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Address Label (e.g. Home, Site)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street Address & Landmark") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Area") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                if (recipient.isNotBlank() && street.isNotBlank()) {
                                    onAddAddress(label, recipient, phone.ifBlank { "+234 800 000 0000" }, street, city.ifBlank { "Ikeja" }, stateName, true)
                                    showForm = false
                                    recipient = ""
                                    street = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Delivery Address")
                        }
                    }
                }
            }
        }

        items(addresses) { addr ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (addr.isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(addr.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (addr.isDefault) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = CleanGreen, shape = RoundedCornerShape(4.dp)) {
                                    Text("DEFAULT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("${addr.recipientName} • ${addr.phone}", style = MaterialTheme.typography.bodySmall)
                        Text("${addr.streetAddress}, ${addr.city}, ${addr.state}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        if (!addr.isDefault) {
                            TextButton(onClick = { onSetDefault(addr.id) }) {
                                Text("Set Default", fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onDelete(addr.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
