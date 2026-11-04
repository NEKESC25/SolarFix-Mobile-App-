package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.CleanGreen
import com.example.util.CameraPermissionHelper

/**
 * Renders the Seller Sales Analytics KPI Dashboard and Monthly Sales Performance Chart & Breakdown.
 * Covers:
 * - Total sales
 * - Total revenue
 * - Number of orders
 * - Completed orders
 * - Pending orders
 * - Available balance
 * - SolarFix commissions
 * - Monthly sales performance
 */
@Composable
fun SellerSalesAnalyticsSection(
    analytics: SellerSalesAnalytics,
    currency: Currency,
    minWithdrawalNgn: Long,
    onRequestWithdrawal: () -> Unit,
    onOpenOrdersTab: () -> Unit
) {
    var selectedMonthIndex by remember(analytics.monthlyPerformance) {
        mutableStateOf((analytics.monthlyPerformance.size - 1).coerceAtLeast(0))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seller_sales_analytics_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Store Sales Analytics & Performance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Live revenue, order completion, escrow & commission telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = CleanGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Live Ledger",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CleanGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Row 1: Total Sales & Total Revenue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsKpiCard(
                label = "Total Sales",
                value = "${analytics.totalSalesCount} Units",
                subLabel = "Net: ${currency.formatPrice(analytics.netRevenueAfterCommissionNgn)}",
                icon = Icons.Default.TrendingUp,
                accentColor = CleanGreen,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_total_sales")
            )
            AnalyticsKpiCard(
                label = "Total Revenue",
                value = currency.formatPrice(analytics.totalRevenueNgn),
                subLabel = "Gross Store Merchandise Value",
                icon = Icons.Default.Payments,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_total_revenue")
            )
        }

        // Row 2: Number of Orders, Completed Orders, Pending Orders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsKpiCard(
                label = "Total Orders",
                value = "${analytics.totalOrdersCount}",
                subLabel = "All store orders",
                icon = Icons.Default.ReceiptLong,
                accentColor = Color(0xFF3B82F6),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenOrdersTab() }
                    .testTag("analytics_kpi_total_orders")
            )
            AnalyticsKpiCard(
                label = "Completed",
                value = "${analytics.completedOrdersCount}",
                subLabel = "Delivered & verified",
                icon = Icons.Default.TaskAlt,
                accentColor = CleanGreen,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenOrdersTab() }
                    .testTag("analytics_kpi_completed_orders")
            )
            AnalyticsKpiCard(
                label = "Pending Orders",
                value = "${analytics.pendingOrdersCount}",
                subLabel = "In fulfilment",
                icon = Icons.Default.PendingActions,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenOrdersTab() }
                    .testTag("analytics_kpi_pending_orders")
            )
        }

        // Row 3: Available Balance, Pending Escrow Payments & SolarFix Commissions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsKpiCard(
                label = "Available Balance",
                value = currency.formatPrice(analytics.availableBalanceNgn),
                subLabel = "Cleared for bank payout",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = CleanGreen,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_available_balance")
            )
            AnalyticsKpiCard(
                label = "Pending Payments",
                value = currency.formatPrice(analytics.pendingPaymentsEscrowNgn),
                subLabel = "48H Escrow protection",
                icon = Icons.Default.LockClock,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f).testTag("analytics_kpi_pending_payments")
            )
            AnalyticsKpiCard(
                label = "SolarFix Commissions",
                value = currency.formatPrice(analytics.solarfixCommissionsNgn),
                subLabel = "${analytics.commissionRatePercent}% platform fee",
                icon = Icons.Default.Percent,
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_commissions")
            )
        }

        // Row 4: Total Earnings, Withdrawn Earnings & Seller Proceeds Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsKpiCard(
                label = "Total Net Earnings",
                value = currency.formatPrice(analytics.totalEarningsNgn),
                subLabel = "Available + Escrow + Paid",
                icon = Icons.Default.Savings,
                accentColor = CleanGreen,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_total_earnings")
            )
            AnalyticsKpiCard(
                label = "Withdrawn Earnings",
                value = currency.formatPrice(analytics.withdrawnEarningsNgn),
                subLabel = "Disbursed to bank",
                icon = Icons.Default.Output,
                accentColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f).testTag("analytics_kpi_withdrawn_earnings")
            )
            AnalyticsKpiCard(
                label = "Seller Proceeds",
                value = "${analytics.sellerProceedsPercent}%",
                subLabel = "Net payout rate",
                icon = Icons.Default.Storefront,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("analytics_kpi_seller_proceeds_rate")
            )
        }

        // Monthly Sales Performance Chart Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_sales_performance_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Sales Performance (6-Month Trend)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Tap any month bar to inspect Gross Revenue, Commission & Net Earnings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = CleanGreen
                    )
                }

                val maxMonthlyGross = remember(analytics.monthlyPerformance) {
                    analytics.monthlyPerformance.maxOfOrNull { it.grossRevenueNgn }?.coerceAtLeast(1L) ?: 1L
                }

                // Interactive Bar Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    analytics.monthlyPerformance.forEachIndexed { index, metric ->
                        val isSelected = index == selectedMonthIndex
                        val heightFraction = (metric.grossRevenueNgn.toFloat() / maxMonthlyGross.toFloat())
                            .coerceIn(0.12f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedMonthIndex = index }
                        ) {
                            Text(
                                text = "${metric.ordersCount} ord",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) CleanGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(heightFraction * 0.72f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(listOf(CleanGreen, Color(0xFF047857)))
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = metric.shortMonth,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Selected Month Spotlight Breakdown
                val selectedMetric = analytics.monthlyPerformance.getOrNull(selectedMonthIndex)
                if (selectedMetric != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${selectedMetric.monthLabel} Performance",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "${selectedMetric.ordersCount} Orders • ${selectedMetric.unitsSold} Units Sold",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CleanGreen
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Revenue:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = currency.formatPrice(selectedMetric.grossRevenueNgn),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SolarFix Commission (${analytics.commissionRatePercent}%):", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "-${currency.formatPrice(selectedMetric.solarfixCommissionNgn)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net Store Earnings:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = currency.formatPrice(selectedMetric.netEarningsNgn),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CleanGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Instant Seller Payout Action Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Instant Seller Bank Payouts",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Available Balance: ${currency.formatPrice(analytics.availableBalanceNgn)} (Min: ${currency.formatPrice(minWithdrawalNgn)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = onRequestWithdrawal,
                    enabled = analytics.availableBalanceNgn >= minWithdrawalNgn,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("seller_request_withdrawal_button")
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Request Withdrawal")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    label: String,
    value: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Comprehensive Add / Edit Seller Product Dialog.
 * Supports:
 * - Adding new products & editing existing products
 * - Uploading multiple product photographs (Gallery multi-picker + Camera)
 * - Adding product videos where supported (Video picker + Video URL/URI & caption)
 * - Setting product prices & available stock quantities
 * - Adding product specifications (Power Rating, Voltage + custom key-value specs)
 * - Adding product warranties
 * - Setting delivery options
 * - Activating or deactivating product visibility
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProductEditorDialog(
    store: SellerStore,
    existingProduct: MarketplaceProduct?,
    onDismiss: () -> Unit,
    onSaveProduct: (
        existingId: String?,
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
        videoDescription: String,
        installmentAllowed: Boolean,
        isActive: Boolean,
        modelNumber: String,
        deliveryAvailability: String,
        returnPolicy: String,
        sellerAuthenticityConfirmed: Boolean,
        authenticityEvidenceNote: String
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember(existingProduct) { mutableStateOf(existingProduct?.title ?: "") }
    var selectedCategory by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.category?.takeIf { it != ProductCategory.ALL } ?: ProductCategory.INVERTERS
        )
    }
    var brand by remember(existingProduct) { mutableStateOf(existingProduct?.brand ?: "") }
    var modelNumber by remember(existingProduct) { mutableStateOf(existingProduct?.effectiveModelNumber ?: "") }
    var powerRating by remember(existingProduct) { mutableStateOf(existingProduct?.powerRating ?: "5kW") }
    var voltage by remember(existingProduct) { mutableStateOf(existingProduct?.voltage ?: "48V") }
    var priceText by remember(existingProduct) {
        mutableStateOf(existingProduct?.basePriceNgn?.toString() ?: "")
    }
    var stockText by remember(existingProduct) {
        mutableStateOf(existingProduct?.stockCount?.toString() ?: "10")
    }
    var condition by remember(existingProduct) {
        mutableStateOf(existingProduct?.normalizedCondition ?: "Brand New")
    }
    var warrantyPeriod by remember(existingProduct) {
        mutableStateOf(existingProduct?.warrantyPeriod ?: "5 Years Manufacturer Warranty")
    }
    var deliveryAvailability by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.deliveryAvailability ?: "Available Nationwide (24-48H Dispatch)"
        )
    }
    var returnPolicy by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.returnPolicy ?: "7-Day DOA Replacement & 48-Hour SolarFix Escrow Return Policy"
        )
    }
    var sellerAuthenticityConfirmed by remember(existingProduct) {
        mutableStateOf(existingProduct?.sellerAuthenticityConfirmed ?: true)
    }
    var authenticityEvidenceNote by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.authenticityEvidenceNote ?: "OEM Serial & Batch Declaration Confirmed"
        )
    }
    var description by remember(existingProduct) {
        mutableStateOf(existingProduct?.description ?: "")
    }
    var installmentAllowed by remember(existingProduct) {
        mutableStateOf(existingProduct?.installmentAllowed ?: true)
    }
    var isActive by remember(existingProduct) {
        mutableStateOf(existingProduct?.isActive ?: true)
    }

    // Multiple Product Photographs state
    var photoBitmaps by remember(existingProduct) {
        mutableStateOf(existingProduct?.allPhotos ?: emptyList())
    }

    // Product Video state
    var videoUrlOrUri by remember(existingProduct) {
        mutableStateOf(existingProduct?.videoUrlOrUri ?: "")
    }
    var videoDescription by remember(existingProduct) {
        mutableStateOf(existingProduct?.videoDescription ?: "360° Load Test & Serial Inspection Video")
    }

    // Delivery Options state
    val allAvailableDeliveryOptions = remember {
        listOf(
            "Doorstep Delivery (24-48H)",
            "Warehouse Self-Pickup",
            "Same-Day Express (City)",
            "Nationwide Heavy Solar Freight",
            "Free Certified Installation"
        )
    }
    var selectedDeliveryOptions by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.deliveryOptions ?: listOf(
                "Doorstep Delivery (24-48H)",
                "Warehouse Self-Pickup"
            )
        )
    }

    // Dynamic Product Specifications state
    var customSpecPairs by remember(existingProduct) {
        val initialSpecs = existingProduct?.specs?.entries
            ?.filter { it.key != "Power Rating" && it.key != "Voltage" }
            ?.map { it.key to it.value }
            ?: listOf("Efficiency" to "98.2% Pure Sine Wave", "Protection" to "IP65 Surge & BMS Protected")
        mutableStateOf(initialSpecs)
    }
    var newSpecKey by remember { mutableStateOf("") }
    var newSpecValue by remember { mutableStateOf("") }

    // Multi-Photo Picker Launcher (up to 5 photos)
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val loaded = uris.mapNotNull { uri ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            photoBitmaps = photoBitmaps + loaded
        }
    }

    // Camera Launcher for capturing additional product photographs
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            photoBitmaps = photoBitmaps + bmp
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch()
    }

    // Video Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUrlOrUri = uri.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingProduct == null) "Add New Solar Product" else "Edit Product Listing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Store: ${store.storeName} • Multi-Photo, Video, Specs & Delivery",
                            style = MaterialTheme.typography.labelSmall,
                            color = CleanGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Product Editor")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Multiple Product Photographs Section
                    Text(
                        text = "1. Multiple Product Photographs (${photoBitmaps.size} Uploaded)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                multiPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("editor_pick_multiple_photos_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Multiple Photos", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (CameraPermissionHelper.hasCameraPermission(context)) {
                                    cameraLauncher.launch()
                                } else {
                                    cameraPermissionLauncher.launch(CameraPermissionHelper.CAMERA_PERMISSION)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("editor_camera_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capture Photo", fontSize = 11.sp)
                        }
                    }

                    if (photoBitmaps.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(photoBitmaps) { idx, bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, CleanGreen, RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Product Photo ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clickable {
                                                photoBitmaps = photoBitmaps.filterIndexed { i, _ -> i != idx }
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove Photo",
                                            tint = Color.White,
                                            modifier = Modifier.padding(3.dp)
                                        )
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(topEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = if (idx == 0) "Main" else "#${idx + 1}",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Product Video Support
                    Text(
                        text = "2. Product Demonstration / QC Video (Optional)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = videoUrlOrUri,
                            onValueChange = { videoUrlOrUri = it },
                            label = { Text("Product Video URI or Demo Link") },
                            placeholder = { Text("e.g. content://media/... or solarfix://video/qc-demo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("editor_video_url_input")
                        )
                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick Video", fontSize = 11.sp)
                        }
                    }
                    OutlinedTextField(
                        value = videoDescription,
                        onValueChange = { videoDescription = it },
                        label = { Text("Video Caption / Inspection Summary") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Core Product Identity, Category, Price & Stock
                    Text(
                        text = "3. Product Details, Price & Stock Quantity",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Product Title *") },
                        placeholder = { Text("e.g. Deye 8kW Hybrid Inverter (Dual MPPT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("editor_product_title_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ProductCategory.values().filter { it != ProductCategory.ALL }) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand / OEM *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = modelNumber,
                            onValueChange = { modelNumber = it },
                            label = { Text("Model Number *") },
                            placeholder = { Text("e.g. SUN-5K-SG04LP1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("editor_product_model_input")
                        )
                    }

                    // Condition: Brand New or Used
                    Text(
                        text = "Product Condition (Brand New or Used)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Brand New", "Used").forEach { condOpt ->
                            FilterChip(
                                selected = condition.equals(condOpt, ignoreCase = true) ||
                                    (condOpt == "Brand New" && !condition.contains("Used", ignoreCase = true)),
                                onClick = { condition = condOpt },
                                label = { Text(condOpt) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Product Price (₦) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("editor_product_price_input")
                        )
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Available Quantity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("editor_product_stock_input")
                        )
                    }

                    // 4. Technical Specifications Builder
                    Text(
                        text = "4. Technical Specifications",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = powerRating,
                            onValueChange = { powerRating = it },
                            label = { Text("Power / Capacity Rating") },
                            placeholder = { Text("e.g. 5kW / 5.12kWh / 550W") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = voltage,
                            onValueChange = { voltage = it },
                            label = { Text("Nominal Voltage") },
                            placeholder = { Text("e.g. 48V DC / 230V AC") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Custom Specification Rows
                    customSpecPairs.forEachIndexed { index, (k, v) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$k: $v",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    customSpecPairs = customSpecPairs.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Spec", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSpecKey,
                            onValueChange = { newSpecKey = it },
                            label = { Text("Spec Name (e.g. MPPT)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSpecValue,
                            onValueChange = { newSpecValue = it },
                            label = { Text("Spec Value (e.g. Dual 120A)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalIconButton(
                            onClick = {
                                if (newSpecKey.isNotBlank() && newSpecValue.isNotBlank()) {
                                    customSpecPairs = customSpecPairs + (newSpecKey.trim() to newSpecValue.trim())
                                    newSpecKey = ""
                                    newSpecValue = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Specification")
                        }
                    }

                    // 5. Product Warranty
                    Text(
                        text = "5. Product Warranty Coverage",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    val warrantyPresets = listOf(
                        "2 Years Manufacturer Warranty",
                        "5 Years Manufacturer Warranty",
                        "10 Years Lithium BMS Warranty",
                        "25 Years Linear Output Warranty"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(warrantyPresets) { preset ->
                            FilterChip(
                                selected = warrantyPeriod == preset,
                                onClick = { warrantyPeriod = preset },
                                label = { Text(preset, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = warrantyPeriod,
                        onValueChange = { warrantyPeriod = it },
                        label = { Text("Warranty Policy *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("editor_product_warranty_input")
                    )

                    // 6. Delivery Availability & Return Policy
                    Text(
                        text = "6. Delivery Availability & Return Policy",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allAvailableDeliveryOptions) { option ->
                            val isSelected = selectedDeliveryOptions.contains(option)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDeliveryOptions = if (isSelected) {
                                        (selectedDeliveryOptions - option).ifEmpty { listOf(option) }
                                    } else {
                                        selectedDeliveryOptions + option
                                    }
                                    deliveryAvailability = selectedDeliveryOptions.joinToString(" • ")
                                },
                                label = { Text(option, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = deliveryAvailability,
                        onValueChange = { deliveryAvailability = it },
                        label = { Text("Delivery Availability Summary *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = returnPolicy,
                        onValueChange = { returnPolicy = it },
                        label = { Text("Return Policy *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("editor_product_return_policy_input")
                    )

                    // 7. Description & Listing Visibility
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Product Description & Application Notes") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 8. Product Authenticity & Quality Verification Declaration
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CleanGreen.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "8. Mandatory Product Authenticity Declaration",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = CleanGreen
                            )
                            OutlinedTextField(
                                value = authenticityEvidenceNote,
                                onValueChange = { authenticityEvidenceNote = it },
                                label = { Text("OEM Serial / Batch / Authenticity Evidence Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sellerAuthenticityConfirmed = !sellerAuthenticityConfirmed }
                            ) {
                                Checkbox(
                                    checked = sellerAuthenticityConfirmed,
                                    onCheckedChange = { sellerAuthenticityConfirmed = it },
                                    modifier = Modifier.testTag("editor_authenticity_checkbox")
                                )
                                Text(
                                    text = "I confirm that this product is genuine, specifications are accurate, and it is not counterfeit or re-labeled.",
                                    style = MaterialTheme.typography.labelSmall
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
                            Text("Listing Active in Customer Storefront", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("Turn off to temporarily deactivate product without deleting it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow CDcare-Style Smart Installments (3–6 Months)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("Increases conversion on high-ticket inverters & batteries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = installmentAllowed, onCheckedChange = { installmentAllowed = it })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save / Publish Action
                Button(
                    onClick = {
                        val parsedPrice = priceText.toLongOrNull() ?: 150000L
                        val parsedStock = stockText.toIntOrNull() ?: 10
                        val finalSpecs = customSpecPairs.toMap().toMutableMap().apply {
                            put("Power Rating", powerRating)
                            put("Voltage", voltage)
                            if (modelNumber.isNotBlank()) {
                                put("Model", modelNumber.trim())
                            }
                        }
                        onSaveProduct(
                            existingProduct?.id,
                            title.ifBlank { "${brand.ifBlank { "SolarFix" }} $powerRating ${selectedCategory.label}" },
                            selectedCategory,
                            brand.ifBlank { "SolarFix Certified OEM" },
                            powerRating,
                            voltage,
                            parsedPrice,
                            parsedStock,
                            condition,
                            finalSpecs,
                            description.ifBlank { "Verified solar hardware supplied by ${store.storeName} with $warrantyPeriod." },
                            warrantyPeriod,
                            selectedDeliveryOptions,
                            photoBitmaps,
                            videoUrlOrUri.takeIf { it.isNotBlank() },
                            videoDescription,
                            installmentAllowed,
                            isActive,
                            modelNumber,
                            deliveryAvailability,
                            returnPolicy,
                            sellerAuthenticityConfirmed,
                            authenticityEvidenceNote
                        )
                    },
                    enabled = sellerAuthenticityConfirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("editor_save_product_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingProduct == null) "Publish Genuine Product to Store" else "Save Product Updates",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Modal for rejecting an unshipped customer order in compliance with SolarFix Marketplace Rules.
 */
@Composable
fun SellerRejectOrderModal(
    order: MarketplaceOrder,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    val ruleReasons = listOf(
        "Out of Stock / Failed Pre-Dispatch QC Test",
        "Unserviceable Remote Delivery Location",
        "Unit Damaged During Warehouse Serial Scan",
        "Customer Requested Cancellation Before Dispatch"
    )
    var selectedReason by remember { mutableStateOf(ruleReasons.first()) }
    var customNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = {
            Text(
                text = "Reject Order #${order.id}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SolarFix Marketplace Rule: Unshipped orders may only be rejected for valid stock, QC, or logistics reasons. Rejecting automatically refunds 100% of the buyer's escrow payment and restores item inventory.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ruleReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Additional Note to Buyer (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (customNote.isNotBlank()) "$selectedReason (${customNote.trim()})" else selectedReason
                    onConfirmReject(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_reject_order_button")
            ) {
                Text("Reject & Refund Buyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
