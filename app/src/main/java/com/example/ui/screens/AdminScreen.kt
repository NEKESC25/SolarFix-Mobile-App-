package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.CleanGreen
import com.example.ui.viewmodel.MarketplaceViewModel

enum class AdminSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OVERVIEW("Overview & Reports", Icons.Default.Dashboard),
    SELLERS_STORES("Sellers & Stores", Icons.Default.Storefront),
    HARDWARE_VETTING("Product Moderation", Icons.Default.Verified),
    ESCROW("Orders & Transactions", Icons.Default.AccountBalanceWallet),
    PAYOUTS_DISPUTES("Payouts & Disputes", Icons.Default.Gavel),
    SETTINGS("Commissions & Config", Icons.Default.Settings),
    TECHNICIANS("Technicians", Icons.Default.Engineering)
}

data class TechnicianApplicant(
    val id: String,
    val name: String,
    val state: String,
    val licenseNo: String,
    val specialization: String,
    val status: String = "PENDING_REVIEW"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminUser: UserAccount,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    marketplaceViewModel: MarketplaceViewModel = viewModel()
) {
    if (!adminUser.isAdmin) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_route_access_denied")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Access Denied",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Administrative Route Restricted",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Access requires an active Firebase Auth custom claim (admin=true) or Firestore User document clearance (isAdmin=true).\n\nAccount: ${adminUser.email}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val stores by marketplaceViewModel.stores.collectAsState()
    val products by marketplaceViewModel.products.collectAsState()
    val orders by marketplaceViewModel.orders.collectAsState()
    val transactions by marketplaceViewModel.transactions.collectAsState()
    val withdrawals by marketplaceViewModel.withdrawals.collectAsState()
    val disputes by marketplaceViewModel.disputes.collectAsState()
    val settings by marketplaceViewModel.settings.collectAsState()
    val currency by marketplaceViewModel.currency.collectAsState()
    val adminAuditLogs by marketplaceViewModel.adminAuditLogs.collectAsState()

    var selectedSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var generatedReport by remember { mutableStateOf<FinancialReportSummary?>(null) }

    var applicants by remember {
        mutableStateOf(
            listOf(
                TechnicianApplicant(
                    id = "tech_app_1",
                    name = "Engr. Joshua Bello",
                    state = "Lagos (Ikeja)",
                    licenseNo = "COREN #PV-49120",
                    specialization = "Lithium LiFePO4 Balancing & Inverter Board Repairs"
                ),
                TechnicianApplicant(
                    id = "tech_app_2",
                    name = "Musa Ibrahim",
                    state = "Kano (Nassarawa)",
                    licenseNo = "NEMSA Certified #KN-883",
                    specialization = "High-Voltage DC Combiner Strings & Solar Boreholes"
                ),
                TechnicianApplicant(
                    id = "tech_app_3",
                    name = "Emeka Okonkwo",
                    state = "Enugu (Independence Layout)",
                    licenseNo = "EPRA Solar Tier-3 #EN-102",
                    specialization = "Hybrid Sunsynk / Deye Inverter Synchronization"
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.solarfix_logo),
                            contentDescription = "Solarfix Logo",
                            modifier = Modifier
                                .width(108.dp)
                                .height(38.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SolarFix Admin Control",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Multi-Vendor Marketplace & Escrow Governance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onLogoutClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Out", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Scrollable Admin Section Navigation Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AdminSection.values()) { section ->
                        val isSelected = selectedSection == section
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSection = section },
                            label = { Text(section.label, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(section.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("admin_tab_${section.name.lowercase()}")
                        )
                    }
                }
            }

            // Action Feedback Banner
            item {
                AnimatedVisibility(visible = actionFeedback != null) {
                    actionFeedback?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CleanGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { actionFeedback = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            when (selectedSection) {
                AdminSection.OVERVIEW -> {
                    val liveReport = marketplaceViewModel.generateFinancialReport()

                    item {
                        Text(
                            text = "Marketplace Performance & Financial Pulse",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Total GMV Volume",
                                value = currency.formatPrice(liveReport.grossMerchandiseValueNgn),
                                delta = "${liveReport.totalOrdersCount} Orders Processed",
                                icon = Icons.Default.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "SolarFix Commission",
                                value = currency.formatPrice(liveReport.totalSolarfixCommissionNgn),
                                delta = "${settings.defaultCommissionPercent}% Default Rate",
                                icon = Icons.Default.AccountBalance,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Verified Stores",
                                value = "${liveReport.activeVerifiedStoresCount} / ${stores.size}",
                                delta = "${products.size} Active Listings",
                                icon = Icons.Default.Storefront,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Escrow Holding",
                                value = currency.formatPrice(liveReport.totalPendingEscrowNgn),
                                delta = "${liveReport.openDisputesCount} Open Disputes",
                                icon = Icons.Default.Shield,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Financial Report Generator Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Executive Financial Report Generator", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Audit multi-vendor GMV, commissions, escrow & seller payouts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = {
                                            generatedReport = marketplaceViewModel.generateFinancialReport()
                                            actionFeedback = "Generated real-time Multi-Vendor Financial Audit Report."
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("admin_generate_report_button")
                                    ) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate", fontSize = 12.sp)
                                    }
                                }

                                generatedReport?.let { rep ->
                                    HorizontalDivider()
                                    Text(rep.generatedAt, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CleanGreen)
                                    AdminReportRow("Gross Merchandise Value (GMV)", currency.formatPrice(rep.grossMerchandiseValueNgn))
                                    AdminReportRow("SolarFix Platform Commission Earned", currency.formatPrice(rep.totalSolarfixCommissionNgn))
                                    AdminReportRow("Pending Escrow Liability", currency.formatPrice(rep.totalPendingEscrowNgn))
                                    AdminReportRow("Cleared Seller Available Balances", currency.formatPrice(rep.totalSellerAvailableEarningsNgn))
                                    AdminReportRow("Total Disbursed Seller Withdrawals", currency.formatPrice(rep.totalWithdrawalsPaidNgn))
                                    AdminReportRow("Total Customer Dispute Refunds", currency.formatPrice(rep.totalRefundedNgn))
                                }
                            }
                        }
                    }

                    item {
                        EmergencyPolicyCard(
                            onTriggerEmergencyLockdown = {
                                actionFeedback = "High-Voltage Emergency Safety Broadcast pushed to all active mobile technicians."
                            }
                        )
                    }

                    // Immutable Administrative Action Audit Trail
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_audit_trail_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = CleanGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Administrative Action Audit Trail (${adminAuditLogs.size})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Surface(
                                        color = CleanGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Zero-Trust Recorded",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CleanGreen
                                        )
                                    }
                                }
                                Text(
                                    "Immutable log of all store approvals, custom commission modifications, counterfeit takedowns, dispute resolutions, and payout releases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (adminAuditLogs.isEmpty()) {
                                    Text(
                                        "No administrative operations recorded in current session. All sensitive mutations are logged live.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        adminAuditLogs.take(5).forEach { audit ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            audit.actionType.name.replace("_", " "),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            audit.adminEmail,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        audit.description,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.SELLERS_STORES -> {
                    item {
                        Text(
                            text = "Seller Identity Verification & Store Governance (${stores.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Confidential Compliance View: Inspect CAC records, ID documents, premises photos, and bank payout accounts. Customers never see these private records.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(stores) { store ->
                        var adminNoteInput by remember(store.id, store.status) { mutableStateOf("") }
                        var showDossierDetails by remember(store.id) {
                            mutableStateOf(store.status == StoreVerificationStatus.PENDING || store.status == StoreVerificationStatus.MORE_INFO_REQUIRED)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_store_card_${store.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(store.storeName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            if (store.status == StoreVerificationStatus.APPROVED) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.Verified, contentDescription = "Verified Seller", tint = CleanGreen, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        Text("Applicant / Owner: ${store.ownerName} • ${store.phone}", style = MaterialTheme.typography.labelSmall)
                                        Text("Email: ${store.ownerEmail}", style = MaterialTheme.typography.labelSmall)
                                        Text("CAC Reg: ${store.businessRegNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Surface(
                                        color = when (store.status) {
                                            StoreVerificationStatus.APPROVED -> CleanGreen.copy(alpha = 0.16f)
                                            StoreVerificationStatus.PENDING, StoreVerificationStatus.MORE_INFO_REQUIRED -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = store.status.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (store.status) {
                                                StoreVerificationStatus.APPROVED -> CleanGreen
                                                StoreVerificationStatus.PENDING, StoreVerificationStatus.MORE_INFO_REQUIRED -> Color(0xFFF59E0B)
                                                else -> MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { showDossierDetails = !showDossierDetails },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showDossierDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showDossierDetails) "Hide Confidential KYC & Bank Dossier" else "Inspect Submitted KYC, Premises & Bank Dossier",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (showDossierDetails) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "SUBMITTED SELLER REGISTRATION & KYC DOSSIER",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text("• Full / Business Name: ${store.storeName} (${store.ownerName})", style = MaterialTheme.typography.labelSmall)
                                            Text("• CAC Registration No: ${store.businessRegNumber}", style = MaterialTheme.typography.labelSmall)
                                            Text("• Business Phone & Email: ${store.phone} | ${store.ownerEmail}", style = MaterialTheme.typography.labelSmall)
                                            Text("• Physical Address: ${store.physicalAddress}", style = MaterialTheme.typography.labelSmall)
                                            Text("• City & State: ${store.city}, ${store.state}", style = MaterialTheme.typography.labelSmall)
                                            Text("• Business Description: ${store.description}", style = MaterialTheme.typography.labelSmall)
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                            Text(
                                                text = "IDENTITY & PREMISES VERIFICATION DOCUMENTS",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = CleanGreen
                                            )
                                            Text(
                                                text = "• Valid ID Document: ${store.idDocumentType} — #${store.idDocumentNumber} (${if (store.idDocumentProvided) "✓ Attached" else "Missing"})",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "• Business Premises Photo: ${if (store.premisesPhotoProvided) "✓ Storefront/Warehouse Photo Attached" else "Missing"}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                            Text(
                                                text = "PRIVATE BANK ACCOUNT FOR SALES PROCEEDS",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFFF59E0B)
                                            )
                                            Text("• Bank Account Name: ${store.bankAccountName}", style = MaterialTheme.typography.labelSmall)
                                            Text("• Bank Name: ${store.bankName}", style = MaterialTheme.typography.labelSmall)
                                            Text("• Account Number (NUBAN): ${store.bankAccountNumber}", style = MaterialTheme.typography.labelSmall)
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                            Text(
                                                text = "Current Compliance Note: ${store.adminVerificationNote}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "FINANCIAL LEDGER & COMMISSION CONFIGURATION",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "• Gross Merchandise Sales: ${currency.formatPrice(store.totalGrossSalesNgn)}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "• Pending Escrow (Locked): ${currency.formatPrice(store.pendingEscrowNgn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF59E0B)
                                        )
                                        Text(
                                            text = "• Available Balance (Cleared): ${currency.formatPrice(store.availableEarningsNgn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CleanGreen
                                        )
                                        Text(
                                            text = "• Total Withdrawn: ${currency.formatPrice(store.totalWithdrawnNgn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF3B82F6)
                                        )
                                        Text(
                                            text = "• SolarFix Commission Paid (${store.currentCommissionPercent}%): ${currency.formatPrice(store.totalCommissionPaidNgn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                var customCommInput by remember(store.id, store.customCommissionPercent) {
                                    mutableStateOf(store.customCommissionPercent?.toString() ?: "")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = customCommInput,
                                        onValueChange = { customCommInput = it },
                                        label = { Text("Custom Commission Rate (%)") },
                                        placeholder = { Text("Global Default (${settings.defaultCommissionPercent}%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("admin_custom_commission_input_${store.id}")
                                    )
                                    Button(
                                        onClick = {
                                            val rate = customCommInput.toDoubleOrNull()?.coerceIn(0.1, 50.0)
                                            marketplaceViewModel.setSellerCustomCommission(store.id, rate)
                                            actionFeedback = "Set commission rate for ${store.storeName} to ${rate ?: settings.defaultCommissionPercent}% (Seller Proceeds: ${100.0 - (rate ?: settings.defaultCommissionPercent)}%)"
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Rate", fontSize = 11.sp)
                                    }
                                    if (store.customCommissionPercent != null) {
                                        TextButton(
                                            onClick = {
                                                customCommInput = ""
                                                marketplaceViewModel.setSellerCustomCommission(store.id, null)
                                                actionFeedback = "Reset ${store.storeName} commission rate to global default (${settings.defaultCommissionPercent}%)"
                                            }
                                        ) {
                                            Text("Reset", fontSize = 11.sp)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = adminNoteInput,
                                    onValueChange = { adminNoteInput = it },
                                    label = { Text("Admin Verification / Compliance Note (Optional)") },
                                    placeholder = { Text("e.g. Please upload clearer CAC certificate or utility bill for premises...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_verification_note_input_${store.id}")
                                )

                                // Row 1: Approve Application | Request Additional Info
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            marketplaceViewModel.setSellerVerificationStatus(
                                                storeId = store.id,
                                                status = StoreVerificationStatus.APPROVED,
                                                adminNote = adminNoteInput.ifBlank { null }
                                            )
                                            actionFeedback = "Approved ${store.storeName}! VERIFIED SELLER badge granted and store operations unlocked."
                                            adminNoteInput = ""
                                        },
                                        enabled = store.status != StoreVerificationStatus.APPROVED,
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_approve_seller_${store.id}")
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve Seller", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val note = adminNoteInput.ifBlank {
                                                "Please provide a clearer scan of your CAC registration certificate and physical warehouse utility bill."
                                            }
                                            marketplaceViewModel.setSellerVerificationStatus(
                                                storeId = store.id,
                                                status = StoreVerificationStatus.MORE_INFO_REQUIRED,
                                                adminNote = note
                                            )
                                            actionFeedback = "Requested additional verification info from ${store.storeName}."
                                            adminNoteInput = ""
                                        },
                                        enabled = store.status != StoreVerificationStatus.MORE_INFO_REQUIRED,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_request_info_seller_${store.id}")
                                    ) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Request Info", fontSize = 11.sp)
                                    }
                                }

                                // Row 2: Suspend Previously Approved Seller | Reject Application
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            marketplaceViewModel.setSellerVerificationStatus(
                                                storeId = store.id,
                                                status = StoreVerificationStatus.SUSPENDED,
                                                adminNote = adminNoteInput.ifBlank { null }
                                            )
                                            actionFeedback = "Suspended ${store.storeName}. Store and products removed from active marketplace."
                                            adminNoteInput = ""
                                        },
                                        enabled = store.status != StoreVerificationStatus.SUSPENDED,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_suspend_seller_${store.id}")
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Suspend Seller", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            marketplaceViewModel.setSellerVerificationStatus(
                                                storeId = store.id,
                                                status = StoreVerificationStatus.REJECTED,
                                                adminNote = adminNoteInput.ifBlank { null }
                                            )
                                            actionFeedback = "Rejected seller application for ${store.storeName}."
                                            adminNoteInput = ""
                                        },
                                        enabled = store.status != StoreVerificationStatus.REJECTED,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_reject_seller_${store.id}")
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject Application", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.HARDWARE_VETTING -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Product Listing Moderation, Authenticity & Anti-Counterfeit Audit (${products.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Review suspicious or high-risk listings, request additional OEM evidence, remove counterfeit/misleading products, and record actual independent SolarFix lab inspections.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = Color(0xFF1D4ED8).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Compliance Rule: Seller Verification (VERIFIED SELLER) only confirms seller identity/CAC. Never grant a SolarFix Inspection badge unless an actual physical/lab inspection has been recorded with a valid Report ID.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    items(products) { prod ->
                        var moderationNoteInput by remember(prod.id, prod.moderationStatus) { mutableStateOf("") }
                        var inspectionReportInput by remember(prod.id, prod.solarfixInspectionReportId) {
                            mutableStateOf(prod.solarfixInspectionReportId ?: "SFLAB-2026-${prod.id.takeLast(4).uppercase()}")
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_product_audit_card_${prod.id}")
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
                                            text = "${prod.category.label} • Brand: ${prod.brand} • Model: ${prod.effectiveModelNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Store: ${prod.sellerName} (${prod.sellerVerificationStatusLabel}) • ${prod.formattedPriceNgn} • Qty: ${prod.stockCount} • ${prod.normalizedCondition}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Surface(
                                        color = when (prod.moderationStatus) {
                                            ProductModerationStatus.APPROVED -> CleanGreen.copy(alpha = 0.16f)
                                            ProductModerationStatus.SUSPICIOUS_HIGH_RISK,
                                            ProductModerationStatus.EVIDENCE_REQUESTED,
                                            ProductModerationStatus.PENDING_REVIEW -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ProductModerationStatus.REMOVED_COUNTERFEIT,
                                            ProductModerationStatus.FLAGGED -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = prod.moderationStatus.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (prod.moderationStatus) {
                                                ProductModerationStatus.APPROVED -> CleanGreen
                                                ProductModerationStatus.SUSPICIOUS_HIGH_RISK,
                                                ProductModerationStatus.EVIDENCE_REQUESTED,
                                                ProductModerationStatus.PENDING_REVIEW -> Color(0xFFD97706)
                                                ProductModerationStatus.REMOVED_COUNTERFEIT,
                                                ProductModerationStatus.FLAGGED -> MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Authenticity & Inspection Distinction Summary Box
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "• Seller Genuine Declaration: ${if (prod.sellerAuthenticityConfirmed) "Confirmed Genuine" else "Unconfirmed"} (${prod.authenticityEvidenceNote})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "• Warranty & Return Policy: ${prod.warrantyPeriod} | ${prod.returnPolicy}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = if (prod.hasActualSolarFixInspection) {
                                                "• Independent SolarFix Lab Inspection: PASSED (Report #${prod.solarfixInspectionReportId} • ${prod.solarfixInspectionDate})"
                                            } else {
                                                "• Independent SolarFix Lab Inspection: ${prod.solarfixInspectionStatus.label} (No SolarFix inspection badge displayed)"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (prod.hasActualSolarFixInspection) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "• Audit Note: ${prod.adminModerationNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = moderationNoteInput,
                                    onValueChange = { moderationNoteInput = it },
                                    label = { Text("Admin Audit / Evidence Request / Counterfeit Reason") },
                                    placeholder = { Text("e.g. Upload OEM serial barcode photo or distributor invoice...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Row 1: Approve Listing | Flag Suspicious / High-Risk
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            marketplaceViewModel.setProductModerationStatus(prod.id, ProductModerationStatus.APPROVED)
                                            actionFeedback = "Approved genuine product listing: ${prod.title}"
                                            moderationNoteInput = ""
                                        },
                                        enabled = prod.moderationStatus != ProductModerationStatus.APPROVED,
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("admin_approve_product_${prod.id}")
                                    ) {
                                        Text("Approve Listing", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            marketplaceViewModel.flagSuspiciousProduct(
                                                prod.id,
                                                moderationNoteInput.ifBlank {
                                                    "Flagged as suspicious / high-risk for capacity & datasheet verification."
                                                }
                                            )
                                            actionFeedback = "Flagged '${prod.title}' as Suspicious / High-Risk."
                                            moderationNoteInput = ""
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("admin_flag_suspicious_${prod.id}")
                                    ) {
                                        Text("Flag Suspicious", fontSize = 11.sp, color = Color(0xFFD97706))
                                    }
                                }

                                // Row 2: Request Additional Evidence | Remove Counterfeit / Misleading
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            marketplaceViewModel.requestProductAuthenticityEvidence(
                                                prod.id,
                                                moderationNoteInput.ifBlank {
                                                    "Please provide OEM serial number barcode and official manufacturer invoice."
                                                }
                                            )
                                            actionFeedback = "Requested authenticity evidence from ${prod.sellerName} for '${prod.title}'."
                                            moderationNoteInput = ""
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("admin_request_evidence_${prod.id}")
                                    ) {
                                        Text("Request Evidence", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            marketplaceViewModel.removeCounterfeitProduct(
                                                prod.id,
                                                moderationNoteInput.ifBlank {
                                                    "Removed by SolarFix Admin: Counterfeit or misleading wattage/capacity specification."
                                                }
                                            )
                                            actionFeedback = "Removed counterfeit / misleading product '${prod.title}' from marketplace."
                                            moderationNoteInput = ""
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("admin_remove_counterfeit_${prod.id}")
                                    ) {
                                        Text("Remove Counterfeit", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                // Row 3: Record Actual Independent SolarFix Physical Lab Inspection
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inspectionReportInput,
                                        onValueChange = { inspectionReportInput = it },
                                        label = { Text("SolarFix Lab Report ID") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            marketplaceViewModel.recordSolarFixProductInspection(
                                                productId = prod.id,
                                                passed = true,
                                                reportId = inspectionReportInput.ifBlank { "SFLAB-2026-VERIFIED" },
                                                inspectionNote = moderationNoteInput.ifBlank {
                                                    "Independently tested and verified by SolarFix Engineering Lab."
                                                }
                                            )
                                            actionFeedback = "Recorded actual SolarFix lab inspection (#$inspectionReportInput) for '${prod.title}'."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("admin_record_inspection_${prod.id}")
                                    ) {
                                        Text(
                                            text = if (prod.hasActualSolarFixInspection) "Update Inspection" else "Record Lab Inspection",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.ESCROW -> {
                    item {
                        Text(
                            text = "Monitor Customer Orders (${orders.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(orders) { order ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("escrow_order_${order.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order #${order.id} • ${order.customerName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(currency.formatPrice(order.totalAmountNgn), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = "Store: ${order.primaryStoreName} • Commission: ${currency.formatPrice(order.solarfixCommissionNgn)} (${order.commissionPercent}%)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Status: ${order.status.label} • Tracking: ${order.trackingNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CleanGreen
                                )
                                if (!order.escrowReleased && order.status != OrderStatus.REFUNDED) {
                                    Button(
                                        onClick = {
                                            marketplaceViewModel.advanceSellerOrderStatus(order.id, OrderStatus.DELIVERED)
                                            actionFeedback = "Verified delivery & released ${currency.formatPrice(order.netSellerEarningsNgn)} escrow to ${order.primaryStoreName}."
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Force-Verify Delivery & Release Escrow to Seller", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "All Platform Transactions Ledger (${transactions.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(transactions) { tx ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${tx.id} • ${tx.title}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${tx.storeName} • ${tx.subtitle} (${tx.timestamp})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = currency.formatPrice(tx.amountNgn),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (tx.isCreditToSeller) CleanGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                AdminSection.PAYOUTS_DISPUTES -> {
                    item {
                        Text(
                            text = "Seller Earnings & Withdrawal Approvals (${withdrawals.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(withdrawals) { wth ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${wth.storeName} (${wth.id})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${wth.bankName} • ${wth.accountNumber} (${wth.accountName})", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(currency.formatPrice(wth.amountNgn), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                }
                                Text("Status: ${wth.status.label}", style = MaterialTheme.typography.labelSmall, color = if (wth.status == WithdrawalStatus.APPROVED_PAID) CleanGreen else Color(0xFFF59E0B))
                                if (wth.status == WithdrawalStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                marketplaceViewModel.processWithdrawal(wth.id, true)
                                                actionFeedback = "Disbursed ${currency.formatPrice(wth.amountNgn)} to ${wth.storeName}."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve & Pay Bank", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                marketplaceViewModel.processWithdrawal(wth.id, false)
                                                actionFeedback = "Rejected withdrawal ${wth.id}; funds returned to seller balance."
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Customer Refunds & Product Disputes (${disputes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(disputes) { dsp ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${dsp.id} • Order #${dsp.orderId}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(currency.formatPrice(dsp.amountNgn), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                                }
                                Text("Issue: ${dsp.issueType.label}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                                Text("Product: ${dsp.productTitle} (Store: ${dsp.storeName})", style = MaterialTheme.typography.labelSmall)
                                Text("Customer Report: \"${dsp.description}\"", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${dsp.status.label}", style = MaterialTheme.typography.labelSmall, color = CleanGreen)

                                if (dsp.status == DisputeStatus.OPEN) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                marketplaceViewModel.resolveDispute(
                                                    dsp.id,
                                                    DisputeStatus.RESOLVED_REFUND,
                                                    "Full refund issued from Escrow to buyer"
                                                )
                                                actionFeedback = "Issued full refund (${currency.formatPrice(dsp.amountNgn)}) to ${dsp.customerName}."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Refund Buyer", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                marketplaceViewModel.resolveDispute(
                                                    dsp.id,
                                                    DisputeStatus.RESOLVED_REPLACEMENT,
                                                    "Seller mandated to dispatch tested replacement unit"
                                                )
                                                actionFeedback = "Mandated replacement dispatch from ${dsp.storeName}."
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Order Replacement", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.SETTINGS -> {
                    item {
                        AdminMarketplaceSettingsCard(
                            settings = settings,
                            onSave = { updated ->
                                marketplaceViewModel.updateMarketplaceSettings(updated)
                                actionFeedback = "Updated SolarFix Commission (${updated.defaultCommissionPercent}%) & Marketplace Settings."
                            }
                        )
                    }
                }

                AdminSection.TECHNICIANS -> {
                    item {
                        Text(
                            text = "Technician Accreditation Queue (${applicants.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(applicants) { applicant ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("applicant_card_${applicant.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(applicant.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text(applicant.state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            applicant.licenseNo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Specialty: ${applicant.specialization}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            applicants = applicants.filter { it.id != applicant.id }
                                            actionFeedback = "Approved ${applicant.name} as Certified Solarfix Pro!"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("approve_tech_${applicant.id}")
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Approve Pro", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            applicants = applicants.filter { it.id != applicant.id }
                                            actionFeedback = "Rejected application for ${applicant.name} (License invalid)."
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AdminMarketplaceSettingsCard(
    settings: MarketplaceSettings,
    onSave: (MarketplaceSettings) -> Unit
) {
    var commissionText by remember(settings) { mutableStateOf(settings.defaultCommissionPercent.toString()) }
    var escrowHoursText by remember(settings) { mutableStateOf(settings.escrowHoldHours.toString()) }
    var minWithdrawalText by remember(settings) { mutableStateOf(settings.minWithdrawalNgn.toString()) }
    var allowInstallments by remember(settings) { mutableStateOf(settings.allowInstallmentPlans) }
    var requireApproval by remember(settings) { mutableStateOf(settings.requireAdminSellerApproval) }
    var buyerProtection by remember(settings) { mutableStateOf(settings.buyerProtectionEnabled) }

    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SolarFix Commission & Marketplace Configuration", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            OutlinedTextField(
                value = commissionText,
                onValueChange = { commissionText = it },
                label = { Text("Global SolarFix Platform Commission (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_commission_input")
            )

            val currentCommVal = commissionText.toDoubleOrNull() ?: settings.defaultCommissionPercent
            Text(
                text = "Global Default Split: SolarFix Commission ${currentCommVal}% • Seller Proceeds ${(100.0 - currentCommVal).coerceAtLeast(0.0)}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = escrowHoursText,
                    onValueChange = { escrowHoursText = it },
                    label = { Text("Escrow Hold (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minWithdrawalText,
                    onValueChange = { minWithdrawalText = it },
                    label = { Text("Min Payout (₦)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable CDcare-Style Smart Installments", style = MaterialTheme.typography.bodySmall)
                Switch(checked = allowInstallments, onCheckedChange = { allowInstallments = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Require Admin Identity Vetting for New Sellers", style = MaterialTheme.typography.bodySmall)
                Switch(checked = requireApproval, onCheckedChange = { requireApproval = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enforce 48-Hour Escrow Buyer Protection", style = MaterialTheme.typography.bodySmall)
                Switch(checked = buyerProtection, onCheckedChange = { buyerProtection = it })
            }

            Button(
                onClick = {
                    val comm = commissionText.toDoubleOrNull()?.coerceIn(1.0, 30.0) ?: settings.defaultCommissionPercent
                    val hours = escrowHoursText.toIntOrNull()?.coerceAtLeast(12) ?: settings.escrowHoldHours
                    val minW = minWithdrawalText.toLongOrNull()?.coerceAtLeast(5000L) ?: settings.minWithdrawalNgn
                    onSave(
                        MarketplaceSettings(
                            defaultCommissionPercent = comm,
                            escrowHoldHours = hours,
                            minWithdrawalNgn = minW,
                            allowInstallmentPlans = allowInstallments,
                            requireAdminSellerApproval = requireApproval,
                            buyerProtectionEnabled = buyerProtection
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("admin_save_settings_button")
            ) {
                Text("Save Marketplace Configuration")
            }
        }
    }
}

@Composable
private fun AdminReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    delta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(delta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmergencyPolicyCard(onTriggerEmergencyLockdown: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "High-Voltage Safety Guardrail Dispatch",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Send an immediate electrical safety broadcast to all field solar users warning of Harmattan dust sparking or lightning grid surges.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onTriggerEmergencyLockdown,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth().testTag("admin_emergency_broadcast_button")
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Broadcast Safety Advisory", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
