package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.InverterBrand
import com.example.data.model.InverterErrorCode
import com.example.data.model.SymptomDiagnostic
import com.example.ui.components.SeverityBadge
import com.example.ui.components.SolarfixHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiChatState
import com.example.ui.viewmodel.TechnicianViewModel
import kotlinx.coroutines.launch

enum class TechnicianSubTab(val title: String) {
    AI_CHAT("AI Diagnostic Chat"),
    ERROR_CODES("Inverter Fault Codes"),
    SYMPTOMS("Common Symptoms"),
    SAFETY("Safety Protocols")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianScreen(
    viewModel: TechnicianViewModel,
    onNavigateToSystemHub: () -> Unit
) {
    var activeTab by remember { mutableStateOf(TechnicianSubTab.AI_CHAT) }
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeErrorCode by viewModel.activeErrorCode.collectAsState()
    val activeSymptom by viewModel.activeSymptom.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatState by viewModel.aiChatState.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                SolarfixHeader(
                    title = "AI Solar Technician",
                    subtitle = "Automated Diagnostics & Expert Fixes",
                    currentCurrency = com.example.data.model.AvailableCurrencies[0],
                    onCurrencySelected = {}
                )

                // Sub-navigation tab row
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    TechnicianSubTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
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
            when (activeTab) {
                TechnicianSubTab.AI_CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Chat messages list
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatMessages) { message ->
                                ChatMessageBubble(
                                    message = message,
                                    onSuggestionClick = { suggestion ->
                                        viewModel.sendMessage(suggestion)
                                    }
                                )
                            }

                            if (chatState is AiChatState.Loading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Analyzing solar telemetry & diagnostics...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Message Input Area
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = {
                                        Text(
                                            "Ask about any inverter fault, battery drain...",
                                            fontSize = 13.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(24.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            val query = textInput
                                            textInput = ""
                                            viewModel.sendMessage(query)
                                        }
                                    },
                                    enabled = textInput.isNotBlank(),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Message",
                                        tint = if (textInput.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                TechnicianSubTab.ERROR_CODES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Brand Selector Chips
                        Text(
                            text = "Select Inverter Brand",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(InverterBrand.values()) { brand ->
                                FilterChip(
                                    selected = selectedBrand == brand,
                                    onClick = { viewModel.setSelectedBrand(brand) },
                                    label = { Text(brand.displayName) },
                                    modifier = Modifier.testTag("chip_${brand.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search box for error code
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search error code (e.g. Error 04, 51, F08)...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("error_search_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val filteredList = viewModel.getFilteredErrorCodes()

                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CleanGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No matching error code found in ${selectedBrand.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = {
                                            activeTab = TechnicianSubTab.AI_CHAT
                                            viewModel.sendMessage("Help me diagnose code '$searchQuery' on ${selectedBrand.displayName} inverter.")
                                        }
                                    ) {
                                        Text("Ask AI Technician directly →")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredList) { item ->
                                    ErrorCodeCard(
                                        errorCode = item,
                                        onClick = { viewModel.selectErrorCode(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                TechnicianSubTab.SYMPTOMS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tap any common solar fault symptom below for immediate root-cause diagnosis and step-by-step DIY fix.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        items(viewModel.getAllSymptoms()) { symptom ->
                            SymptomCard(
                                symptom = symptom,
                                onClick = { viewModel.selectSymptom(symptom) }
                            )
                        }
                    }
                }

                TechnicianSubTab.SAFETY -> {
                    SafetyProtocolsView(
                        onCallTechnician = onNavigateToSystemHub
                    )
                }
            }

            // Error Code Detail Bottom Sheet / Dialog
            if (activeErrorCode != null) {
                ErrorCodeDetailDialog(
                    errorCode = activeErrorCode!!,
                    onDismiss = { viewModel.selectErrorCode(null) },
                    onAskAiMore = {
                        val code = activeErrorCode!!
                        viewModel.selectErrorCode(null)
                        activeTab = TechnicianSubTab.AI_CHAT
                        viewModel.sendMessage("Can you give me more in-depth troubleshooting for ${code.brand.displayName} ${code.code}: ${code.title}?")
                    },
                    onDispatchTechnician = {
                        viewModel.selectErrorCode(null)
                        onNavigateToSystemHub()
                    }
                )
            }

            // Symptom Detail Dialog
            if (activeSymptom != null) {
                SymptomDetailDialog(
                    symptom = activeSymptom!!,
                    onDismiss = { viewModel.selectSymptom(null) },
                    onAskAiMore = {
                        val sym = activeSymptom!!
                        viewModel.selectSymptom(null)
                        activeTab = TechnicianSubTab.AI_CHAT
                        viewModel.sendMessage("I need help troubleshooting this symptom: ${sym.title}. What could be causing it?")
                    },
                    onDispatchTechnician = {
                        viewModel.selectSymptom(null)
                        onNavigateToSystemHub()
                    }
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "AI",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Suggestions chips
        if (message.quickSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 34.dp)
            ) {
                items(message.quickSuggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCodeCard(
    errorCode: InverterErrorCode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("error_card_${errorCode.code.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorCode.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SeverityBadge(severity = errorCode.severity)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorCode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = errorCode.probableCause,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SymptomCard(
    symptom: SymptomDiagnostic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("symptom_card_${symptom.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (symptom.iconName) {
                        "battery_alert" -> Icons.Default.BatteryAlert
                        "wb_sunny" -> Icons.Default.WbSunny
                        "bolt" -> Icons.Default.Bolt
                        "water_drop" -> Icons.Default.WaterDrop
                        else -> Icons.Default.Build
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = symptom.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = symptom.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorCodeDetailDialog(
    errorCode: InverterErrorCode,
    onDismiss: () -> Unit,
    onAskAiMore: () -> Unit,
    onDispatchTechnician: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${errorCode.brand.displayName} - ${errorCode.code}",
                    fontWeight = FontWeight.Bold
                )
                SeverityBadge(severity = errorCode.severity)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = errorCode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Text(
                        text = "Probable Cause:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = errorCode.probableCause,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Text(
                        text = "Step-by-Step Fix:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CleanGreen
                    )
                    errorCode.stepByStepFix.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                if (errorCode.safetyWarning != null) {
                    item {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorCode.safetyWarning,
                                    color = AlertRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAskAiMore,
                modifier = Modifier.testTag("dialog_ask_ai_button")
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI More")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDispatchTechnician) {
                Text("Call Technician")
            }
        }
    )
}

@Composable
fun SymptomDetailDialog(
    symptom: SymptomDiagnostic,
    onDismiss: () -> Unit,
    onAskAiMore: () -> Unit,
    onDispatchTechnician: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = symptom.title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "Quick Assessment:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = symptom.quickDiagnosis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Text(
                        text = "Resolution Steps:",
                        fontWeight = FontWeight.Bold,
                        color = CleanGreen,
                        fontSize = 13.sp
                    )
                    symptom.steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                if (symptom.professionalRequired) {
                    item {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ This issue involves electrical safety / high voltage. We recommend verified technician on-site inspection.",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAskAiMore) {
                Text("Ask AI More")
            }
        },
        dismissButton = {
            TextButton(onClick = onDispatchTechnician) {
                Text("Technician Directory")
            }
        }
    )
}

@Composable
fun SafetyProtocolsView(
    onCallTechnician: () -> Unit
) {
    val safetyRules = listOf(
        Pair("1. Always Switch Off DC Isolator First", "Solar panels produce continuous high DC voltage (>150V - 500V) whenever daylight is present. DC arcs do not self-extinguish like AC. Always trip the DC isolator before checking wiring."),
        Pair("2. Never Short-Circuit Battery Terminals", "Lithium and lead-acid batteries can instantly release thousands of amperes in a short-circuit, melting spanners and exploding with severe burns. Always wrap tools with electrical insulating tape."),
        Pair("3. Test Earth Grounding Annually", "Tropical thunderstorms in Africa carry extreme lightning energy. Ensure your DC Surge Protection Device (SPD) has green indicators, and pour water into earth pits during dry seasons."),
        Pair("4. Avoid Water on Hot Panels", "Cleaning dusty solar panels in the scorching afternoon sun can cause thermal glass shock, shattering the tempered glass. Only wash panels early in the morning before 8:30 AM.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Solar Electrical Safety Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Critical guidelines before opening inverter covers or touching battery terminals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        items(safetyRules) { (title, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        item {
            Button(
                onClick = onCallTechnician,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("emergency_technician_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency? Connect with Certified Technician")
            }
        }
    }
}
