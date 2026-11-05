package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.legal.DataDomainProvisionDetail
import com.example.data.legal.PrivacyPolicyAndDataProtectionManager
import com.example.data.legal.PrivacyPolicySection
import com.example.data.legal.PrivacyPolicySectionTopic
import com.example.data.legal.SpecificDataDomainProvision
import com.example.ui.theme.CleanGreen

/**
 * Full-screen Modal Dialog wrapper for the SolarFix Privacy Policy & Data Protection Center.
 * Accessible from Registration, Login, Account Settings, Terms of Service, and Application Footer
 * without losing user form state.
 */
@Composable
fun PrivacyPolicyModalDialog(
    entryPointLabel: String = "SolarFix Privacy Center",
    userIdentifier: String = "active_user",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("privacy_policy_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            PrivacyPolicyScreen(
                entryPointLabel = entryPointLabel,
                userIdentifier = userIdentifier,
                onBackClick = onDismiss
            )
        }
    }
}

/**
 * Comprehensive Privacy Policy and Data Protection Center inside SolarFix.
 *
 * Covers:
 * - All 15 mandatory core privacy & data protection areas.
 * - All 8 specific data domain provisions (Customer info, Seller verification docs, Bank accounts,
 *   Payment records, Delivery addresses, AI Technician interactions, Uploaded media/docs, Transaction history).
 * - Interactive Privacy-by-Design Consent Management & Data Subject Rights (Access, Correction,
 *   Deletion, Consent Withdrawal, and Privacy Complaints).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    entryPointLabel: String = "SolarFix Privacy Center",
    userIdentifier: String = "active_user",
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeViewTab by remember { mutableStateOf("ALL") } // ALL, 15_SECTIONS, 8_DOMAINS, PRIVACY_CONTROLS
    var selectedSectionTopic by remember { mutableStateOf<PrivacyPolicySectionTopic?>(null) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }

    var consentState by remember(userIdentifier) {
        mutableStateOf(PrivacyPolicyAndDataProtectionManager.getConsentPreferences(userIdentifier))
    }
    var correctionFieldInput by remember { mutableStateOf("") }
    var correctionValueInput by remember { mutableStateOf("") }
    var complaintInput by remember { mutableStateOf("") }

    val dsarHistory by PrivacyPolicyAndDataProtectionManager.dsarRecordsFlow.collectAsStateWithLifecycle()

    val filteredCoreSections = remember(searchQuery, selectedSectionTopic) {
        PrivacyPolicyAndDataProtectionManager.searchPrivacySections(searchQuery).filter { sec ->
            selectedSectionTopic == null || sec.topic == selectedSectionTopic
        }
    }

    val filteredDomainProvisions = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            PrivacyPolicyAndDataProtectionManager.specificDomainProvisions
        } else {
            PrivacyPolicyAndDataProtectionManager.specificDomainProvisions.filter { dom ->
                dom.domain.title.lowercase().contains(q) ||
                    dom.exactFieldsCollected.any { it.lowercase().contains(q) } ||
                    dom.excludedByDataMinimization.any { it.lowercase().contains(q) } ||
                    dom.encryptionAndStorageMechanism.lowercase().contains(q) ||
                    dom.accessAndSharingPolicy.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Privacy Policy & Data Protection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Version ${PrivacyPolicyAndDataProtectionManager.CURRENT_PRIVACY_POLICY_VERSION} • NDPA 2023 Compliant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("close_privacy_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Surface(
                        color = CleanGreen.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CleanGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AES-256-GCM",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CleanGreen
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val record = PrivacyPolicyAndDataProtectionManager.exportUserPersonalDataSnapshot(
                                userIdentifier = userIdentifier
                            )
                            statusBannerMessage = "${record.requestId}: ${record.resolutionSummary}"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("privacy_export_data_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export My Data", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("privacy_done_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("privacy_policy_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // 1. Privacy-by-Design & Data Minimization Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("privacy_by_design_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF071B2F),
                                        Color(0xFF0E3A5D),
                                        Color(0xFF065F46)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PRIVACY-BY-DESIGN & DATA MINIMIZATION",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "• $entryPointLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Text(
                                text = "SolarFix Data Protection & Encryption Architecture",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )

                            Text(
                                text = PrivacyPolicyAndDataProtectionManager.PRIVACY_BY_DESIGN_STATEMENT,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // Status Banner for DSAR actions
            if (statusBannerMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_dsar_status_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusBannerMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { statusBannerMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 2. Search Bar & Navigation Mode Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Privacy Policy (e.g. AES-256, KYC, Paystack, deletion, breach)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Privacy Policy")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeViewTab == "ALL",
                            onClick = { activeViewTab = "ALL" },
                            label = { Text("Complete Policy & Controls") },
                            modifier = Modifier.testTag("privacy_tab_all")
                        )
                        FilterChip(
                            selected = activeViewTab == "8_DOMAINS",
                            onClick = { activeViewTab = "8_DOMAINS" },
                            label = { Text("8 Data Domains (A–H)") },
                            modifier = Modifier.testTag("privacy_tab_domains")
                        )
                        FilterChip(
                            selected = activeViewTab == "15_SECTIONS",
                            onClick = { activeViewTab = "15_SECTIONS" },
                            label = { Text("15 Policy Clauses") },
                            modifier = Modifier.testTag("privacy_tab_sections")
                        )
                        FilterChip(
                            selected = activeViewTab == "PRIVACY_CONTROLS",
                            onClick = { activeViewTab = "PRIVACY_CONTROLS" },
                            label = { Text("My Data Rights & Consent") },
                            modifier = Modifier.testTag("privacy_tab_controls")
                        )
                    }
                }
            }

            // 3. Interactive Data Subject Rights & Consent Controls Card
            if (activeViewTab == "ALL" || activeViewTab == "PRIVACY_CONTROLS") {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_interactive_controls_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Interactive Privacy & Consent Controls",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        text = "Manage optional processing consent or exercise your NDPA Data Subject Rights",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Consent Toggle 1: AI Technician Diagnostic History
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Save AI Technician Diagnostic History (Encrypted Local DB)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Store past inverter fault chats locally using AES-256-GCM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = consentState.aiTechnicianDiagnosticHistoryRetention,
                                    onCheckedChange = { checked ->
                                        consentState = PrivacyPolicyAndDataProtectionManager.updateConsentPreferences(
                                            userIdentifier = userIdentifier,
                                            aiTechnicianDiagnosticHistoryRetention = checked,
                                            localizedSolarIrradianceOptimization = consentState.localizedSolarIrradianceOptimization,
                                            productSafetyAndFeatureNotifications = consentState.productSafetyAndFeatureNotifications
                                        )
                                        statusBannerMessage = "Updated AI Technician history consent: $checked"
                                    },
                                    modifier = Modifier.testTag("consent_toggle_ai_history")
                                )
                            }

                            // Consent Toggle 2: Localized Solar Irradiance Optimization
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Localized Solar Peak-Sun Weather Estimates",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Use city-level solar irradiance parameters for daily kWh yield estimates",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = consentState.localizedSolarIrradianceOptimization,
                                    onCheckedChange = { checked ->
                                        consentState = PrivacyPolicyAndDataProtectionManager.updateConsentPreferences(
                                            userIdentifier = userIdentifier,
                                            aiTechnicianDiagnosticHistoryRetention = consentState.aiTechnicianDiagnosticHistoryRetention,
                                            localizedSolarIrradianceOptimization = checked,
                                            productSafetyAndFeatureNotifications = consentState.productSafetyAndFeatureNotifications
                                        )
                                        statusBannerMessage = "Updated Solar Irradiance optimization consent: $checked"
                                    },
                                    modifier = Modifier.testTag("consent_toggle_weather")
                                )
                            }

                            // Consent Toggle 3: Product Safety & Feature Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "High-Voltage Recall & Safety Alerts",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Optional notifications for firmware updates and battery safety advisories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = consentState.productSafetyAndFeatureNotifications,
                                    onCheckedChange = { checked ->
                                        consentState = PrivacyPolicyAndDataProtectionManager.updateConsentPreferences(
                                            userIdentifier = userIdentifier,
                                            aiTechnicianDiagnosticHistoryRetention = consentState.aiTechnicianDiagnosticHistoryRetention,
                                            localizedSolarIrradianceOptimization = consentState.localizedSolarIrradianceOptimization,
                                            productSafetyAndFeatureNotifications = checked
                                        )
                                        statusBannerMessage = "Updated Safety Notifications consent: $checked"
                                    },
                                    modifier = Modifier.testTag("consent_toggle_notifications")
                                )
                            }

                            // One-tap Withdraw All Optional Consents + Request Deletion Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        consentState = PrivacyPolicyAndDataProtectionManager.withdrawAllOptionalConsents(userIdentifier)
                                        statusBannerMessage = "All optional processing consents have been withdrawn immediately."
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("privacy_withdraw_all_consent_button")
                                ) {
                                    Icon(Icons.Default.RemoveModerator, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Withdraw Consent", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val rec = PrivacyPolicyAndDataProtectionManager.submitDataDeletionRequest(
                                            userIdentifier = userIdentifier,
                                            reason = "In-app erasure request"
                                        )
                                        consentState = PrivacyPolicyAndDataProtectionManager.getConsentPreferences(userIdentifier)
                                        statusBannerMessage = "${rec.requestId}: ${rec.resolutionSummary}"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("privacy_deletion_request_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Request Deletion", fontSize = 11.sp)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Request Correction of Inaccurate Information
                            Text(
                                text = "Request Correction of Inaccurate Information",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = correctionFieldInput,
                                    onValueChange = { correctionFieldInput = it },
                                    label = { Text("Field (e.g. Phone, Address, Bank)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("privacy_correction_field_input")
                                )
                                OutlinedTextField(
                                    value = correctionValueInput,
                                    onValueChange = { correctionValueInput = it },
                                    label = { Text("Corrected Value") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("privacy_correction_value_input")
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val rec = PrivacyPolicyAndDataProtectionManager.submitDataCorrectionRequest(
                                        userIdentifier = userIdentifier,
                                        fieldToCorrect = correctionFieldInput.ifBlank { "Account Contact Info" },
                                        updatedValueSummary = correctionValueInput.ifBlank { "Updated accurate record" }
                                    )
                                    correctionFieldInput = ""
                                    correctionValueInput = ""
                                    statusBannerMessage = "${rec.requestId}: ${rec.resolutionSummary}"
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("privacy_correction_request_button")
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Data Correction Request")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Raise a Privacy Complaint with DPO
                            Text(
                                text = "Raise a Privacy Complaint (SolarFix DPO & NDPC Escalation)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = complaintInput,
                                onValueChange = { complaintInput = it },
                                label = { Text("Describe your privacy concern or question for our DPO") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("privacy_complaint_input")
                            )
                            FilledTonalButton(
                                onClick = {
                                    val rec = PrivacyPolicyAndDataProtectionManager.submitPrivacyComplaint(
                                        userIdentifier = userIdentifier,
                                        complaintDetails = complaintInput.ifBlank { "Privacy inquiry submitted via in-app DPO desk" }
                                    )
                                    complaintInput = ""
                                    statusBannerMessage = "${rec.requestId}: ${rec.resolutionSummary}"
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("privacy_complaint_submit_button")
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Privacy Complaint to DPO")
                            }

                            if (dsarHistory.isNotEmpty()) {
                                Text(
                                    text = "Recent Privacy Requests (${dsarHistory.size}): Latest ${dsarHistory.first().requestId} — ${dsarHistory.first().requestType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CleanGreen
                                )
                            }
                        }
                    }
                }
            }

            // 4. Specific Provisions for All 8 Data Domains (A–H)
            if (activeViewTab == "ALL" || activeViewTab == "8_DOMAINS") {
                item {
                    Text(
                        text = "Specific Data Domain Provisions (A–H)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(
                    items = filteredDomainProvisions,
                    key = { it.domain.name }
                ) { domainDetail ->
                    SpecificDataDomainCard(detail = domainDetail)
                }
            }

            // 5. All 15 Core Privacy Policy Sections
            if (activeViewTab == "ALL" || activeViewTab == "15_SECTIONS") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Comprehensive Privacy Policy Clauses (1–15)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { selectedSectionTopic = null },
                                label = { Text("All 15 Clauses") }
                            )
                            PrivacyPolicySectionTopic.entries.forEach { topic ->
                                FilterChip(
                                    selected = selectedSectionTopic == topic,
                                    onClick = {
                                        selectedSectionTopic = if (selectedSectionTopic == topic) null else topic
                                    },
                                    label = { Text("${topic.sectionNumber}. ${topic.shortLabel}") }
                                )
                            }
                        }
                    }
                }

                items(
                    items = filteredCoreSections,
                    key = { it.topic.name }
                ) { section ->
                    PrivacyPolicySectionCard(section = section)
                }
            }

            // 6. DPO Contact & Supervisory Authority Footer
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("privacy_dpo_footer_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SolarFix Data Protection Officer (DPO) & Supervisory Authority",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "• Data Protection Officer: ${PrivacyPolicyAndDataProtectionManager.DATA_PROTECTION_OFFICER_EMAIL}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Privacy Ombudsman Desk: ${PrivacyPolicyAndDataProtectionManager.PRIVACY_COMPLAINTS_EMAIL}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Supervisory Authority: ${PrivacyPolicyAndDataProtectionManager.SUPERVISORY_AUTHORITY}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecificDataDomainCard(detail: DataDomainProvisionDetail) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("privacy_domain_card_${detail.domain.domainNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = detail.domain.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Domain Provision"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "What SolarFix Collects:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    detail.exactFieldsCollected.forEach { field ->
                        Text(
                            text = "• $field",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Strictly Excluded (Data Minimization):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    detail.excludedByDataMinimization.forEach { excluded ->
                        Text(
                            text = "✗ $excluded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = CleanGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Encryption & Storage: ${detail.encryptionAndStorageMechanism}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CleanGreen
                            )
                            Text(
                                text = "Access & Sharing: ${detail.accessAndSharingPolicy}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Retention Schedule: ${detail.retentionPeriod}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicySectionCard(section: PrivacyPolicySection) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("privacy_section_${section.topic.sectionNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${section.topic.sectionNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = section.topic.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = section.summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Privacy Clause"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    section.detailedClauses.forEach { clause ->
                        Text(
                            text = clause,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                    Text(
                        text = "Enforced in Codebase: ${section.implementationReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
