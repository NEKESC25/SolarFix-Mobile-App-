package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.components.CameraXCaptureView
import com.example.util.CameraPermissionHelper
import com.example.util.rememberCameraPermissionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiChatState
import com.example.ui.viewmodel.TechnicianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: TechnicianViewModel,
    onNavigateToSystemHub: () -> Unit = {},
    onOpenFaultLibrary: () -> Unit = {}
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatState by viewModel.aiChatState.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var attachedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isCameraXOpen by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Camera Permission State managed via CameraPermissionHelper utility
    val cameraPermissionState = rememberCameraPermissionState(
        onGranted = {
            isCameraXOpen = true
        },
        onDenied = { _ ->
            showPermissionExplanation = true
        }
    )

    // Function to launch CameraX viewfinder with permission validation
    fun openCameraX() {
        if (cameraPermissionState.isGranted) {
            isCameraXOpen = true
        } else {
            cameraPermissionState.requestPermission()
        }
    }

    // Photo Picker Launcher - Selects photo from device gallery without broad storage permissions
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                attachedPhoto = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size, chatState) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI Solar Technician",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(CleanGreen)
                                )
                                Text(
                                    text = "Multimodal Diagnostics • Gemini 3.8",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenFaultLibrary,
                        modifier = Modifier.testTag("open_fault_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Fault Code Manual",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Consultation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSystemHub,
                        modifier = Modifier.testTag("chat_technician_dispatch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = "Technicians",
                            tint = SolarAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Diagnostic Guidance Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ActionChip(
                            label = "📖 Inverter Error Codes",
                            onClick = onOpenFaultLibrary
                        )
                    }
                    item {
                        ActionChip(
                            label = "📸 Snap Inverter Screen",
                            onClick = {
                                openCameraX()
                            }
                        )
                    }
                    item {
                        ActionChip(
                            label = "🔋 Battery Drain at Night",
                            onClick = {
                                viewModel.sendMessage("Why is my solar battery dropping fast from 100% to 20% within 1-2 hours at night?")
                            }
                        )
                    }
                    item {
                        ActionChip(
                            label = "⚡ Inverter Trips on Pump",
                            onClick = {
                                viewModel.sendMessage("My inverter trips with Overload / Error 51 whenever the water pumping machine turns on. How do I fix it?")
                            }
                        )
                    }
                    item {
                        ActionChip(
                            label = "☀️ Low Solar Yield",
                            onClick = {
                                viewModel.sendMessage("The sun is bright but my MPPT shows almost 0 Amps solar charging. What should I inspect?")
                            }
                        )
                    }
                    item {
                        ActionChip(
                            label = "⚠️ Electric Shock on Body",
                            onClick = {
                                viewModel.sendMessage("I feel a tingling electric shock when touching the metallic body of my inverter or panel frames. Is it earthing?")
                            }
                        )
                    }
                }
            }

            // Message Bubble List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageItem(
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (attachedPhoto != null) "Analyzing photo and diagnosing solar hardware..."
                                else "AI Solar Technician is analyzing fault...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Photo Preview Bar (Shown when user has snapped or selected a photo)
            AnimatedVisibility(visible = attachedPhoto != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (attachedPhoto != null) {
                                Image(
                                    bitmap = attachedPhoto!!.asImageBitmap(),
                                    contentDescription = "Attached Photo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                )
                            }
                            Column {
                                Text(
                                    text = "Solar Photo Attached",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "AI will visually inspect screens, terminals & wiring",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { attachedPhoto = null },
                            modifier = Modifier.testTag("remove_attached_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input Field Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // CameraX Action Button
                    IconButton(
                        onClick = { openCameraX() },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("chat_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Photo of Solar Hardware",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Gallery Action Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("chat_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Choose Photo from Gallery",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (attachedPhoto != null) "Add notes (e.g. Inverter brand, beep sound)..."
                                else "Describe fault or snap equipment photo...",
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    // Send Button
                    val canSend = textInput.isNotBlank() || attachedPhoto != null
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val query = textInput
                                val photo = attachedPhoto
                                textInput = ""
                                attachedPhoto = null
                                viewModel.sendMessage(query, photo)
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Restart / Clear Consultation Confirmation Dialog
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                icon = {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                title = { Text("Restart AI Technician Session?") },
                text = { Text("This will clear the current diagnostic conversation history.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearChat()
                            showClearConfirm = false
                        }
                    ) {
                        Text("Clear History")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // CameraX Fullscreen Viewfinder Overlay
        if (isCameraXOpen) {
            Dialog(
                onDismissRequest = { isCameraXOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true
                )
            ) {
                CameraXCaptureView(
                    onPhotoCaptured = { bitmap ->
                        attachedPhoto = bitmap
                        isCameraXOpen = false
                    },
                    onClose = { isCameraXOpen = false }
                )
            }
        }

        // Camera Permission Explanation Dialog
        if (showPermissionExplanation) {
            AlertDialog(
                onDismissRequest = { showPermissionExplanation = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Camera Permission Needed") },
                text = {
                    Text(
                        "Solarfix AI requires camera access so you can point your phone at your inverter screen, battery terminals, or solar array to automatically detect faults, error codes, and wiring issues."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionExplanation = false
                            if (cameraPermissionState.shouldShowRationale) {
                                cameraPermissionState.requestPermission()
                            } else {
                                cameraPermissionState.openSettings()
                            }
                        }
                    ) {
                        Text(if (cameraPermissionState.shouldShowRationale) "Grant Permission" else "Open App Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionExplanation = false }) {
                        Text("Not Now")
                    }
                }
            )
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AI Avatar
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "AI Solarfix",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Message Bubble
            Surface(
                color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 18.dp
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // If user attached a photo, render the photo in the chat bubble
                    if (message.attachedBitmap != null) {
                        Image(
                            bitmap = message.attachedBitmap.asImageBitmap(),
                            contentDescription = "Solar Photo Inspected",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 21.sp
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

        // Quick suggestions from AI
        if (message.quickSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 40.dp)
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
