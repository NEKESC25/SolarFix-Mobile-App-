package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaintenanceTask
import com.example.data.model.SolarSystemProfile
import com.example.data.model.TechnicianContact
import com.example.ui.components.SolarfixHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.SystemHubViewModel

@Composable
fun SystemHubScreen(
    viewModel: SystemHubViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val maintenanceTasks by viewModel.maintenanceTasks.collectAsState()
    val selectedTechnician by viewModel.selectedTechnician.collectAsState()
    val isEditingProfile by viewModel.isEditingProfile.collectAsState()

    val completedCount = viewModel.getCompletedTasksCount()
    val totalCount = maintenanceTasks.size

    Scaffold(
        topBar = {
            SolarfixHeader(
                title = "System Hub & Care",
                subtitle = "Hardware Profile & Maintenance",
                currentCurrency = com.example.data.model.AvailableCurrencies[0],
                onCurrencySelected = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Equipment Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeRepairService,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "My Solar Installation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = { viewModel.setEditingProfile(true) },
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileSpecItem(
                                label = "Inverter",
                                value = "${profile.systemSizeKva} kVA",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileSpecItem(
                                label = "Battery Storage",
                                value = "${profile.batteryCapacityKwh} kWh",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileSpecItem(
                                label = "Solar Panels",
                                value = "${profile.solarPanelWatts} W",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Battery Chemistry: ${profile.batteryType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Installer: ${profile.installerName} (Installed ${profile.installYear})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Maintenance Progress & Tasks Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Preventive Maintenance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$completedCount of $totalCount completed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CleanGreen,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }

            // Maintenance Task List Items
            items(maintenanceTasks) { task ->
                MaintenanceTaskCard(
                    task = task,
                    onToggle = { viewModel.toggleMaintenanceTask(task.id) }
                )
            }

            // Certified Technicians Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Certified Solar Technicians",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verified on-site repairs, wiring & installations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Technician Cards
            items(viewModel.technicians) { tech ->
                TechnicianCard(
                    tech = tech,
                    onCallClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${tech.phone.replace(" ", "")}")
                        }
                        context.startActivity(dialIntent)
                    },
                    onSelect = { viewModel.selectTechnician(tech) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Edit Profile Dialog
        if (isEditingProfile) {
            EditProfileDialog(
                currentProfile = profile,
                onDismiss = { viewModel.setEditingProfile(false) },
                onSave = { kva, batteryType, batteryKwh, panelWatts, installer ->
                    viewModel.saveProfile(kva, batteryType, batteryKwh, panelWatts, installer)
                }
            )
        }

        // Technician Booking Dialog
        if (selectedTechnician != null) {
            val tech = selectedTechnician!!
            AlertDialog(
                onDismissRequest = { viewModel.selectTechnician(null) },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CleanGreen)
                        Text(text = tech.name, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tech.businessName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📍 Coverage: ${tech.location}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "★ ${tech.rating} • ${tech.completedJobs} Verified Installations",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Certifications:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        tech.badges.forEach { badge ->
                            Text(text = "• $badge", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${tech.phone.replace(" ", "")}")
                            }
                            context.startActivity(dialIntent)
                            viewModel.selectTechnician(null)
                        }
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.selectTechnician(null) }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileSpecItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MaintenanceTaskCard(
    task: MaintenanceTask,
    onToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = CleanGreen)
                    )

                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Interval: ${task.frequency}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, top = 8.dp)
                ) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SolarAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = task.safetyTips,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicianCard(
    tech: TechnicianContact,
    onCallClick: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("tech_card_${tech.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tech.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (tech.verified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Tech",
                                tint = CleanGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = tech.businessName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "📍 ${tech.location}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CleanGreen),
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call Technician",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${tech.rating} (${tech.completedJobs} jobs)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                tech.badges.take(2).forEach { badge ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: SolarSystemProfile,
    onDismiss: () -> Unit,
    onSave: (Double, String, Double, Int, String) -> Unit
) {
    var kvaText by remember { mutableStateOf(currentProfile.systemSizeKva.toString()) }
    var batteryType by remember { mutableStateOf(currentProfile.batteryType) }
    var batteryKwhText by remember { mutableStateOf(currentProfile.batteryCapacityKwh.toString()) }
    var panelWattsText by remember { mutableStateOf(currentProfile.solarPanelWatts.toString()) }
    var installerText by remember { mutableStateOf(currentProfile.installerName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Solar Installation Specs", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = kvaText,
                    onValueChange = { kvaText = it },
                    label = { Text("Inverter Capacity (kVA)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = batteryType,
                    onValueChange = { batteryType = it },
                    label = { Text("Battery Type (e.g. Lithium LiFePO4, Tubular)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = batteryKwhText,
                    onValueChange = { batteryKwhText = it },
                    label = { Text("Battery Capacity (kWh)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = panelWattsText,
                    onValueChange = { panelWattsText = it },
                    label = { Text("Total Solar Panels (Watts)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = installerText,
                    onValueChange = { installerText = it },
                    label = { Text("Installer / Company Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kva = kvaText.toDoubleOrNull() ?: currentProfile.systemSizeKva
                    val bKwh = batteryKwhText.toDoubleOrNull() ?: currentProfile.batteryCapacityKwh
                    val pWatts = panelWattsText.toIntOrNull() ?: currentProfile.solarPanelWatts
                    onSave(kva, batteryType, bKwh, pWatts, installerText)
                }
            ) {
                Text("Save Setup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
