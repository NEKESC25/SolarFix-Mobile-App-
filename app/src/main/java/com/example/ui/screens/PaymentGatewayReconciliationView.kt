package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Currency
import com.example.data.payment.*
import com.example.ui.theme.CleanGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayReconciliationView(
    transactions: List<PaymentVerificationResult>,
    reconciliationRecords: List<PaymentReconciliationRecord>,
    refundRecords: List<GatewayRefundRecord>,
    sellerSubaccounts: List<PaystackSubaccount>,
    currency: Currency,
    onBack: () -> Unit,
    onRunReconciliation: () -> Unit,
    onSimulateWebhook: (reference: String, eventType: String) -> Unit,
    onProcessRefund: (orderId: String, amountNgn: Long, reason: String) -> Unit
) {
    var selectedSection by remember { mutableStateOf("overview") }
    var showRefundModal by remember { mutableStateOf(false) }
    var selectedTxForRefund by remember { mutableStateOf<PaymentVerificationResult?>(null) }
    var refundReasonInput by remember { mutableStateOf("Customer returned goods during 7-day escrow inspection period") }

    val latestRecon = reconciliationRecords.lastOrNull()
    val totalTransactionsCount = transactions.size
    val totalGrossNgn = transactions.filter { it.isVerified && it.status == GatewayPaymentStatus.SUCCESS }
        .sumOf { it.amountNgn }
    val totalRefundedNgn = refundRecords.filter { it.status == "SUCCESS" }
        .sumOf { it.amountNgn }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Paystack Payment & Escrow System", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Nigeria Secure Gateway • Server-Side Verification", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("payment_audit_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onRunReconciliation,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("trigger_reconciliation_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconcile", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("payment_gateway_reconciliation_screen")
        ) {
            // 1. Navigation Chips for Sub-sections
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        FilterChip(
                            selected = selectedSection == "overview",
                            onClick = { selectedSection = "overview" },
                            label = { Text("Security Overview") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("sec_nav_overview")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSection == "reconciliation",
                            onClick = { selectedSection = "reconciliation" },
                            label = { Text("Reconciliation (${reconciliationRecords.size})") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("sec_nav_reconciliation")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSection == "transactions",
                            onClick = { selectedSection = "transactions" },
                            label = { Text("Transactions (${transactions.size})") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("sec_nav_transactions")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSection == "subaccounts",
                            onClick = { selectedSection = "subaccounts" },
                            label = { Text("Splits (${sellerSubaccounts.size})") },
                            leadingIcon = { Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("sec_nav_subaccounts")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSection == "webhooks",
                            onClick = { selectedSection = "webhooks" },
                            label = { Text("Webhook Guard") },
                            leadingIcon = { Icon(Icons.Default.Webhook, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("sec_nav_webhooks")
                        )
                    }
                }
            }

            // 2. OVERVIEW SECTION
            if (selectedSection == "overview") {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2540)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Paystack Nigeria Gateway", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("PCI-DSS Level 1 Encrypted Integration", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00C3F7))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CleanGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Gross Processed", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(currency.formatPrice(totalGrossNgn), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                                Column {
                                    Text("Total Refunds", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(currency.formatPrice(totalRefundedNgn), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF87171))
                                }
                                Column {
                                    Text("Total Transactions", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("$totalTransactionsCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("10 Security & Payment System Compliances", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                val securityChecklist = listOf(
                    "1. Server-Side Verification: Every successful payment is verified against Paystack backend API before order creation." to true,
                    "2. Zero Frontend Trust: Frontend success callbacks are completely discarded; verification relies only on server responses." to true,
                    "3. Webhook Synchronization: Webhooks are validated using HMAC-SHA512 against the 'x-paystack-signature' header with PAYSTACK_SECRET_KEY on the backend." to true,
                    "4. Duplicate Payment Prevention: Processed references are recorded to block replay attacks and duplicate debits." to true,
                    "5. Complete Transaction History: Full audit trail of gateway references, channels, timestamps, and customer emails." to true,
                    "6. Zero Card/PIN Storage: SolarFix never stores customers' card numbers, CVVs, or banking PINs (PCI-DSS)." to true,
                    "7. Unique Payment References: Structured format SF-PAY-YYYYMMDD-XXXXXX ensures collision-free tracking." to true,
                    "8. Proper Refund Handling: Automated refund mechanism reversing escrow funds and updating transaction logs." to true,
                    "9. Strict Amount Matching: Orders are blocked if the verified amount does not match the cart total down to the kobo." to true,
                    "10. Payment Reconciliation: Daily balance audits verifying Gross = Net Seller Split + Commission + Refunds." to true
                )

                items(securityChecklist) { (ruleText, isCompliant) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompliant) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCompliant) CleanGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(ruleText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // 3. RECONCILIATION REPORTS SECTION
            if (selectedSection == "reconciliation") {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().testTag("reconciliation_summary_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Balance Reconciliation", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Surface(
                                    color = if (latestRecon?.status == ReconciliationStatus.BALANCED) CleanGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = latestRecon?.status?.name ?: "BALANCED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (latestRecon?.status == ReconciliationStatus.BALANCED) CleanGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (latestRecon != null) {
                                ReconRowItem("Reconciliation Date", latestRecon.reconciliationDate)
                                ReconRowItem("Total Transactions Audited", "${latestRecon.totalTransactionsCount}")
                                ReconRowItem("Gross Volume", currency.formatPrice(latestRecon.grossVolumeKobo / 100L))
                                ReconRowItem("Net Seller Proceeds", currency.formatPrice(latestRecon.netSellerPayoutKobo / 100L))
                                ReconRowItem("SolarFix Platform Commission", currency.formatPrice(latestRecon.solarfixCommissionKobo / 100L))
                                ReconRowItem("Total Refunds Deducted", currency.formatPrice(latestRecon.totalRefundsKobo / 100L))
                                ReconRowItem("Discrepancy (₦)", "₦${latestRecon.discrepancyKobo / 100}")
                            } else {
                                Text("No reconciliation reports generated yet. Click 'Reconcile' above.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Text("Historical Reconciliation Logs", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                items(reconciliationRecords.reversed()) { recon ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(recon.reconciliationId, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(recon.reconciliationDate, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                text = "Gross: ${currency.formatPrice(recon.grossVolumeKobo / 100L)} • Sellers: ${currency.formatPrice(recon.netSellerPayoutKobo / 100L)} • Commission: ${currency.formatPrice(recon.solarfixCommissionKobo / 100L)} • Refunds: ${currency.formatPrice(recon.totalRefundsKobo / 100L)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 4. TRANSACTIONS & AUDIT LOG SECTION
            if (selectedSection == "transactions") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Verified Payment Transactions (${transactions.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Server Verified", style = MaterialTheme.typography.labelSmall, color = CleanGreen)
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        Text("No verified transactions recorded yet. Complete a checkout to generate an audit log.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                items(transactions) { tx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("transaction_item_${tx.reference}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tx.reference,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Gateway ID: ${tx.gatewayId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (tx.isVerified && tx.status == GatewayPaymentStatus.SUCCESS) CleanGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (tx.isVerified) "VERIFIED" else tx.status.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (tx.isVerified) CleanGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount: ${currency.formatPrice(tx.amountKobo / 100L)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Channel: ${tx.channel.uppercase()}", style = MaterialTheme.typography.labelSmall)
                            }

                            Text("Customer: ${tx.customerEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (tx.isVerified && tx.status == GatewayPaymentStatus.SUCCESS) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedTxForRefund = tx
                                            showRefundModal = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("initiate_refund_${tx.reference}")
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Issue Refund", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. MARKETPLACE SUBACCOUNTS SECTION
            if (selectedSection == "subaccounts") {
                item {
                    Text("Paystack Marketplace Split Subaccounts", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                item {
                    Surface(
                        color = CleanGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Marketplace Payment Splitting: Customer payments are divided automatically upon transaction authorization based on the configured commission rate: eligible net seller proceeds are routed to the seller's verified Nigerian bank subaccount, while SolarFix commission is retained in escrow.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(sellerSubaccounts) { sub ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("subaccount_item_${sub.subaccountCode}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sub.businessName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                    Text(sub.subaccountCode, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            ReconRowItem("Settlement Bank", sub.settlementBank)
                            ReconRowItem("Account Number", sub.accountNumber)
                            ReconRowItem("Seller Share Percentage", "${sub.percentageCharge}%")
                            ReconRowItem("SolarFix Escrow Retained", "${100.0 - sub.percentageCharge}%")
                        }
                    }
                }
            }

            // 6. WEBHOOK SIMULATOR & HMAC GUARD SECTION
            if (selectedSection == "webhooks") {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("webhook_simulator_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Paystack Webhook & HMAC-SHA512 Test Tool", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "Simulate real-time webhooks sent from Paystack servers. Paystack signs payloads with the merchant's PAYSTACK_SECRET_KEY via the x-paystack-signature header. SolarFix verifies this HMAC-SHA512 signature on the backend before processing.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val sampleRef = transactions.firstOrNull()?.reference ?: PaystackPaymentGatewayManager.generateUniquePaymentReference()
                                        onSimulateWebhook(sampleRef, "charge.success")
                                    },
                                    modifier = Modifier.weight(1f).testTag("simulate_charge_webhook_button")
                                ) {
                                    Text("Simulate charge.success", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val sampleRef = transactions.firstOrNull()?.reference ?: PaystackPaymentGatewayManager.generateUniquePaymentReference()
                                        onSimulateWebhook(sampleRef, "transfer.success")
                                    },
                                    modifier = Modifier.weight(1f).testTag("simulate_transfer_webhook_button")
                                ) {
                                    Text("Simulate transfer.success", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Refund Dialog
    if (showRefundModal && selectedTxForRefund != null) {
        val tx = selectedTxForRefund!!
        var refundAmountText by remember { mutableStateOf((tx.amountKobo / 100L).toString()) }

        Dialog(onDismissRequest = { showRefundModal = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Process Gateway Refund", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Reference: ${tx.reference}", style = MaterialTheme.typography.labelSmall)
                    Text("Gateway ID: ${tx.gatewayId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = refundAmountText,
                        onValueChange = { refundAmountText = it },
                        label = { Text("Refund Amount (₦)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("refund_amount_input")
                    )

                    OutlinedTextField(
                        value = refundReasonInput,
                        onValueChange = { refundReasonInput = it },
                        label = { Text("Refund Reason") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("refund_reason_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showRefundModal = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val amt = refundAmountText.toLongOrNull() ?: (tx.amountKobo / 100L)
                                onProcessRefund(tx.reference, amt, refundReasonInput)
                                showRefundModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("submit_refund_button")
                        ) {
                            Text("Refund Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}

