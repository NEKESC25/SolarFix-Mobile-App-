package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanGreen
import com.example.ui.viewmodel.MarketplaceViewModel

private val NIGERIAN_BANKS = listOf(
    "Zenith Bank PLC",
    "Guaranty Trust Bank (GTCO)",
    "Access Bank PLC",
    "First Bank of Nigeria",
    "United Bank for Africa (UBA)",
    "Stanbic IBTC Bank",
    "Fidelity Bank PLC",
    "Moniepoint MFB"
)

private val ID_DOCUMENT_TYPES = listOf(
    "NIN Slip / National ID Card",
    "International Passport",
    "Driver's License",
    "Permanent Voter's Card (PVC)",
    "CAC Certificate + Director ID"
)

private fun createSampleVerificationBitmap(title: String, subtitle: String, bgHex: Int): Bitmap {
    val bmp = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(bgHex)
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 20f
        isAntiAlias = true
        isFakeBoldText = true
    }
    canvas.drawText(title, 20f, 80f, paint)
    paint.textSize = 15f
    paint.isFakeBoldText = false
    canvas.drawText(subtitle, 20f, 115f, paint)
    return bmp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRegistrationContent(
    viewModel: MarketplaceViewModel,
    onBackToMarketplace: () -> Unit
) {
    BackHandler { onBackToMarketplace() }

    // 1. Business & Contact Identity Fields
    var fullNameOrBusinessName by remember { mutableStateOf("") }
    var ownerFullName by remember { mutableStateOf("") }
    var cacRegistrationNumber by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }
    var businessEmail by remember { mutableStateOf("") }
    var physicalAddress by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("Lagos State") }
    var cityName by remember { mutableStateOf("") }
    var businessDescription by remember { mutableStateOf("") }

    // 2. KYC / KYB Visual & Document Attachments
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var logoAttachedLabel by remember { mutableStateOf<String?>(null) }

    var selectedIdType by remember { mutableStateOf(ID_DOCUMENT_TYPES.first()) }
    var idDocumentNumber by remember { mutableStateOf("") }
    var idDocumentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var idDocAttachedLabel by remember { mutableStateOf<String?>(null) }

    var premisesPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var premisesAttachedLabel by remember { mutableStateOf<String?>(null) }

    // 3. Bank Settlement Details (Encrypted & Private)
    var bankAccountName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf(NIGERIAN_BANKS.first()) }
    var bankAccountNumber by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf<String?>(null) }

    val logoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            logoBitmap = bmp
            logoAttachedLabel = "Captured Company Logo / Profile Photo"
        }
    }

    val idDocCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            idDocumentBitmap = bmp
            idDocAttachedLabel = "Captured $selectedIdType"
        }
    }

    val premisesCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            premisesPhotoBitmap = bmp
            premisesAttachedLabel = "Captured Business Premises / Warehouse Photo"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("seller_registration_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // Header Banner
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
                                listOf(Color(0xFF0B243B), Color(0xFF143C5E), Color(0xFF065F46))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SOLARFIX SELLER ONBOARDING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            TextButton(
                                onClick = onBackToMarketplace,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back to Shop", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Become a Verified SolarFix Equipment Seller",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "Any individual or registered solar business can apply. New applications start in 'Pending Verification' status until vetted by SolarFix Admin. Only Approved Sellers receive the VERIFIED SELLER badge and can publish products.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Strict Privacy & Data Protection Assurance Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CleanGreen.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Protected KYC Data",
                        tint = CleanGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Seller Privacy & Data Protection Guarantee",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CleanGreen
                        )
                        Text(
                            text = "Customers NEVER have access to your identification documents, premises photos, or bank account details. These records are strictly restricted to SolarFix Compliance Verification.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: Business & Contact Details
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Business & Contact Information",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedTextField(
                        value = fullNameOrBusinessName,
                        onValueChange = { fullNameOrBusinessName = it },
                        label = { Text("Full Name or Registered Business Name *") },
                        placeholder = { Text("e.g. SunPeak Solar Technologies Ltd") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_business_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ownerFullName,
                            onValueChange = { ownerFullName = it },
                            label = { Text("Contact / Director Name *") },
                            placeholder = { Text("e.g. Engr. Tunde Bakare") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_owner_name_input")
                        )
                        OutlinedTextField(
                            value = cacRegistrationNumber,
                            onValueChange = { cacRegistrationNumber = it },
                            label = { Text("CAC Reg Number (if applicable)") },
                            placeholder = { Text("e.g. RC-3094812 or BN-91023") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_cac_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = businessPhone,
                            onValueChange = { businessPhone = it },
                            label = { Text("Business Phone Number *") },
                            placeholder = { Text("+234 803 000 0000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_phone_input")
                        )
                        OutlinedTextField(
                            value = businessEmail,
                            onValueChange = { businessEmail = it },
                            label = { Text("Business Email Address *") },
                            placeholder = { Text("sales@sunpeaksolar.ng") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_email_input")
                        )
                    }

                    OutlinedTextField(
                        value = physicalAddress,
                        onValueChange = { physicalAddress = it },
                        label = { Text("Business Physical Address (Shop / Warehouse) *") },
                        placeholder = { Text("e.g. Suite 14, Arena Solar Plaza, Oshodi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_physical_address_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stateName,
                            onValueChange = { stateName = it },
                            label = { Text("State *") },
                            placeholder = { Text("e.g. Lagos State") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_state_input")
                        )
                        OutlinedTextField(
                            value = cityName,
                            onValueChange = { cityName = it },
                            label = { Text("City / Town *") },
                            placeholder = { Text("e.g. Ikeja") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_city_input")
                        )
                    }

                    OutlinedTextField(
                        value = businessDescription,
                        onValueChange = { businessDescription = it },
                        label = { Text("Business Description & Solar Specializations *") },
                        placeholder = { Text("Describe the solar inverters, LiFePO4 batteries, PV panels, and warranties your business supplies...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_description_input")
                    )
                }
            }
        }

        // Section 2: Identity & Business Premises Verification Uploads
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Identity & Business Premises Verification",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // 2A. Profile Photograph or Company Logo
                    VerificationDocumentUploadBox(
                        title = "Profile Photograph or Company Logo *",
                        subtitle = logoAttachedLabel ?: "Upload your official company logo or seller profile portrait",
                        bitmap = logoBitmap,
                        isAttached = logoBitmap != null || logoAttachedLabel != null,
                        onCaptureCamera = { logoCameraLauncher.launch() },
                        onAttachSample = {
                            logoBitmap = createSampleVerificationBitmap(
                                title = fullNameOrBusinessName.ifBlank { "SOLAR SELLER LOGO" }.take(20),
                                subtitle = "Official Store Emblem",
                                bgHex = 0xFF0F766E.toInt()
                            )
                            logoAttachedLabel = "Attached Official Seller Logo"
                        },
                        testTagPrefix = "reg_logo"
                    )

                    HorizontalDivider()

                    // 2B. Valid Identification Document
                    Text(
                        text = "Valid Identification Document (Private - Admin Only) *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ID_DOCUMENT_TYPES) { docType ->
                            FilterChip(
                                selected = selectedIdType == docType,
                                onClick = { selectedIdType = docType },
                                label = { Text(docType, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = idDocumentNumber,
                        onValueChange = { idDocumentNumber = it },
                        label = { Text("$selectedIdType Number *") },
                        placeholder = { Text("e.g. NIN-74829103841 or A09482192") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_id_number_input")
                    )

                    VerificationDocumentUploadBox(
                        title = "Upload $selectedIdType Scan / Photo *",
                        subtitle = idDocAttachedLabel ?: "Clear photograph of government-issued ID card",
                        bitmap = idDocumentBitmap,
                        isAttached = idDocumentBitmap != null || idDocAttachedLabel != null,
                        onCaptureCamera = { idDocCameraLauncher.launch() },
                        onAttachSample = {
                            idDocumentBitmap = createSampleVerificationBitmap(
                                title = selectedIdType.take(22),
                                subtitle = "ID #: ${idDocumentNumber.ifBlank { "VERIFIED-ID-DOC" }}",
                                bgHex = 0xFF1E3A8A.toInt()
                            )
                            idDocAttachedLabel = "Attached $selectedIdType Scan"
                        },
                        testTagPrefix = "reg_id_doc"
                    )

                    HorizontalDivider()

                    // 2C. Business Premises Photograph
                    VerificationDocumentUploadBox(
                        title = "Business Premises / Warehouse Photograph *",
                        subtitle = premisesAttachedLabel ?: "Photograph showing physical storefront, signboard, or solar stockroom",
                        bitmap = premisesPhotoBitmap,
                        isAttached = premisesPhotoBitmap != null || premisesAttachedLabel != null,
                        onCaptureCamera = { premisesCameraLauncher.launch() },
                        onAttachSample = {
                            premisesPhotoBitmap = createSampleVerificationBitmap(
                                title = "SOLAR WAREHOUSE & SHOP",
                                subtitle = physicalAddress.ifBlank { "Verified Solar Storefront Premises" }.take(28),
                                bgHex = 0xFF92400E.toInt()
                            )
                            premisesAttachedLabel = "Attached Business Premises Photograph"
                        },
                        testTagPrefix = "reg_premises"
                    )
                }
            }
        }

        // Section 3: Bank Account Details for Sales Proceeds
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CleanGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "3. Bank Account Details for Sales Proceeds",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Used exclusively for disbursing cleared 48-Hour Escrow earnings. Never shown to customers.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Select Settlement Bank *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NIGERIAN_BANKS) { bank ->
                            FilterChip(
                                selected = bankName == bank,
                                onClick = { bankName = bank },
                                label = { Text(bank, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_bank_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bankAccountName,
                            onValueChange = { bankAccountName = it },
                            label = { Text("Bank Account Name *") },
                            placeholder = { Text("Must match business or owner name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("reg_bank_account_name_input")
                        )
                        OutlinedTextField(
                            value = bankAccountNumber,
                            onValueChange = { if (it.length <= 10) bankAccountNumber = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Account Number *") },
                            placeholder = { Text("10-digit NUBAN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("reg_bank_account_number_input")
                        )
                    }
                }
            }
        }

        // Validation Error Alert
        if (validationError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Seller Terms of Use, Privacy Policy & Marketplace Agreement Section (Unselected by default)
        item {
            var showSellerTermsModal by remember { mutableStateOf(false) }
            var showSellerPrivacyModal by remember { mutableStateOf(false) }
            var sellerTermsAccepted by remember { mutableStateOf(false) }
            var sellerPrivacyAcknowledged by remember { mutableStateOf(false) }
            var sellerAgreementAccepted by remember { mutableStateOf(false) }

            if (showSellerTermsModal) {
                TermsOfServiceModalDialog(
                    entryPointLabel = "Seller Registration & KYB Onboarding",
                    userIdentifier = businessEmail.ifBlank { "seller_applicant" },
                    onDismiss = { showSellerTermsModal = false },
                    onAcceptTerms = { sellerTermsAccepted = true }
                )
            }
            if (showSellerPrivacyModal) {
                PrivacyPolicyModalDialog(
                    entryPointLabel = "Seller Registration & KYB Onboarding",
                    userIdentifier = businessEmail.ifBlank { "seller_applicant" },
                    onDismiss = { showSellerPrivacyModal = false }
                )
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seller_registration_terms_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seller Legal Acceptance & KYB Consent",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Open and review the documents below before providing affirmative consent:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSellerTermsModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seller_reg_terms_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Terms", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showSellerPrivacyModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seller_reg_privacy_button")
                        ) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Privacy Policy", fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sellerTermsAccepted,
                            onCheckedChange = { sellerTermsAccepted = it },
                            modifier = Modifier.testTag("seller_reg_accept_terms_checkbox")
                        )
                        Text(
                            text = "I accept the SolarFix Terms of Use & Terms of Service (v3.2) *",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sellerPrivacyAcknowledged,
                            onCheckedChange = { sellerPrivacyAcknowledged = it },
                            modifier = Modifier.testTag("seller_reg_accept_privacy_checkbox")
                        )
                        Text(
                            text = "I acknowledge the SolarFix Privacy Policy & encrypted PRIVATE_ADMIN_VAULT storage of my KYC documents *",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sellerAgreementAccepted,
                            onCheckedChange = { sellerAgreementAccepted = it },
                            modifier = Modifier.testTag("seller_reg_accept_agreement_checkbox")
                        )
                        Text(
                            text = "I accept Independent Seller Merchant-of-Record duties, anti-counterfeit rules & 5% escrow commission *",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Submit Registration Application Button
        item {
            Button(
                onClick = {
                    when {
                        fullNameOrBusinessName.isBlank() -> {
                            validationError = "Please enter your Full Name or Registered Business Name."
                        }
                        businessPhone.isBlank() || businessEmail.isBlank() -> {
                            validationError = "Please provide both your Business Phone Number and Business Email Address."
                        }
                        physicalAddress.isBlank() || cityName.isBlank() || stateName.isBlank() -> {
                            validationError = "Please complete your Business Physical Address, State, and City."
                        }
                        businessDescription.isBlank() -> {
                            validationError = "Please provide a brief description of your solar equipment business."
                        }
                        idDocumentNumber.isBlank() -> {
                            validationError = "Please enter your Valid Identification Document number."
                        }
                        bankAccountName.isBlank() || bankName.isBlank() || bankAccountNumber.length < 10 -> {
                            validationError = "Please provide complete Bank Account Name, Bank Name, and 10-digit Account Number."
                        }
                        else -> {
                            validationError = null
                            // Record seller legal document acceptances
                            com.example.data.legal.UserConsentAndAuditReportManager.recordDocumentAcceptance(
                                userId = businessEmail,
                                documentType = com.example.data.legal.LegalDocumentType.TERMS_OF_SERVICE,
                                entryPoint = "Seller KYB Registration"
                            )
                            com.example.data.legal.UserConsentAndAuditReportManager.recordDocumentAcceptance(
                                userId = businessEmail,
                                documentType = com.example.data.legal.LegalDocumentType.PRIVACY_POLICY,
                                entryPoint = "Seller KYB Registration"
                            )
                            com.example.data.legal.UserConsentAndAuditReportManager.recordDocumentAcceptance(
                                userId = businessEmail,
                                documentType = com.example.data.legal.LegalDocumentType.SELLER_MARKETPLACE_AGREEMENT,
                                entryPoint = "Seller KYB Registration"
                            )
                            viewModel.submitSellerRegistration(
                                fullNameOrBusinessName = fullNameOrBusinessName,
                                ownerFullName = ownerFullName.ifBlank { fullNameOrBusinessName },
                                cacRegistrationNumber = cacRegistrationNumber.ifBlank { "Individual Seller (CAC Optional)" },
                                businessPhone = businessPhone,
                                businessEmail = businessEmail,
                                physicalAddress = physicalAddress,
                                state = stateName,
                                city = cityName,
                                businessDescription = businessDescription,
                                logoBitmap = logoBitmap,
                                idDocumentType = selectedIdType,
                                idDocumentNumber = idDocumentNumber,
                                idDocumentBitmap = idDocumentBitmap,
                                premisesPhotoBitmap = premisesPhotoBitmap,
                                bankAccountName = bankAccountName,
                                bankName = bankName,
                                bankAccountNumber = bankAccountNumber
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_seller_registration_button")
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Seller Application (Pending Verification)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun VerificationDocumentUploadBox(
    title: String,
    subtitle: String,
    bitmap: Bitmap?,
    isAttached: Boolean,
    onCaptureCamera: () -> Unit,
    onAttachSample: () -> Unit,
    testTagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            if (isAttached) {
                Surface(
                    color = CleanGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "✓ Attached",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CleanGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CleanGreen, RoundedCornerShape(10.dp))
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCaptureCamera,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_camera_btn")
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera Capture", fontSize = 11.sp)
            }

            FilledTonalButton(
                onClick = onAttachSample,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_upload_btn")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAttached) "Replace File" else "Attach Document", fontSize = 11.sp)
            }
        }
    }
}
