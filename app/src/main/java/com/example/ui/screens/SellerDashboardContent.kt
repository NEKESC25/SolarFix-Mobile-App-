package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.CleanGreen
import com.example.ui.viewmodel.MarketplaceViewModel

private enum class SellerDashTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OVERVIEW_EARNINGS("Revenue & Payouts", Icons.Default.AccountBalanceWallet),
    INVENTORY("My Products", Icons.Default.Inventory2),
    ORDERS_FULFILMENT("Customer Orders", Icons.Default.LocalShipping),
    REVIEWS("Customer Reviews", Icons.Default.RateReview),
    STORE_PROFILE("Store Settings", Icons.Default.Storefront)
}

@Composable
fun SellerDashboardContent(
    viewModel: MarketplaceViewModel,
    onSwitchToCustomerShop: () -> Unit,
    onOpenSellerRegistration: () -> Unit = {}
) {
    BackHandler { onSwitchToCustomerShop() }

    val stores by viewModel.stores.collectAsState()
    val activeStoreId by viewModel.activeSellerStoreId.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val allOrders by viewModel.orders.collectAsState()
    val allReviews by viewModel.reviews.collectAsState()
    val allWithdrawals by viewModel.withdrawals.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val activeStore = stores.find { it.id == activeStoreId } ?: stores.firstOrNull()
    val storeProducts = allProducts.filter { it.storeId == activeStore?.id }
    val storeOrders = allOrders.filter { it.primaryStoreId == activeStore?.id }
    val storeReviews = allReviews.filter { it.storeId == activeStore?.id }
    val storeWithdrawals = allWithdrawals.filter { it.storeId == activeStore?.id }
    val storeTransactions = allTransactions.filter { it.storeId == activeStore?.id }

    var selectedTab by remember { mutableStateOf(SellerDashTab.OVERVIEW_EARNINGS) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }
    var showCreateNewStoreMode by remember { mutableStateOf(false) }

    // Full Product Editor Modal state (null = closed; product = editing; showAddNewProductModal = adding new)
    var showAddNewProductModal by remember { mutableStateOf(false) }
    var productBeingEdited by remember { mutableStateOf<MarketplaceProduct?>(null) }

    // Order Rejection Modal state
    var orderBeingRejected by remember { mutableStateOf<MarketplaceOrder?>(null) }

    // Order Filter state inside ORDERS_FULFILMENT tab
    var selectedOrderFilter by remember { mutableStateOf("ALL") }

    if (activeStore == null) return
    val isApprovedSeller = activeStore.status == StoreVerificationStatus.APPROVED
    val analytics = remember(activeStore, storeOrders, storeProducts, settings) {
        viewModel.getSellerSalesAnalytics(activeStore.id)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("seller_dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Active Store Switcher & Quick Upload Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF065F46))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = when (activeStore.status) {
                                    StoreVerificationStatus.APPROVED -> CleanGreen
                                    StoreVerificationStatus.PENDING, StoreVerificationStatus.MORE_INFO_REQUIRED -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    if (isApprovedSeller) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isApprovedSeller) "VERIFIED SELLER" else activeStore.status.label.uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "SolarFix Commission: ${activeStore.customCommissionPercent ?: settings.defaultCommissionPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Text(
                            text = activeStore.storeName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "${activeStore.businessRegNumber} • ${activeStore.location} • ${activeStore.storeOpeningDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "In-App Store Route: ${activeStore.inAppBreadcrumb} (ID: ${activeStore.id})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFDE68A),
                            fontSize = 10.sp
                        )

                        // Switch Store or Apply as New Seller
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(stores) { st ->
                                AssistChip(
                                    onClick = {
                                        showCreateNewStoreMode = false
                                        viewModel.selectActiveSellerStore(st.id)
                                    },
                                    label = {
                                        Text(
                                            text = "${st.storeName} (${if (st.status == StoreVerificationStatus.APPROVED) "✓" else st.status.label})",
                                            color = if (st.id == activeStore.id) Color(0xFFFBBF24) else Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = onOpenSellerRegistration,
                                    label = {
                                        Text("+ New Seller Registration", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    productBeingEdited = null
                                    showAddNewProductModal = true
                                },
                                enabled = isApprovedSeller,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("seller_upload_product_button")
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isApprovedSeller) "Add New Product" else "Approval Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.openSellerStoreInsideApp(activeStore.id) },
                                enabled = isApprovedSeller,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("seller_preview_store_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Storefront", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Strict Verification Gate Banner when Seller is NOT Approved
        if (!isApprovedSeller) {
            item {
                SellerVerificationGateCard(
                    store = activeStore,
                    onResubmitVerification = { cac, address, idType, idNum, bankAccName, bankName, bankAccNo, note ->
                        viewModel.resubmitSellerVerificationInfo(
                            storeId = activeStore.id,
                            cacRegistrationNumber = cac,
                            physicalAddress = address,
                            idDocumentType = idType,
                            idDocumentNumber = idNum,
                            bankAccountName = bankAccName,
                            bankName = bankName,
                            bankAccountNumber = bankAccNo,
                            sellerResponseNote = note
                        )
                    }
                )
            }
        }

        // Seller Dashboard Navigation Tabs
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SellerDashTab.values()) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label, fontSize = 12.sp) },
                        leadingIcon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("seller_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedTab) {
            SellerDashTab.OVERVIEW_EARNINGS -> {
                // Full Sales Analytics KPI Grid + Monthly Sales Performance Chart
                item {
                    SellerSalesAnalyticsSection(
                        analytics = analytics,
                        currency = currency,
                        minWithdrawalNgn = settings.minWithdrawalNgn,
                        onRequestWithdrawal = { showWithdrawalDialog = true },
                        onOpenOrdersTab = { selectedTab = SellerDashTab.ORDERS_FULFILMENT }
                    )
                }

                // Completed Sales Summary Card
                val completedOrdersList = storeOrders.filter { it.status == OrderStatus.DELIVERED }
                if (completedOrdersList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed Sales (${completedOrdersList.size} Delivered Orders)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(completedOrdersList) { compOrd ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Order #${compOrd.id} • ${compOrd.customerName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${compOrd.items.joinToString { "${it.quantity}x ${it.productTitle}" }} • Commission: -${currency.formatPrice(compOrd.solarfixCommissionNgn)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${currency.formatPrice(compOrd.netSellerEarningsNgn)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = CleanGreen
                                    )
                                    Text("Escrow Released", style = MaterialTheme.typography.labelSmall, color = CleanGreen)
                                }
                            }
                        }
                    }
                }

                // Withdrawal History
                if (storeWithdrawals.isNotEmpty()) {
                    item {
                        Text("Withdrawal Requests (${storeWithdrawals.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    items(storeWithdrawals) { wth ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${wth.id} • ${wth.bankName} (${wth.accountNumber})", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${wth.accountName} • ${wth.requestedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(currency.formatPrice(wth.amountNgn), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    Text(wth.status.label, style = MaterialTheme.typography.labelSmall, color = if (wth.status == WithdrawalStatus.APPROVED_PAID) CleanGreen else Color(0xFFF59E0B))
                                }
                            }
                        }
                    }
                }

                // Store Transaction Ledger (Order Credits, Commissions, Releases, Withdrawals)
                item {
                    Text("Store Transaction & Commission Ledger (${storeTransactions.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                items(storeTransactions) { tx ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("${tx.subtitle} • ${tx.timestamp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = (if (tx.isCreditToSeller) "+" else "-") + currency.formatPrice(tx.amountNgn),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (tx.isCreditToSeller) CleanGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            SellerDashTab.INVENTORY -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${activeStore.storeName} Inventory (${storeProducts.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Active: ${analytics.activeProductsCount} • Deactivated: ${analytics.deactivatedProductsCount} • Out of Stock: ${analytics.outOfStockProductsCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    productBeingEdited = null
                                    showAddNewProductModal = true
                                },
                                enabled = isApprovedSeller,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("inventory_add_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Product", fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(storeProducts) { prod ->
                    var priceInput by remember(prod.id, prod.basePriceNgn) { mutableStateOf(prod.basePriceNgn.toString()) }
                    var stockInput by remember(prod.id, prod.stockCount) { mutableStateOf(prod.stockCount.toString()) }
                    var evidenceInput by remember(prod.id) { mutableStateOf("") }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("seller_inventory_card_${prod.id}")
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${prod.category.label} • ${prod.brand} • Model: ${prod.effectiveModelNumber} • ${prod.normalizedCondition}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${prod.powerRating} • ${prod.voltage} • ${prod.warrantyPeriod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Active / Deactivated Status Badge
                                    Surface(
                                        color = if (prod.isActive) CleanGreen.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (prod.isActive) "Active" else "Deactivated",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (prod.isActive) CleanGreen else Color(0xFFF59E0B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = if (prod.moderationStatus == ProductModerationStatus.APPROVED) CleanGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = prod.moderationStatus.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (prod.moderationStatus == ProductModerationStatus.APPROVED) CleanGreen else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Seller Verification vs Independent SolarFix Inspection status line
                            Text(
                                text = if (prod.hasActualSolarFixInspection) {
                                    "✓ Seller Verified • ✓ Independently Inspected by SolarFix (#${prod.solarfixInspectionReportId})"
                                } else {
                                    "✓ Seller Verified • Not Independently Inspected by SolarFix (No SolarFix inspection badge shown)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (prod.hasActualSolarFixInspection) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // If Admin flagged as Suspicious or Requested Authenticity Evidence, show response box
                            if (prod.moderationStatus == ProductModerationStatus.EVIDENCE_REQUESTED ||
                                prod.moderationStatus == ProductModerationStatus.SUSPICIOUS_HIGH_RISK
                            ) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Admin Authenticity Audit Note: ${prod.adminModerationNote}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFB45309)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = evidenceInput,
                                                onValueChange = { evidenceInput = it },
                                                label = { Text("Provide OEM Serial / Invoice Evidence") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.submitSellerProductAuthenticityEvidence(
                                                        prod.id,
                                                        evidenceInput.ifBlank { "OEM Serial Barcode & Distributor Invoice Attached" }
                                                    )
                                                    evidenceInput = ""
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Submit Evidence", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Media, Specs & Delivery Options Summary Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "📷 ${prod.allPhotos.size.coerceAtLeast(prod.photoAngles.size)} Photos",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (!prod.videoUrlOrUri.isNullOrBlank()) {
                                    item {
                                        Surface(
                                            color = CleanGreen.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "🎥 Video Attached",
                                                color = CleanGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "⚙ ${prod.specs.size} Specs",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                items(prod.deliveryOptions) { delivOpt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "🚚 $delivOpt",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Inline Price & Quantity Editor
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = priceInput,
                                    onValueChange = { priceInput = it },
                                    label = { Text("Price (₦)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = stockInput,
                                    onValueChange = { stockInput = it },
                                    label = { Text("Qty / Stock") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.65f)
                                )
                                Button(
                                    onClick = {
                                        val parsedPrice = priceInput.toLongOrNull() ?: prod.basePriceNgn
                                        val parsedStock = stockInput.toIntOrNull() ?: prod.stockCount
                                        viewModel.updateSellerProductPriceAndStock(prod.id, parsedPrice, parsedStock)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Update", fontSize = 11.sp)
                                }
                            }

                            // Product Management Actions: Edit Full Product | Deactivate/Activate | Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { productBeingEdited = prod },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_product_button_${prod.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Details & Media", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.toggleSellerProductActiveStatus(prod.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).testTag("toggle_active_product_button_${prod.id}")
                                ) {
                                    Icon(
                                        imageVector = if (prod.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (prod.isActive) "Deactivate" else "Activate", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSellerProduct(prod.id) },
                                    modifier = Modifier.testTag("delete_product_button_${prod.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            SellerDashTab.ORDERS_FULFILMENT -> {
                val filteredStoreOrders = when (selectedOrderFilter) {
                    "PENDING" -> storeOrders.filter {
                        it.status in setOf(
                            OrderStatus.CONFIRMED,
                            OrderStatus.PROCESSING,
                            OrderStatus.PACKED,
                            OrderStatus.IN_TRANSIT,
                            OrderStatus.OUT_FOR_DELIVERY
                        )
                    }
                    "COMPLETED" -> storeOrders.filter { it.status == OrderStatus.DELIVERED }
                    "REJECTED" -> storeOrders.filter {
                        it.status == OrderStatus.REJECTED_BY_SELLER ||
                            it.status == OrderStatus.REFUNDED ||
                            it.status == OrderStatus.DISPUTED
                    }
                    else -> storeOrders
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Customer Orders & Fulfilment (${storeOrders.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedOrderFilter == "ALL",
                                    onClick = { selectedOrderFilter = "ALL" },
                                    label = { Text("All (${storeOrders.size})", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedOrderFilter == "PENDING",
                                    onClick = { selectedOrderFilter = "PENDING" },
                                    label = { Text("Pending (${analytics.pendingOrdersCount})", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedOrderFilter == "COMPLETED",
                                    onClick = { selectedOrderFilter = "COMPLETED" },
                                    label = { Text("Completed Sales (${analytics.completedOrdersCount})", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedOrderFilter == "REJECTED",
                                    onClick = { selectedOrderFilter = "REJECTED" },
                                    label = { Text("Rejected / Refunded (${analytics.rejectedOrRefundedOrdersCount})", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                items(filteredStoreOrders) { order ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("seller_order_card_${order.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Order #${order.id} • ${order.customerName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = order.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (order.status == OrderStatus.REJECTED_BY_SELLER || order.status == OrderStatus.REFUNDED) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        CleanGreen
                                    }
                                )
                            }
                            Text(
                                text = "Items: ${order.items.joinToString { "${it.quantity}x ${it.productTitle}" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Deliver to: ${order.deliveryAddress.formattedLine()} • Tracking: ${order.trackingNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!order.sellerRejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "Rejection Reason: ${order.sellerRejectionReason}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Commission & Net Seller Earnings Breakdown
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Gross Order Amount: ${currency.formatPrice(order.subtotalNgn)}", style = MaterialTheme.typography.labelSmall)
                                    Text("SolarFix Commission (${order.commissionPercent}%): -${currency.formatPrice(order.solarfixCommissionNgn)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    Text("Your Net Store Earning: ${currency.formatPrice(order.netSellerEarningsNgn)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CleanGreen)
                                }
                            }

                            // Accept / Reject & Fulfilment Action Buttons
                            when (order.status) {
                                OrderStatus.CONFIRMED -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.acceptSellerOrder(order.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                            modifier = Modifier.weight(1f).testTag("seller_accept_order_${order.id}")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Accept Order", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { orderBeingRejected = order },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f).testTag("seller_reject_order_${order.id}")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject Order", fontSize = 12.sp)
                                        }
                                    }
                                }
                                OrderStatus.PROCESSING -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.advanceSellerOrderStatus(order.id, OrderStatus.PACKED) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Mark Packed & QC Tested", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { orderBeingRejected = order },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Reject", fontSize = 12.sp)
                                        }
                                    }
                                }
                                OrderStatus.PACKED -> {
                                    Button(
                                        onClick = { viewModel.advanceSellerOrderStatus(order.id, OrderStatus.IN_TRANSIT) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Dispatch to Logistics (In Transit)", fontSize = 12.sp)
                                    }
                                }
                                OrderStatus.IN_TRANSIT -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.advanceSellerOrderStatus(order.id, OrderStatus.OUT_FOR_DELIVERY) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Mark Out for Delivery", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.advanceSellerOrderStatus(order.id, OrderStatus.DELIVERED) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Confirm Delivered", fontSize = 11.sp)
                                        }
                                    }
                                }
                                OrderStatus.OUT_FOR_DELIVERY -> {
                                    Button(
                                        onClick = { viewModel.advanceSellerOrderStatus(order.id, OrderStatus.DELIVERED) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Confirm Delivered & Release Escrow", fontSize = 12.sp)
                                    }
                                }
                                else -> {
                                    Text(
                                        text = if (order.escrowReleased) {
                                            "✓ Completed Sale — Escrow Released to Available Earnings"
                                        } else {
                                            "Status: ${order.status.label}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (order.escrowReleased) CleanGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SellerDashTab.REVIEWS -> {
                item {
                    Text(
                        text = "Customer Reviews on ${activeStore.storeName} (${storeReviews.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(storeReviews) { rev ->
                    var replyInput by remember(rev.id) { mutableStateOf(rev.sellerReply.orEmpty()) }
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.customerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("★".repeat(rev.rating) + " • ${rev.date}", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = replyInput,
                                onValueChange = { replyInput = it },
                                label = { Text("Official Store Response") },
                                placeholder = { Text("Thank customer or address warranty support...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { viewModel.replyToReview(rev.id, replyInput) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (rev.sellerReply.isNullOrBlank()) "Post Seller Reply" else "Update Reply", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            SellerDashTab.STORE_PROFILE -> {
                item {
                    SellerStoreProfileEditorCard(
                        store = if (showCreateNewStoreMode) null else activeStore,
                        onToggleCreateNew = { showCreateNewStoreMode = !showCreateNewStoreMode },
                        onSaveStore = { id, name, owner, email, regNo, loc, phone, tagline, desc, warranty, logoBmp, coverBmp ->
                            viewModel.saveSellerStoreProfile(
                                storeId = id,
                                storeName = name,
                                ownerName = owner,
                                ownerEmail = email,
                                businessRegNumber = regNo,
                                location = loc,
                                phone = phone,
                                tagline = tagline,
                                description = desc,
                                warrantyPolicy = warranty,
                                logoBitmap = logoBmp,
                                coverImageBitmap = coverBmp
                            )
                            showCreateNewStoreMode = false
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Product Modal
    if (showAddNewProductModal || productBeingEdited != null) {
        SellerProductEditorDialog(
            store = activeStore,
            existingProduct = productBeingEdited,
            onDismiss = {
                showAddNewProductModal = false
                productBeingEdited = null
            },
            onSaveProduct = { existingId, title, cat, brand, power, volt, price, stock, cond, specs, desc, warranty, delivOpts, photos, videoUrl, videoDesc, installAllowed, active, modelNo, delivAvail, retPolicy, authConfirmed, authNote ->
                viewModel.saveOrUpdateSellerProduct(
                    existingProductId = existingId,
                    storeId = activeStore.id,
                    title = title,
                    category = cat,
                    brand = brand,
                    powerRating = power,
                    voltage = volt,
                    priceNgn = price,
                    stockCount = stock,
                    condition = cond,
                    specs = specs,
                    description = desc,
                    warrantyPeriod = warranty,
                    deliveryOptions = delivOpts,
                    photoBitmaps = photos,
                    videoUrlOrUri = videoUrl,
                    videoDescription = videoDesc,
                    installmentAllowed = installAllowed,
                    isActive = active,
                    modelNumber = modelNo,
                    deliveryAvailability = delivAvail,
                    returnPolicy = retPolicy,
                    sellerAuthenticityConfirmed = authConfirmed,
                    authenticityEvidenceNote = authNote
                )
                showAddNewProductModal = false
                productBeingEdited = null
            }
        )
    }

    // Reject Customer Order Modal
    if (orderBeingRejected != null) {
        SellerRejectOrderModal(
            order = orderBeingRejected!!,
            onDismiss = { orderBeingRejected = null },
            onConfirmReject = { reason ->
                viewModel.rejectSellerOrder(orderBeingRejected!!.id, reason)
                orderBeingRejected = null
            }
        )
    }

    if (showWithdrawalDialog) {
        SellerWithdrawalModal(
            store = activeStore,
            minWithdrawalNgn = settings.minWithdrawalNgn,
            currency = currency,
            onDismiss = { showWithdrawalDialog = false },
            onSubmit = { amount, bank, accNo, accName ->
                val ok = viewModel.requestSellerWithdrawal(activeStore.id, amount, bank, accNo, accName)
                if (ok) showWithdrawalDialog = false
            }
        )
    }
}

@Composable
private fun SellerVerificationGateCard(
    store: SellerStore,
    onResubmitVerification: (String, String, String, String, String, String, String, String) -> Unit
) {
    var showResubmitForm by remember(store.id, store.status) {
        mutableStateOf(store.status == StoreVerificationStatus.MORE_INFO_REQUIRED)
    }
    var cacNumber by remember(store) { mutableStateOf(store.businessRegNumber) }
    var physicalAddr by remember(store) { mutableStateOf(store.physicalAddress) }
    var idType by remember(store) { mutableStateOf(store.idDocumentType) }
    var idNumber by remember(store) { mutableStateOf(store.idDocumentNumber) }
    var bankAccName by remember(store) { mutableStateOf(store.bankAccountName) }
    var bankName by remember(store) { mutableStateOf(store.bankName) }
    var bankAccNo by remember(store) { mutableStateOf(store.bankAccountNumber) }
    var sellerNote by remember(store) { mutableStateOf("") }

    val statusColor = when (store.status) {
        StoreVerificationStatus.PENDING, StoreVerificationStatus.MORE_INFO_REQUIRED -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seller_verification_gate_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (store.status == StoreVerificationStatus.PENDING) Icons.Default.HourglassTop else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Store Status: ${store.status.label}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = statusColor
                    )
                    Text(
                        text = "Only approved sellers can publish products and operate their stores.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SolarFix Administrator Verification Note:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = store.adminVerificationNote,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (store.status == StoreVerificationStatus.MORE_INFO_REQUIRED || store.status == StoreVerificationStatus.PENDING) {
                OutlinedButton(
                    onClick = { showResubmitForm = !showResubmitForm },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showResubmitForm) "Hide KYC Update Form" else "Provide / Update Requested Verification Details")
                }

                if (showResubmitForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cacNumber,
                            onValueChange = { cacNumber = it },
                            label = { Text("CAC Registration Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = physicalAddr,
                            onValueChange = { physicalAddr = it },
                            label = { Text("Physical Business Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = idType,
                                onValueChange = { idType = it },
                                label = { Text("ID Document Type") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = idNumber,
                                onValueChange = { idNumber = it },
                                label = { Text("ID Document Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Bank Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bankAccNo,
                                onValueChange = { bankAccNo = it },
                                label = { Text("Account Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = bankAccName,
                            onValueChange = { bankAccName = it },
                            label = { Text("Bank Account Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sellerNote,
                            onValueChange = { sellerNote = it },
                            label = { Text("Message to SolarFix Compliance Admin") },
                            placeholder = { Text("e.g. Uploaded clearer CAC certificate and premises signboard photo.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onResubmitVerification(
                                    cacNumber,
                                    physicalAddr,
                                    idType,
                                    idNumber,
                                    bankAccName,
                                    bankName,
                                    bankAccNo,
                                    sellerNote
                                )
                                showResubmitForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seller_resubmit_verification_button")
                        ) {
                            Text("Submit Additional Verification Info")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = accent)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SellerStoreProfileEditorCard(
    store: SellerStore?,
    onToggleCreateNew: () -> Unit,
    onSaveStore: (String?, String, String, String, String, String, String, String, String, String, Bitmap?, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    var storeName by remember(store) { mutableStateOf(store?.storeName ?: "") }
    var ownerName by remember(store) { mutableStateOf(store?.ownerName ?: "Kingsley Nekes") }
    var ownerEmail by remember(store) { mutableStateOf(store?.ownerEmail ?: "nekeskingsley@gmail.com") }
    var regNo by remember(store) { mutableStateOf(store?.businessRegNumber ?: "RC-1948201 • COREN/NEMSA") }
    var location by remember(store) { mutableStateOf(store?.location ?: "Ikeja & Lekki, Lagos") }
    var phone by remember(store) { mutableStateOf(store?.phone ?: "+234 803 700 1234") }
    var tagline by remember(store) { mutableStateOf(store?.tagline ?: "Premier Hybrid Inverters & Lithium Storage") }
    var description by remember(store) { mutableStateOf(store?.description ?: "Verified online solar store operating inside SolarFix.") }
    var warranty by remember(store) { mutableStateOf(store?.warrantyPolicy ?: "5 Years Manufacturer Warranty + 48H Escrow") }
    var logoBitmap by remember(store) { mutableStateOf(store?.logoBitmap) }
    var coverImageBitmap by remember(store) { mutableStateOf(store?.coverImageBitmap) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                logoBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                coverImageBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (store == null) "Create New Online Store Inside SolarFix" else "Manage Individual Store Profile",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onToggleCreateNew) {
                    Text(if (store == null) "Edit Existing Store" else "+ Create New Store", fontSize = 12.sp)
                }
            }

            if (store != null) {
                // In-App Store Route & Store Identity Summary
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Individual Store Inside SolarFix (No External Website)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("• Internal Store Identifier: ${store.id}", style = MaterialTheme.typography.labelSmall)
                        Text("• Unique In-App Store Route: ${store.inAppStoreRoute}", style = MaterialTheme.typography.labelSmall)
                        Text("• Navigation Path: ${store.inAppBreadcrumb}", style = MaterialTheme.typography.labelSmall)
                        Text("• Store Opening Date: ${store.storeOpeningDate} • ${store.successfulSalesCount} Successful Sales", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Private KYC & Bank Settlement Vault (Hidden from Customers)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CleanGreen
                        )
                        Text("• Physical Address: ${store.physicalAddress}", style = MaterialTheme.typography.labelSmall)
                        Text("• Verified ID: ${store.idDocumentType} (${store.idDocumentNumber})", style = MaterialTheme.typography.labelSmall)
                        Text("• Premises Photo: ${if (store.premisesPhotoProvided) "Verified on file" else "Pending"}", style = MaterialTheme.typography.labelSmall)
                        Text("• Settlement Bank: ${store.bankName} • ${store.bankAccountNumber} (${store.bankAccountName})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Store Logo & Store Cover Image Customization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (logoBitmap != null) "Logo Uploaded ✓" else "Upload Store Logo", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        coverPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (coverImageBitmap != null) "Cover Image ✓" else "Upload Cover Banner", fontSize = 11.sp)
                }
            }

            if (coverImageBitmap != null) {
                Image(
                    bitmap = coverImageBitmap!!.asImageBitmap(),
                    contentDescription = "Store Cover Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                )
            }

            OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Store Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Store Banner Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = regNo, onValueChange = { regNo = it }, label = { Text("CAC / RC / License") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Store Location") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Store Phone") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = warranty, onValueChange = { warranty = it }, label = { Text("Store Warranty & Return Policy") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Store Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    if (storeName.isNotBlank()) {
                        onSaveStore(
                            store?.id,
                            storeName,
                            ownerName,
                            ownerEmail,
                            regNo,
                            location,
                            phone,
                            tagline,
                            description,
                            warranty,
                            logoBitmap,
                            coverImageBitmap
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_seller_store_button")
            ) {
                Text(if (store == null) "Submit Store (Pending Verification)" else "Save Store Changes")
            }
        }
    }
}

@Composable
private fun SellerWithdrawalModal(
    store: SellerStore,
    minWithdrawalNgn: Long,
    currency: Currency,
    onDismiss: () -> Unit,
    onSubmit: (Long, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(store.availableEarningsNgn.toString()) }
    var bankName by remember { mutableStateOf(store.bankName.ifBlank { "Zenith Bank PLC" }) }
    var accountNumber by remember { mutableStateOf(store.bankAccountNumber.ifBlank { "1019482015" }) }
    var accountName by remember { mutableStateOf(store.bankAccountName.ifBlank { store.storeName.uppercase() }) }

    val amt = amountText.toLongOrNull() ?: 0L
    val exceedsAvailable = amt > store.availableEarningsNgn
    val belowMin = amt < minWithdrawalNgn
    val isValid = amt > 0L && !exceedsAvailable && !belowMin && bankName.isNotBlank() && accountNumber.isNotBlank() && accountName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CleanGreen) },
        title = { Text("Request Seller Earnings Payout", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Available Balance: ${currency.formatPrice(store.availableEarningsNgn)} (Ready for Payout)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CleanGreen
                        )
                        Text(
                            text = "Pending Escrow: ${currency.formatPrice(store.pendingEscrowNgn)} (Locked until delivery confirmation)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "Withdrawn to Date: ${currency.formatPrice(store.totalWithdrawnNgn)} • Min Payout: ${currency.formatPrice(minWithdrawalNgn)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (exceedsAvailable) {
                    Text(
                        text = "⚠️ Cannot request more than your available balance of ${currency.formatPrice(store.availableEarningsNgn)}. Pending escrow payments cannot be withdrawn.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (belowMin && amountText.isNotBlank()) {
                    Text(
                        text = "⚠️ Minimum withdrawal amount is ${currency.formatPrice(minWithdrawalNgn)}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Withdrawal Amount (₦)") },
                    isError = exceedsAvailable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Settlement Bank Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("10-Digit NUBAN Account Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Verified Account Holder Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSubmit(amt, bankName, accountNumber, accountName)
                    }
                },
                enabled = isValid
            ) {
                Text("Submit Withdrawal Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
