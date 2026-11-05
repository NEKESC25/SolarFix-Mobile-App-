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
import com.example.data.legal.ResponsibilityComparisonEntry
import com.example.data.legal.TermsOfServiceRepository
import com.example.data.legal.TermsOfServiceSection
import com.example.data.legal.TermsSectionTopic
import com.example.ui.theme.CleanGreen

/**
 * Full-screen Modal Dialog wrapper for the SolarFix Terms of Use and Terms of Service.
 * Can be opened from Registration, Login, Account Settings, or Application Footer/About section
 * without losing user form state.
 */
@Composable
fun TermsOfServiceModalDialog(
    entryPointLabel: String = "SolarFix Legal Center",
    userIdentifier: String = "active_user",
    onDismiss: () -> Unit,
    onAcceptTerms: (() -> Unit)? = null
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
                .testTag("terms_of_service_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            TermsOfServiceScreen(
                entryPointLabel = entryPointLabel,
                userIdentifier = userIdentifier,
                onBackClick = onDismiss,
                onAcceptTerms = {
                    TermsOfServiceRepository.recordTermsAcceptance(
                        userIdOrEmail = userIdentifier,
                        entryPoint = entryPointLabel
                    )
                    onAcceptTerms?.invoke()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Dedicated Terms of Use & Terms of Service Screen inside SolarFix.
 *
 * Covers all 21 required legal & operational sections, clearly distinguishes
 * SolarFix's responsibilities from those of independent marketplace sellers,
 * and explicitly preserves customers' statutory legal and consumer protection rights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    entryPointLabel: String = "SolarFix Legal Center",
    userIdentifier: String = "active_user",
    onBackClick: () -> Unit,
    onAcceptTerms: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAudienceFilter by remember { mutableStateOf("ALL") } // ALL, CUSTOMER, SELLER
    var selectedTopicChip by remember { mutableStateOf<TermsSectionTopic?>(null) }
    var showResponsibilityMatrix by remember { mutableStateOf(true) }
    var hasAcceptedInSession by remember {
        mutableStateOf(TermsOfServiceRepository.hasAcceptedLatestTerms(userIdentifier))
    }

    val filteredSections = remember(searchQuery, selectedAudienceFilter, selectedTopicChip) {
        TermsOfServiceRepository.searchSections(searchQuery).filter { section ->
            val matchesAudience = when (selectedAudienceFilter) {
                "CUSTOMER" -> section.appliesToCustomer
                "SELLER" -> section.appliesToSeller
                else -> true
            }
            val matchesTopic = selectedTopicChip == null || section.topic == selectedTopicChip
            matchesAudience && matchesTopic
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Terms of Use & Terms of Service",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Version ${TermsOfServiceRepository.CURRENT_TERMS_VERSION} • Effective ${TermsOfServiceRepository.EFFECTIVE_DATE}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("close_terms_button")
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
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = CleanGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "21 Clauses",
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
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("terms_close_bottom_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            TermsOfServiceRepository.recordTermsAcceptance(
                                userIdOrEmail = userIdentifier,
                                entryPoint = entryPointLabel
                            )
                            hasAcceptedInSession = true
                            onAcceptTerms?.invoke()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("accept_terms_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasAcceptedInSession) "Terms Accepted" else "Accept Terms of Service",
                            fontWeight = FontWeight.Bold
                        )
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
                .testTag("terms_of_service_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // 1. Hero Banner & Statutory Consumer Protection Guarantee
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terms_statutory_rights_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0A1F35),
                                        Color(0xFF10375C),
                                        Color(0xFF064E3B)
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
                                        text = "FAIR & TRANSPARENT LEGAL FRAMEWORK",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "• $entryPointLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Text(
                                text = "SolarFix Multi-Vendor Marketplace & Diagnostic Terms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )

                            Text(
                                text = TermsOfServiceRepository.STATUTORY_RIGHTS_PRESERVATION_BANNER,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // 2. Search Bar & Audience Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Terms (e.g. escrow, refund, counterfeit, commission)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Terms")
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
                            .testTag("terms_search_input")
                    )

                    // Audience Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedAudienceFilter == "ALL",
                            onClick = { selectedAudienceFilter = "ALL" },
                            label = { Text("All 21 Sections") },
                            leadingIcon = {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("terms_filter_all")
                        )
                        FilterChip(
                            selected = selectedAudienceFilter == "CUSTOMER",
                            onClick = { selectedAudienceFilter = "CUSTOMER" },
                            label = { Text("Customer Rights") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("terms_filter_customer")
                        )
                        FilterChip(
                            selected = selectedAudienceFilter == "SELLER",
                            onClick = { selectedAudienceFilter = "SELLER" },
                            label = { Text("Seller Rules") },
                            leadingIcon = {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("terms_filter_seller")
                        )
                    }

                    // Quick-Jump Topic Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { selectedTopicChip = null },
                            label = { Text("Show All Topics") }
                        )
                        TermsSectionTopic.entries.forEach { topic ->
                            FilterChip(
                                selected = selectedTopicChip == topic,
                                onClick = {
                                    selectedTopicChip = if (selectedTopicChip == topic) null else topic
                                },
                                label = { Text("${topic.sectionNumber}. ${topic.shortLabel}") }
                            )
                        }
                    }
                }
            }

            // 3. Dedicated Distinction Card: SolarFix Responsibilities vs. Independent Marketplace Sellers
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solarfix_vs_seller_responsibility_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResponsibilityMatrix = !showResponsibilityMatrix },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Balance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SolarFix vs. Independent Seller Responsibilities",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        text = "Clear separation between platform escrow/verification duties and vendor Merchant-of-Record obligations",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showResponsibilityMatrix) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Matrix"
                            )
                        }

                        AnimatedVisibility(visible = showResponsibilityMatrix) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TermsOfServiceRepository.responsibilityComparisonMatrix.forEach { entry ->
                                    ResponsibilityComparisonItemCard(entry = entry)
                                }
                            }
                        }
                    }
                }
            }

            // 4. All 21 Legal & Operational Sections
            items(
                items = filteredSections,
                key = { it.topic.name }
            ) { section ->
                TermsSectionCard(section = section)
            }

            // 5. Official Contact & Complaint Escalation Footer Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terms_contact_complaints_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SolarFix Legal & Ombudsman Contact Desk",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "• Legal & Compliance: ${TermsOfServiceRepository.LEGAL_SUPPORT_EMAIL}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Customer & Seller Complaints Desk: ${TermsOfServiceRepository.COMPLAINTS_DESK_EMAIL}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Toll-Free Hotline: ${TermsOfServiceRepository.COMPLAINTS_HOTLINE}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Every complaint receives a tracked ticket within 24 hours and an impartial review within 5 business days, without prejudice to your statutory right to contact the FCCPC or NDPC.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsibilityComparisonItemCard(entry: ResponsibilityComparisonEntry) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "SolarFix (Platform & Escrow): ${entry.solarFixPlatformResponsibility}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Independent Seller (Merchant of Record): ${entry.independentSellerResponsibility}",
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                color = CleanGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Customer Protection: ${entry.customerProtectionGuarantee}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = CleanGreen,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TermsSectionCard(section: TermsOfServiceSection) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("terms_section_${section.topic.sectionNumber}")
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
                            .size(32.dp)
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
                    contentDescription = "Toggle Section"
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
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    section.statutoryRightsPreservationNote?.let { note ->
                        Surface(
                            color = CleanGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CleanGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = CleanGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
